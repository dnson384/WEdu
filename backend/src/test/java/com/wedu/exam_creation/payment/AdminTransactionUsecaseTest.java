package com.wedu.exam_creation.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.dto.AdminTransactionDTO;
import com.wedu.exam_creation.payment.usecase.AdminTransactionUsecase;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminTransactionUsecaseTest {
    final IPaymentRepository repository = mock(IPaymentRepository.class);
    final AdminTransactionUsecase usecase = new AdminTransactionUsecase(repository);
    final String id = "a".repeat(64);
    PaymentOrderEntity order;

    @BeforeEach
    void setup() {
        order = new PaymentOrderEntity();
        order.setId(id);
        order.setVnpayTransaction(
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        null, null, null, null));
        order.setStatus("PENDING");
        order.setAmount(20000);
        order.setCreatedAt(Instant.parse("2026-11-08T00:00:00Z"));
    }

    @Test
    void filtersUseInclusiveVietnamDaysAndFixedPageSize() {
        Instant start = Instant.parse("2026-11-07T17:00:00Z");
        Instant end = Instant.parse("2026-11-08T17:00:00Z");
        when(repository.countTransactions("mail", "PENDING", start, end)).thenReturn(11L);
        when(repository.searchTransactions("mail", "PENDING", start, end, 1))
                .thenReturn(List.of(order));
        AdminTransactionDTO.Page result =
                usecase.search(" mail ", "PENDING", "2026-11-08", "2026-11-08", 1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(repository).searchTransactions("mail", "PENDING", start, end, 1);
    }

    @Test
    void invalidFiltersFailBeforeReadingDatabase() {
        assertThatThrownBy(() -> usecase.search("", "BAD", "", "", 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("", "", "2026-02-30", "", 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("", "", "2026-11-09", "2026-11-08", 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("", "", "", "", -1))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("x".repeat(255), "", "", "", 0))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void returnsStoredSuccessfulPaymentWithoutProviderConfigurationOrWrites() {
        order.setStatus("SUCCEEDED");
        order.getVnpayTransaction().setTransactionNo("123");
        when(repository.countTransactions(id, "SUCCEEDED", null, null)).thenReturn(1L);
        when(repository.searchTransactions(id, "SUCCEEDED", null, null, 0))
                .thenReturn(List.of(order));

        AdminTransactionDTO.Page result = usecase.search(id, "SUCCEEDED", "", "", 0);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStatus()).isEqualTo("SUCCEEDED");
        assertThat(result.getItems().get(0).getTransactionNo()).isEqualTo("123");
        verify(repository).countTransactions(id, "SUCCEEDED", null, null);
        verify(repository).searchTransactions(id, "SUCCEEDED", null, null, 0);
        verifyNoMoreInteractions(repository);
    }
}
