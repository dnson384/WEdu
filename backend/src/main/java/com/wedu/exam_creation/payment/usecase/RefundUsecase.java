package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.payment.domain.entity.*;
import com.wedu.exam_creation.payment.domain.repository.*;
import com.wedu.exam_creation.payment.dto.RefundDTO;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DuplicateKeyException;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RefundUsecase {
    private final IRefundRepository refunds;
    private final IPaymentRepository payments;
    private final RefundAiService ai;
    private final RefundEntitlementService entitlement;
    private final RefundTransactions transactions;
    private final RefundSettings settings;
    private final VnpayGateway gateway;
    private final VnpayProperties vnpay;
    private static final Set<String> REVIEW = Set.of("PENDING_REVIEW", "NEEDS_VERIFICATION");
    private static final Set<String> STATUSES = Set.of("PENDING_REVIEW", "NEEDS_VERIFICATION", "PROCESSING", "REFUNDED", "REJECTED", "FAILED", "CANCELLED");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public RefundDTO.Orders orders(String userId, int page) {
        requirePage(page);
        var list = payments.ordersForUser(userId).stream()
                .sorted(Comparator.comparing(PaymentOrderEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip((long) page * 10).limit(11).toList();
        return new RefundDTO.Orders(list.stream().limit(10).map(o -> new RefundDTO.Order(o.getId(), o.getAmount(),
                o.getStatus(), o.getCreatedAt(), o.getPaidAt())).toList(), page, list.size() > 10);
    }

    public RefundDTO.Page search(String userId, String keyword, String status, String from, String to, int page) {
        requirePage(page);
        keyword = keyword.trim();
        if (keyword.length() > 254 || (!status.isEmpty() && !STATUSES.contains(status))) throw error(400, "Bộ lọc không hợp lệ.");
        Instant start = date(from, false), end = date(to, true);
        if (start != null && end != null && !start.isBefore(end)) throw error(400, "Khoảng ngày không hợp lệ.");
        long total = refunds.count(userId, keyword, status, start, end);
        return new RefundDTO.Page(refunds.search(userId, keyword, status, start, end, page).stream().map(this::item).toList(),
                page, 10, total, (total + 9) / 10, settings.isEnabled(), RefundSettings.POLICY);
    }

    public RefundDTO.Item get(String userId, String id) { return item(owned(userId, id)); }

    public RefundDTO.Item create(String userId, String orderId, String key, RefundDTO.Create body) {
        requireEnabled();
        requireId(orderId);
        if (key == null || !key.matches("[a-zA-Z0-9-]{16,80}")) throw error(400, "Idempotency-Key không hợp lệ.");
        String reason = reason(body == null ? null : body.getReason());
        if (!RefundSettings.POLICY.equals(body.getPolicyVersion())) throw error(400, "Vui lòng xác nhận điều khoản hoàn tiền hiện tại.");
        try {
            return item(transactions.run(() -> {
                RefundEntity previous = refunds.findAttempt(userId, key);
                if (previous != null) {
                    if (!orderId.equals(previous.getOrderId())) throw error(409, "Mã yêu cầu đã được dùng cho đơn khác.");
                    return previous;
                }
                PaymentOrderEntity order = ownedOrder(userId, orderId);
                RefundAccountEntity account = ai.lockedAccount(userId);
                RefundEntity active = refunds.activeForOrder(orderId);
                if (active != null || order.getRefundedAt() != null) throw error(409, "Đơn đã được hoàn hoặc có yêu cầu đang xử lý.");
                RefundEntity refund = new RefundEntity();
                refund.setId(UUID.randomUUID().toString().replace("-", ""));
                refund.setOrderId(orderId); refund.setUserId(userId); refund.setEmail(order.getEmail());
                refund.setIdempotencyKey(key); refund.setAmount(order.getAmount()); refund.setReason(reason);
                refund.setCreatedAt(Instant.now()); refund.setPolicyVersion(RefundSettings.POLICY);
                assess(refund, order, account);
                String status = "INELIGIBLE".equals(refund.getEligibility()) ? "REJECTED"
                        : "ELIGIBLE".equals(refund.getEligibility()) ? "PENDING_REVIEW" : "NEEDS_VERIFICATION";
                if (!"REJECTED".equals(status)) {
                    account.getHolds().add(refund.getId()); ai.save(account, account.getRevision());
                }
                event(refund, status, "SYSTEM", refund.getDecisionReason());
                refunds.insert(refund); notifyUser(refund);
                return refund;
            }));
        } catch (DuplicateKeyException duplicate) {
            RefundEntity previous = refunds.findAttempt(userId, key);
            if (previous != null && orderId.equals(previous.getOrderId())) return item(previous);
            throw error(409, "Yêu cầu đang thay đổi. Vui lòng tải lại.");
        }
    }

    public RefundDTO.Item decide(String userId, String id, String actor, String action, RefundDTO.Decision body) {
        requireEnabled();
        if (body == null || body.getRevision() == null) throw error(400, "Thiếu revision của yêu cầu.");
        if ("approve".equals(action)) {
            RefundEntity snapshot = owned(userId, id);
            checkRevision(snapshot, body.getRevision());
            if (!REVIEW.contains(snapshot.getStatus())) throw error(409, "Yêu cầu không còn chờ duyệt.");
            PaymentOrderEntity order = payments.find(snapshot.getOrderId());
            if (order == null || !vnpay.available()) throw error(503, "Chưa thể xác minh giao dịch gốc.");
            try {
                Map<String, String> original = gateway.query(order);
                if (!matches(order, original, "querydr") || !"00".equals(original.get("vnp_ResponseCode"))
                        || !"00".equals(original.get("vnp_TransactionStatus")) || !"01".equals(original.get("vnp_TransactionType"))
                        || !Objects.equals(order.getTransactionNo(), original.get("vnp_TransactionNo")))
                    throw error(503, "Không xác minh được giao dịch gốc; chưa gửi lệnh hoàn tiền.");
            } catch (com.wedu.exam_creation.payment.infrastructure.VnpayQueryException unavailable) {
                throw error(503, "Chưa xác minh được giao dịch gốc; chưa gửi lệnh hoàn tiền.");
            }
        }
        RefundEntity result = transactions.run(() -> {
            RefundEntity refund = owned(userId, id);
            checkRevision(refund, body.getRevision());
            if (!REVIEW.contains(refund.getStatus())) throw error(409, "Yêu cầu không còn chờ duyệt.");
            RefundAccountEntity account = ai.lockedAccount(refund.getUserId());
            if ("cancel".equals(action) || "reject".equals(action)) {
                refund.setAdminId("reject".equals(action) ? actor : null);
                refund.setDecisionReason("cancel".equals(action) ? "Người dùng hủy yêu cầu." : reason(body.getReason()));
                finish(refund, account, "cancel".equals(action) ? "CANCELLED" : "REJECTED", actor);
                return refund;
            }
            PaymentOrderEntity order = payments.find(refund.getOrderId());
            assess(refund, order, account);
            if ("INELIGIBLE".equals(refund.getEligibility())) {
                finish(refund, account, "REJECTED", "SYSTEM"); return refund;
            }
            if (order == null || !account.getActiveAi().isEmpty() || entitlement.preview(order, Instant.now()) == null)
                throw error(409, "Cần xác minh tác vụ AI hoặc thời hạn PLUS trước khi hoàn tiền.");
            if ("UNKNOWN".equals(refund.getEligibility())) {
                if (order.isRefundTracked() || !body.isLegacyException()) throw error(409, "Cần xác nhận ngoại lệ cho dữ liệu sử dụng AI cũ.");
                refund.setDecisionReason("Ngoại lệ dữ liệu AI cũ: " + reason(body.getReason()));
            } else refund.setDecisionReason("ADMIN xác nhận hoàn tiền.");
            if (!vnpay.available()) throw error(503, "VNPay chưa sẵn sàng.");
            refund.setAdminId(actor);
            // Persist the send intent first. A crash from this point must only reconcile, never resend.
            refund.setRequestId(UUID.randomUUID().toString().replace("-", ""));
            refund.setSubmittedAt(Instant.now());
            event(refund, "PROCESSING", actor, refund.getDecisionReason());
            save(refund); notifyUser(refund);
            return refund;
        });
        if ("PROCESSING".equals(result.getStatus())) {
            // Verify the original transaction before issuing a new refund.
            try {
                PaymentOrderEntity order = payments.find(result.getOrderId());
                applyProvider(result.getId(), gateway.refund(order, result), "refund");
            } catch (RuntimeException unavailable) {
                pending(result.getId(), "Chưa xác định kết quả VNPay. Hệ thống chỉ đối soát, không gửi lại lệnh hoàn tiền.");
            }
        }
        return get(userId, result.getId());
    }

    public RefundDTO.Item reconcile(String id) {
        requireEnabled();
        RefundEntity refund = owned(null, id);
        if (!"PROCESSING".equals(refund.getStatus())) throw error(409, "Yêu cầu không ở trạng thái đối soát.");
        Instant now = Instant.now();
        if (!refunds.claimPoll(id, now)) throw error(409, "Vui lòng đợi 5 phút giữa các lần đối soát.");
        try {
            applyProvider(id, gateway.queryRefund(payments.find(refund.getOrderId()), refund.getRefundTransactionNo()), "querydr");
        } catch (RuntimeException unavailable) { pending(id, "Chưa xác minh được kết quả hoàn tiền từ VNPay."); }
        return get(null, id);
    }

    public void reconcileDue() {
        if (!settings.isEnabled() || !vnpay.available()) return;
        for (RefundEntity refund : refunds.due(Instant.now())) {
            try { reconcile(refund.getId()); } catch (RuntimeException ignored) { /* Retained for a later reconciliation. */ }
        }
    }

    private void applyProvider(String id, Map<String, String> response, String command) {
        transactions.run(() -> {
            RefundEntity refund = owned(null, id);
            if (!"PROCESSING".equals(refund.getStatus())) return null;
            PaymentOrderEntity order = payments.find(refund.getOrderId());
            if (!matches(order, response, command)) throw error(503, "Phản hồi VNPay không khớp giao dịch.");
            String code = response.get("vnp_ResponseCode"), status = response.get("vnp_TransactionStatus");
            refund.setProviderCode(code); refund.setProviderStatus(status);
            if ("02".equals(response.get("vnp_TransactionType"))) {
                String number = response.get("vnp_TransactionNo");
                if (number != null && number.matches("[0-9]{1,15}") && !"0".equals(number)) refund.setRefundTransactionNo(number);
            }
            RefundAccountEntity account = ai.lockedAccount(refund.getUserId());
            if ("00".equals(code) && "00".equals(status) && "02".equals(response.get("vnp_TransactionType"))
                    && refund.getRefundTransactionNo() != null && !refund.getRefundTransactionNo().equals(order.getTransactionNo())) {
                if (!entitlement.revoke(order, Instant.now())) {
                    refund.setPollingStopped(true);
                    refund.setDecisionReason("VNPay xác nhận hoàn tiền; cần xác minh quyền PLUS trước khi hoàn tất nội bộ.");
                    save(refund); return null;
                }
                refund.setDecisionReason("VNPay xác nhận hoàn toàn bộ " + refund.getAmount() + " VND. Đã thu hồi phần PLUS của đơn.");
                finish(refund, account, "REFUNDED", "VNPAY");
            } else if (("refund".equals(command) && "95".equals(code))
                    || ("00".equals(code) && "02".equals(response.get("vnp_TransactionType")) && "09".equals(status))) {
                refund.setDecisionReason("VNPay từ chối hoàn tiền. Mã phản hồi: " + code + ", trạng thái: " + status);
                finish(refund, account, "FAILED", "VNPAY");
            } else {
                refund.setDecisionReason("VNPay chưa xác nhận hoàn tiền hoàn tất.");
                stopOldPolling(refund); save(refund);
            }
            return null;
        });
    }

    private boolean matches(PaymentOrderEntity order, Map<String, String> response, String command) {
        if (order == null || response == null || !command.equals(response.get("vnp_Command"))
                || !vnpay.getTmnCode().equals(response.get("vnp_TmnCode")) || !order.getId().equals(response.get("vnp_TxnRef"))) return false;
        try { return Long.parseLong(response.get("vnp_Amount")) == Math.multiplyExact(order.getAmount(), 100); }
        catch (RuntimeException invalid) { return false; }
    }

    private void pending(String id, String reason) {
        transactions.run(() -> {
            RefundEntity refund = owned(null, id);
            if ("PROCESSING".equals(refund.getStatus())) {
                refund.setDecisionReason(reason); stopOldPolling(refund); save(refund);
            }
            return null;
        });
    }
    private void stopOldPolling(RefundEntity refund) {
        if (refund.getSubmittedAt() != null && refund.getSubmittedAt().plusSeconds(86400).isBefore(Instant.now())) {
            refund.setPollingStopped(true);
            refund.setDecisionReason(refund.getDecisionReason() + " Quá 24 giờ; ADMIN cần xác minh.");
        }
    }

    private void assess(RefundEntity refund, PaymentOrderEntity order, RefundAccountEntity account) {
        refund.setChecks(new ArrayList<>());
        String failure = null;
        if (order == null) { unknown(refund, "Không còn dữ liệu đơn gốc."); return; }
        refund.getChecks().add(new RefundEntity.Check("PAYMENT", "SUCCEEDED".equals(order.getStatus()) ? "PASSED" : "FAILED", "Giao dịch đã thanh toán thành công."));
        refund.getChecks().add(new RefundEntity.Check("WINDOW", order.getPaidAt() == null ? "UNKNOWN"
                : refund.getCreatedAt().isAfter(order.getPaidAt().plusSeconds(172800)) ? "FAILED" : "PASSED", "Gửi trong 48 giờ kể từ khi cấp PLUS."));
        refund.getChecks().add(new RefundEntity.Check("NOT_REFUNDED", order.getRefundedAt() == null ? "PASSED" : "FAILED", "Giao dịch chưa được hoàn tiền trước đó."));
        if (!"SUCCEEDED".equals(order.getStatus())) failure = "Đơn chưa thanh toán thành công.";
        else if (order.getRefundedAt() != null) failure = "Đơn đã được hoàn tiền.";
        else if (order.getPaidAt() != null && refund.getCreatedAt().isAfter(order.getPaidAt().plusSeconds(172800))) failure = "Yêu cầu được gửi quá 48 giờ kể từ khi cấp PLUS.";
        Instant usedAt = refunds.lastAiUse(refund.getUserId());
        if (account.getLastAiUsedAt() != null && (usedAt == null || account.getLastAiUsedAt().isAfter(usedAt))) usedAt = account.getLastAiUsedAt();
        boolean aiUsed = order.getPaidAt() != null && usedAt != null && !usedAt.isBefore(order.getPaidAt());
        refund.getChecks().add(new RefundEntity.Check("AI_UNUSED", aiUsed ? "FAILED" : !account.getActiveAi().isEmpty() || !order.isRefundTracked() ? "UNKNOWN" : "PASSED",
                "Chưa có kết quả sinh đề AI được lưu sau khi cấp PLUS; tác vụ đang chạy và dữ liệu cũ cần xác minh."));
        refund.getChecks().add(new RefundEntity.Check("ENTITLEMENT", entitlement.preview(order, Instant.now()) == null ? "UNKNOWN" : "PASSED", "Xác định được phần PLUS của đơn cần thu hồi."));
        if (order.getPaidAt() != null && usedAt != null && !usedAt.isBefore(order.getPaidAt())) failure = "Đã nhận kết quả sinh đề AI sau khi được cấp PLUS.";
        if (failure != null) { refund.setEligibility("INELIGIBLE"); refund.setDecisionReason(failure); return; }
        if (order.getPaidAt() == null || order.getCreatedAt() == null || order.getTransactionNo() == null || order.getAmount() <= 0) {
            unknown(refund, "Thiếu thông tin đối chiếu thanh toán."); return;
        }
        if (!account.getActiveAi().isEmpty()) { unknown(refund, "Có tác vụ AI đang chạy hoặc chưa xác định kết quả."); return; }
        if (entitlement.preview(order, Instant.now()) == null) { unknown(refund, "Chưa xác minh được thời hạn PLUS theo đơn."); return; }
        if (!order.isRefundTracked()) { unknown(refund, "Đơn cũ chưa có bằng chứng sử dụng AI đầy đủ; cần ADMIN xét ngoại lệ."); return; }
        refund.setEligibility("ELIGIBLE"); refund.setDecisionReason("Trong 48 giờ, chưa dùng AI và chưa hoàn tiền.");
    }
    private void unknown(RefundEntity refund, String reason) { refund.setEligibility("UNKNOWN"); refund.setDecisionReason(reason); }
    private void finish(RefundEntity refund, RefundAccountEntity account, String status, String actor) {
        account.getHolds().remove(refund.getId()); ai.save(account, account.getRevision());
        event(refund, status, actor, refund.getDecisionReason()); save(refund); notifyUser(refund);
    }
    private void save(RefundEntity refund) {
        refund.setUpdatedAt(Instant.now());
        if (!refunds.update(refund, refund.getRevision())) throw new OptimisticLockingFailureException("Refund changed");
    }
    private void event(RefundEntity refund, String status, String actor, String reason) {
        refund.setStatus(status); refund.setUpdatedAt(Instant.now());
        RefundEntity.Event event = new RefundEntity.Event();
        event.setAt(refund.getUpdatedAt()); event.setStatus(status); event.setActor(actor); event.setReason(reason);
        refund.getHistory().add(event);
    }
    private void notifyUser(RefundEntity refund) {
        PaymentEmailEntity email = new PaymentEmailEntity();
        email.setId("refund:" + refund.getId() + ":" + refund.getHistory().size());
        email.setRefundId(refund.getId()); email.setOrderId(refund.getOrderId()); email.setEmail(refund.getEmail());
        email.setStatus(refund.getStatus()); email.setNextAttemptAt(Instant.now());
        email.setMessage(label(refund.getStatus()) + "\nSố tiền: " + refund.getAmount() + " VND.\n" + refund.getDecisionReason()
                + "\nTheo dõi tại mục Thanh toán → Giao dịch và hoàn tiền trong tài khoản WEdu.");
        payments.enqueueEmail(email);
    }
    private String label(String status) {
        return switch (status) {
            case "REFUNDED" -> "Hoàn tiền thành công";
            case "REJECTED" -> "Yêu cầu bị từ chối";
            case "CANCELLED" -> "Yêu cầu đã hủy";
            case "FAILED" -> "Hoàn tiền không thành công";
            case "PROCESSING" -> "Đang xử lý hoàn tiền qua VNPay";
            case "NEEDS_VERIFICATION" -> "Yêu cầu cần xác minh";
            default -> "Đã tiếp nhận yêu cầu hoàn tiền";
        };
    }
    private RefundDTO.Item item(RefundEntity refund) {
        PaymentOrderEntity order = payments.find(refund.getOrderId());
        RefundAccountEntity account = refunds.account(refund.getUserId());
        Instant preview = order == null || order.getRefundedAt() != null ? null : entitlement.preview(order, Instant.now());
        return new RefundDTO.Item(refund.getId(), refund.getOrderId(), refund.getEmail(), refund.getAmount(), refund.getStatus(),
                refund.getReason(), refund.getPolicyVersion(), refund.getEligibility(), refund.getDecisionReason(),
                refund.getCreatedAt(), refund.getUpdatedAt(), order == null ? null : order.getPaidAt(), refunds.lastAiUse(refund.getUserId()),
                account != null && !account.getActiveAi().isEmpty(), preview, preview != null,
                refund.getProviderCode(), refund.getProviderStatus(), refund.getRefundTransactionNo(), refund.getRevision(),
                refund.getHistory().stream().map(e -> new RefundDTO.Event(e.getAt(), e.getStatus(), e.getActor(), e.getReason())).toList(),
                refund.getChecks().stream().map(c -> new RefundDTO.Check(c.getKey(), c.getResult(), c.getReason())).toList());
    }
    private PaymentOrderEntity ownedOrder(String userId, String id) {
        PaymentOrderEntity order = payments.find(id);
        if (order == null || !userId.equals(order.getUserId())) throw error(404, "Không tìm thấy đơn.");
        return order;
    }
    private RefundEntity owned(String userId, String id) {
        if (id == null || !id.matches("[a-f0-9]{32}")) throw error(400, "Mã yêu cầu không hợp lệ.");
        RefundEntity refund = refunds.find(id);
        if (refund == null || (userId != null && !userId.equals(refund.getUserId()))) throw error(404, "Không tìm thấy yêu cầu.");
        return refund;
    }
    private void checkRevision(RefundEntity refund, long revision) {
        if (refund.getRevision() != revision) throw error(409, "Yêu cầu đã thay đổi. Vui lòng tải lại.");
    }
    private String reason(String value) {
        if (value == null || value.trim().length() < 3 || value.trim().length() > 1000) throw error(400, "Lý do phải dài từ 3 đến 1000 ký tự.");
        return value.trim();
    }
    private Instant date(String date, boolean end) {
        if (date == null || date.isEmpty()) return null;
        try { return LocalDate.parse(date).plusDays(end ? 1 : 0).atStartOfDay(ZONE).toInstant(); }
        catch (DateTimeException e) { throw error(400, "Ngày không hợp lệ."); }
    }
    private void requirePage(int page) { if (page < 0 || page > 1000000) throw error(400, "Trang không hợp lệ."); }
    private void requireId(String id) { if (id == null || !id.matches("[a-f0-9]{64}")) throw error(400, "Mã đơn không hợp lệ."); }
    private void requireEnabled() { if (!settings.isEnabled()) throw error(503, "Tính năng hoàn tiền chưa được bật."); }
    private AdminTransactionException error(int status, String message) { return new AdminTransactionException(status, "REFUND_ERROR", message); }
}
