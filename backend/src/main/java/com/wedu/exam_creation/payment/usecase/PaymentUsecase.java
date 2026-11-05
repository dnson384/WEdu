package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.ServiceUnavailableException;
import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.dto.PaymentDTO;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import com.wedu.exam_creation.user.usecase.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class PaymentUsecase {
    private final IPaymentRepository repository;
    private final UserService userService;
    private final VnpayGateway gateway;
    private final VnpayProperties properties;
    private final TransactionTemplate transaction;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PaymentUsecase.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public PaymentUsecase(
            IPaymentRepository repository,
            UserService userService,
            VnpayGateway gateway,
            VnpayProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.userService = userService;
        this.gateway = gateway;
        this.properties = properties;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    private static String idempotencyKeyHash(String userId, String key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((userId + ":" + key).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private PaymentOrderEntity findAttempt(String hash) {
        PaymentOrderEntity order = repository.findByIdempotencyKeyHash(hash);
        // Legacy orders used the request hash as their reference; retain retry compatibility.
        return order != null ? order : repository.find(hash);
    }

    private static String newOrderId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public PaymentDTO.Plan plan() {
        return new PaymentDTO.Plan(20000, "VND", 30, properties.available());
    }

    public PaymentDTO.Order create(CommonUserResponseAllDTO user, String key, String ip) {
        requireAvailable();

        if (!"ROLE_TEACHER".equals(user.getRole()) || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new ForbiddenException("Chỉ giáo viên đang hoạt động được nâng cấp tài khoản");
        }

        if (key == null || !key.matches("[a-zA-Z0-9-]{16,80}")) {
            throw new BadRequestException("Idempotency-Key không hợp lệ");
        }

        String hash = idempotencyKeyHash(user.getId(), key);
        PaymentOrderEntity order = findAttempt(hash);
        if (order == null) {
            order = new PaymentOrderEntity();
            order.setId(newOrderId());
            order.setIdempotencyKeyHash(hash);
            order.setUserId(user.getId());
            order.setEmail(user.getEmail());
            order.setAmount(20000);
            order.setDurationDays(30);
            order.setStatus("PENDING");
            order.setCreatedAt(Instant.now());
            order.setExpiresAt(order.getCreatedAt().plusSeconds(300));
            order.setClientIp(ip);

            try {
                order = repository.insert(order);
            } catch (DuplicateKeyException e) {
                order = findAttempt(hash);
                if (order == null) throw e;
            }
        }
        if (!user.getId().equals(order.getUserId())) {
            throw new NotFoundException("Không tìm thấy đơn thanh toán");
        }
        return view(order);
    }

    public PaymentDTO.Order get(String userId, String id) {
        PaymentOrderEntity order = owned(userId, id);

        if ("PENDING".equals(order.getStatus()) && !order.getExpiresAt().isAfter(Instant.now())) {
            expire(id);
            order = owned(userId, id);
        }

        return view(order);
    }

    public PaymentDTO.Order verify(String userId, String id) {
        requireAvailable();

        PaymentOrderEntity order = owned(userId, id);
        if ("SUCCEEDED".equals(order.getStatus())) {
            return view(order);
        }

        if (!repository.claimVerification(id, userId, Instant.now())) {
            return get(userId, id);
        }

        Map<String, String> result;
        try {
            result = gateway.query(order);
        } catch (RuntimeException e) {
            LOG.warn("VNPay verification unavailable: exceptionType={}", e.getClass().getSimpleName());
            throw verificationUnavailable();
        }

        if (!id.equals(result.get("vnp_TxnRef")) ||
                !properties.getTmnCode().equals(result.get("vnp_TmnCode"))
        ) {
            LOG.warn("VNPay query rejected: merchant or reference mismatch");
            throw verificationUnavailable();
        }

        if ("00".equals(result.get("vnp_ResponseCode"))) {
            String code = process(result, true);

            if (!"00".equals(code) && !"02".equals(code)) {
                throw verificationUnavailable();
            }
        } else if (!Set.of("91", "94").contains(result.getOrDefault("vnp_ResponseCode", ""))) {
            String code = result.get("vnp_ResponseCode");
            LOG.warn("VNPay signed query error: providerCode={}", code != null && code.matches("[0-9]{2}") ? code : "UNKNOWN");
            throw verificationUnavailable();
        }

        return get(userId, id);
    }

    private ServiceUnavailableException verificationUnavailable() {
        return new ServiceUnavailableException("Chưa thể đối soát với VNPay. Đơn đang chờ xác nhận; vui lòng kiểm tra lại sau.");
    }

    public PaymentDTO.Order confirmReturn(String userId, String id, PaymentDTO.ReturnConfirmation request) {
        requireAvailable();
        owned(userId, id);
        Map<String, String> values = new HashMap<>();
        if (request == null || request.getParameters() == null || request.getParameters().isEmpty()
                || request.getParameters().size() > 32) {
            throw new BadRequestException("Dữ liệu kết quả thanh toán không hợp lệ");
        }
        for (var pair : request.getParameters()) {
            if (pair == null || pair.size() != 2 || pair.get(0) == null || pair.get(1) == null
                    || !pair.get(0).matches("vnp_[a-zA-Z0-9_]{1,64}") || pair.get(1).length() > 1024
                    || values.putIfAbsent(pair.get(0), pair.get(1)) != null) {
                throw new BadRequestException("Dữ liệu kết quả thanh toán không hợp lệ");
            }
        }
        if (!gateway.validIpn(values) || !id.equals(values.get("vnp_TxnRef"))
                || !properties.getTmnCode().equals(values.get("vnp_TmnCode"))
                || !values.getOrDefault("vnp_ResponseCode", "").matches("[0-9]{2}")
                || !values.getOrDefault("vnp_TransactionStatus", "").matches("[0-9]{2}")) {
            throw new BadRequestException("Không thể xác thực kết quả thanh toán trả về từ VNPay");
        }
        String code = process(values, false);
        if ("01".equals(code)) throw new NotFoundException("Không tìm thấy đơn thanh toán");
        if (!Set.of("00", "02").contains(code)) {
            throw new BadRequestException("Dữ liệu kết quả thanh toán không khớp với đơn hàng");
        }
        return get(userId, id);
    }

    public Map<String, String> ipn(Map<String, String> values) {
        if (!gateway.validIpn(values)) {
            LOG.warn("VNPay IPN rejected: signature or merchant configuration invalid");
            return acknowledgement("97");
        }

        try {
            return acknowledgement(process(values, false));
        } catch (RuntimeException e) {
            LOG.warn("VNPay IPN processing failed: exceptionType={}", e.getClass().getSimpleName());
            return acknowledgement("99");
        }
    }

    private String process(Map<String, String> values, boolean query) {
        if (!properties.getTmnCode().equals(values.get("vnp_TmnCode"))) {
            return "97";
        }

        String id = values.get("vnp_TxnRef");
        if (id == null || !id.matches("[a-f0-9]{64}")) {
            return "01";
        }

        return inTransaction(() -> {
            PaymentOrderEntity order = repository.find(id);

            if (order == null) {
                return "01";
            }
            if (!Long.toString(order.getAmount() * 100).equals(values.get("vnp_Amount"))) {
                return "04";
            }
            if ("SUCCEEDED".equals(order.getStatus())) {
                return "02";
            }

            String response = values.get("vnp_ResponseCode");
            String status = values.get("vnp_TransactionStatus");
            if (response == null || status == null) {
                return "99";
            }

            boolean success = "00".equals(response) && "00".equals(status);

            String next = success ? "SUCCEEDED" :
                    !query && "24".equals(response) ? "CANCELLED" :
                    "02".equals(status) ? "FAILED" :
                    null;
            if (next == null) {
                return "00";
            }
            if (next.equals(order.getStatus()) ||
                    (!success && !"PENDING".equals(order.getStatus()))) {
                return "02";
            }
            if (success &&
                    (values.get("vnp_TransactionNo") == null
                            || !values.get("vnp_TransactionNo").matches("[0-9]{1,15}")
                            || "0".equals(values.get("vnp_TransactionNo"))
                            || (query && !"01".equals(values.get("vnp_TransactionType")))
                    )
            ) {
                return "99";
            }

            order.setStatus(next);
            order.setResponseCode(response);
            order.setTransactionStatus(status);
            order.setTransactionNo(values.get("vnp_TransactionNo"));

            Instant now = Instant.now();
            if (success) {
                order.setPaidAt(now);

                Instant expiry = userService.grantPaidPlus(order.getUserId(), now, order.getDurationDays());
                order.setPlusExpiresAt(expiry);
                order.setManualReview(expiry == null);
            }

            saveResult(order, now);
            return "00";
        });
    }

    public void expire(String id) {
        inTransaction(() -> {
            PaymentOrderEntity order = repository.find(id);

            if (order != null &&
                    "PENDING".equals(order.getStatus()) &&
                    !order.getExpiresAt().isAfter(Instant.now())
            ) {
                order.setStatus("EXPIRED");
                saveResult(order, Instant.now());
            }

            return null;
        });
    }

    private void saveResult(PaymentOrderEntity order, Instant now) {
        if (!repository.update(order, order.getRevision())) {
            throw new OptimisticLockingFailureException("Payment changed");
        }

        PaymentEmailEntity email = new PaymentEmailEntity();
        email.setId(order.getId() + ":" + order.getStatus());
        email.setOrderId(order.getId());
        email.setEmail(order.getEmail());
        email.setStatus(order.getStatus());
        email.setPlusExpiresAt(order.getPlusExpiresAt());
        email.setManualReview(order.isManualReview());
        email.setNextAttemptAt(now);

        repository.enqueueEmail(email);
    }

    private <T> T inTransaction(Supplier<T> operation) {
        for (int attempt = 0; ; attempt++) {
            try {
                return transaction.execute(ignored -> operation.get());
            } catch (RuntimeException e) {
                if (attempt >= 2 || !retryable(e)) throw e;
            }
        }
    }

    private boolean retryable(Throwable error) {
        for (Throwable e = error; e != null; e = e.getCause()) {
            if (e instanceof TransientDataAccessException) return true;
            if (e instanceof com.mongodb.MongoException mongo && (mongo.hasErrorLabel("TransientTransactionError")
                    || mongo.hasErrorLabel("UnknownTransactionCommitResult"))) return true;
        }
        return false;
    }

    private PaymentOrderEntity owned(String userId, String id) {
        if (id == null || !id.matches("[a-f0-9]{64}")) {
            throw new NotFoundException("Không tìm thấy đơn thanh toán");
        }

        PaymentOrderEntity order = repository.find(id);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new NotFoundException("Không tìm thấy đơn thanh toán");
        }

        return order;
    }

    private PaymentDTO.Order view(PaymentOrderEntity order) {
        boolean pending = "PENDING".equals(order.getStatus()) &&
                order.getExpiresAt().isAfter(Instant.now());

        return new PaymentDTO.Order(
                order.getId(),
                order.getAmount(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getPlusExpiresAt(),
                pending && properties.available() ? gateway.paymentUrl(order) : null,
                order.isManualReview());
    }

    private void requireAvailable() {
        if (!properties.available()) {
            throw new InternalServerException("Thanh toán VNPay chưa sẵn sàng");
        }
    }

    private Map<String, String> acknowledgement(String code) {
        return Map.of("RspCode", code, "Message", switch (code) {
            case "00" -> "Confirm Success";
            case "01" -> "Order not found";
            case "02" -> "Order already confirmed";
            case "04" -> "Invalid amount";
            case "97" -> "Invalid signature";
            default -> "Retry later";
        });
    }
}
