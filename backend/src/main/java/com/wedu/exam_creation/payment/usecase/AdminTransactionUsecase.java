package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.dto.AdminTransactionDTO;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminTransactionUsecase {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> STATUSES = Set.of("PENDING", "SUCCEEDED", "FAILED", "CANCELLED", "EXPIRED");
    private final IPaymentRepository repository;
    private final VnpayGateway gateway;
    private final VnpayProperties properties;

    public AdminTransactionDTO.Page search(String keyword, String status, String from, String to, int page) {
        String term = keyword == null ? "" : keyword.trim();
        String state = status == null ? "" : status.trim();
        if (term.length() > 254 || page < 0 || (!state.isEmpty() && !STATUSES.contains(state))) {
            throw new BadRequestException("Bộ lọc giao dịch không hợp lệ");
        }
        Instant start = date(from, false);
        Instant end = date(to, true);
        if (start != null && end != null && !start.isBefore(end)) {
            throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        long total = repository.countTransactions(term, state, start, end);
        return new AdminTransactionDTO.Page(repository.searchTransactions(term, state, start, end, page)
                .stream().map(this::item).toList(), page, 10, total, (total + 9) / 10);
    }

    private Instant date(String input, boolean exclusiveEnd) {
        if (input == null || input.isBlank()) return null;
        try {
            if (!input.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new DateTimeException("format");
            LocalDate value = LocalDate.parse(input);
            return (exclusiveEnd ? value.plusDays(1) : value).atStartOfDay(ZONE).toInstant();
        } catch (DateTimeException e) {
            throw new BadRequestException("Ngày không hợp lệ, cần định dạng yyyy-MM-dd");
        }
    }

    public AdminTransactionDTO.Lookup lookup(String id) {
        if (id == null || !id.matches("[a-f0-9]{64}")) throw new BadRequestException("Mã đơn không hợp lệ");
        PaymentOrderEntity order = repository.find(id);
        if (order == null) throw new AdminTransactionException(404, "ORDER_NOT_FOUND", "Không tìm thấy đơn giao dịch");
        if (!properties.available() || order.getCreatedAt() == null) throw unavailable();
        Map<String, String> result;
        try {
            result = gateway.query(order);
        } catch (RuntimeException e) {
            throw unavailable();
        }
        if (!id.equals(result.get("vnp_TxnRef")) || !properties.getTmnCode().equals(result.get("vnp_TmnCode"))
                || !"querydr".equals(result.get("vnp_Command"))) throw unavailable();
        if ("91".equals(result.get("vnp_ResponseCode"))) {
            throw new AdminTransactionException(404, "VNPAY_TRANSACTION_NOT_FOUND", "VNPay không tìm thấy giao dịch");
        }
        if (!"00".equals(result.get("vnp_ResponseCode"))) throw unavailable();
        try {
            String rawAmount = result.get("vnp_Amount");
            String status = result.get("vnp_TransactionStatus");
            if (rawAmount == null || !rawAmount.matches("[0-9]{1,18}") || status == null || !status.matches("[0-9]{2}")) {
                throw unavailable();
            }
            BigDecimal amount = new BigDecimal(rawAmount).movePointLeft(2);
            String payDate = result.get("vnp_PayDate");
            Instant paidAt = payDate == null || payDate.isBlank() ? null : LocalDateTime.parse(payDate,
                    DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT)).atZone(ZONE).toInstant();
            return new AdminTransactionDTO.Lookup(item(order), new AdminTransactionDTO.Provider(
                    result.get("vnp_TransactionNo"), amount, result.get("vnp_BankCode"), paidAt,
                    "00", status), amount.compareTo(BigDecimal.valueOf(order.getAmount())) != 0, Instant.now());
        } catch (DateTimeException | NumberFormatException e) {
            throw unavailable();
        }
    }

    private AdminTransactionDTO.Item item(PaymentOrderEntity order) {
        return new AdminTransactionDTO.Item(order.getId(), order.getEmail(), order.getAmount(),
                order.getStatus(), order.getCreatedAt(), order.getTransactionNo());
    }

    private AdminTransactionException unavailable() {
        return new AdminTransactionException(503, "TRANSACTION_LOOKUP_UNAVAILABLE", "Chưa thể tra cứu VNPay, vui lòng thử lại");
    }
}
