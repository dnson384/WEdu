package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.vnpay.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentJobs {
    private final IPaymentRepository repository;
    private final PaymentUsecase payments;
    private final MailService mail;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PaymentJobs.class);

    @Scheduled(fixedDelay = 60000)
    public void expireOrders() {
        for (var order : repository.expiredPending(Instant.now())) {
            try { payments.expire(order.getId()); }
            catch (RuntimeException e) { LOG.warn("Payment expiration will be retried"); }
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void sendEmails() {
        for (int i = 0; i < 20; i++) {
            String token = UUID.randomUUID().toString();
            PaymentEmailEntity email = repository.claimEmail(Instant.now(), token);
            if (email == null) return;
            boolean sent = false;
            try {
                if (email.getRefundId() != null) mail.sendRefundResultEmail(email.getEmail(), email.getRefundId(), email.getMessage());
                else mail.sendPaymentResultEmail(email.getEmail(), email.getOrderId(), message(email));
                sent = true;
            } catch (RuntimeException e) {
                LOG.warn("Payment email attempt failed (attempt {})", email.getAttempts());
            }
            repository.completeEmail(email.getId(), token, Instant.now(), sent);
        }
    }

    private String message(PaymentEmailEntity email) {
        return switch (email.getStatus()) {
            case "SUCCEEDED" -> email.isManualReview()
                    ? "Đã nhận thanh toán 20.000đ. Tài khoản không còn tồn tại; vui lòng liên hệ hỗ trợ với mã đơn để xử lý."
                    : "Thanh toán 20.000đ thành công. PLUS có hiệu lực đến " +
                    DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                            .format(email.getPlusExpiresAt()) + " (giờ Việt Nam).";
            case "CANCELLED" -> "Thanh toán đã bị hủy. Tài khoản chưa được nâng cấp từ đơn này.";
            case "EXPIRED" -> "Đã hết thời gian thanh toán và WEdu chưa nhận xác nhận thành công. Nếu đã bị trừ tiền, hãy kiểm tra lại đơn hoặc liên hệ hỗ trợ.";
            default -> "Thanh toán không thành công. Tài khoản chưa được nâng cấp từ đơn này.";
        };
    }
}
