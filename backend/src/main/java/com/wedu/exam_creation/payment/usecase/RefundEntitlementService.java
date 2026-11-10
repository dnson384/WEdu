package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.user.usecase.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundEntitlementService {
    private final IPaymentRepository payments;
    private final UserService users;

    public Instant preview(PaymentOrderEntity order, Instant now) {
        if (order == null || order.getPlusExpiresAt() == null || order.getPaidAt() == null || order.isPlusLegacyProtected()) return null;
        Instant current = users.paidPlusExpiry(order.getUserId());
        if (current == null) return null; // Preserve legacy PLUS/null and deleted accounts.
        List<PaymentOrderEntity> grants = grants(order.getUserId());
        Instant previous = null;
        for (PaymentOrderEntity grant : grants) {
            Instant start = start(grant);
            if (start == null || start.isBefore(grant.getPaidAt()) || (previous != null && start.isBefore(previous))) return null;
            previous = grant.getPlusExpiresAt();
        }
        if (previous == null || !previous.equals(current)) return null;
        long remaining = remaining(order, now);
        return current.minusSeconds(remaining);
    }

    public boolean revoke(PaymentOrderEntity order, Instant now) {
        if (order.getRefundedAt() != null) return true;
        Instant expiry = preview(order, now);
        if (expiry == null) return false;
        long seconds = remaining(order, now);
        Instant end = order.getPlusExpiresAt();
        for (PaymentOrderEntity grant : grants(order.getUserId())) {
            if (grant.getId().equals(order.getId())) continue;
            if (!start(grant).isBefore(end) && seconds > 0) {
                grant.setPlusStartsAt(start(grant).minusSeconds(seconds));
                grant.setPlusExpiresAt(grant.getPlusExpiresAt().minusSeconds(seconds));
                if (!payments.update(grant, grant.getRevision())) throw new OptimisticLockingFailureException("PLUS grant changed");
            }
        }
        order.setRefundedAt(now);
        if (!payments.update(order, order.getRevision()) || !users.revokePaidTime(order.getUserId(), expiry))
            throw new OptimisticLockingFailureException("PLUS entitlement changed");
        return true;
    }

    private List<PaymentOrderEntity> grants(String userId) {
        return payments.ordersForUser(userId).stream().filter(o -> "SUCCEEDED".equals(o.getStatus())
                        && o.getRefundedAt() == null && o.getPlusExpiresAt() != null && o.getPaidAt() != null)
                .sorted(Comparator.comparing(this::start, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }
    private Instant start(PaymentOrderEntity order) {
        if (order.getDurationDays() <= 0 || order.getPlusExpiresAt() == null) return null;
        return order.getPlusStartsAt() != null ? order.getPlusStartsAt()
                : order.getPlusExpiresAt().minusSeconds(order.getDurationDays() * 86400L);
    }
    private long remaining(PaymentOrderEntity order, Instant now) {
        Instant begin = start(order).isAfter(now) ? start(order) : now;
        return Math.max(0, java.time.Duration.between(begin, order.getPlusExpiresAt()).getSeconds());
    }
}
