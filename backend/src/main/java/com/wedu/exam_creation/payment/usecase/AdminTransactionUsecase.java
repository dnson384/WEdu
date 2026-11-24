package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.dto.AdminTransactionDTO;
import java.time.*;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminTransactionUsecase {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> STATUSES =
            Set.of("PENDING", "SUCCEEDED", "FAILED", "CANCELLED", "EXPIRED");
    private final IPaymentRepository repository;

    public AdminTransactionDTO.Page search(
            String keyword, String status, String from, String to, int page) {
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
        return new AdminTransactionDTO.Page(
                repository.searchTransactions(term, state, start, end, page).stream()
                        .map(this::item)
                        .toList(),
                page,
                10,
                total,
                (total + 9) / 10);
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

    private AdminTransactionDTO.Item item(PaymentOrderEntity order) {
        return new AdminTransactionDTO.Item(
                order.getId(),
                order.getEmail(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getVnpayTransaction().getTransactionNo());
    }
}
