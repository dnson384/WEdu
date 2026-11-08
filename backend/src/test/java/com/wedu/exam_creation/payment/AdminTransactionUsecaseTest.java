package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import com.wedu.exam_creation.payment.usecase.AdminTransactionUsecase;
import com.wedu.exam_creation.payment.usecase.AdminTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminTransactionUsecaseTest {
    final IPaymentRepository repository = mock(IPaymentRepository.class);
    final VnpayGateway gateway = mock(VnpayGateway.class);
    final VnpayProperties properties = mock(VnpayProperties.class);
    final AdminTransactionUsecase usecase = new AdminTransactionUsecase(repository, gateway, properties);
    final String id = "a".repeat(64);
    PaymentOrderEntity order;

    @BeforeEach void setup() {
        order = new PaymentOrderEntity();
        order.setId(id);
        order.setStatus("PENDING");
        order.setAmount(20000);
        order.setCreatedAt(Instant.parse("2026-11-08T00:00:00Z"));
        when(repository.find(id)).thenReturn(order);
        when(properties.available()).thenReturn(true);
        when(properties.getTmnCode()).thenReturn("MERCHANT");
    }

    Map<String, String> response() {
        return new HashMap<>(Map.of("vnp_TxnRef", id, "vnp_TmnCode", "MERCHANT", "vnp_Command", "querydr",
                "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_Amount", "2000000",
                "vnp_PayDate", "20261108073000", "vnp_TransactionNo", "123"));
    }

    @Test void lookupIsReadOnlyAndUsesVietnamTimeAndMinorUnits() {
        when(gateway.query(order)).thenReturn(response());
        var result = usecase.lookup(id);
        assertThat(result.getProvider().getAmount()).isEqualByComparingTo("20000");
        assertThat(result.getProvider().getPaidAt()).isEqualTo(Instant.parse("2026-11-08T00:30:00Z"));
        assertThat(result.getOrder().getStatus()).isEqualTo("PENDING");
        assertThat(result.isAmountMismatch()).isFalse();
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verify(repository).find(id);
        verifyNoMoreInteractions(repository);
    }

    @Test void querySuccessDoesNotImplyPaymentSuccessAndMismatchIsVisible() {
        var response = response();
        response.put("vnp_TransactionStatus", "01");
        response.put("vnp_Amount", "3000000");
        response.remove("vnp_PayDate");
        when(gateway.query(order)).thenReturn(response);
        var result = usecase.lookup(id);
        assertThat(result.getProvider().getTransactionStatus()).isEqualTo("01");
        assertThat(result.getProvider().getPaidAt()).isNull();
        assertThat(result.isAmountMismatch()).isTrue();
        verify(repository).find(id);
        verifyNoMoreInteractions(repository);
    }

    @Test void providerNotFoundIsDistinctFromUnavailable() {
        var response = response();
        response.put("vnp_ResponseCode", "91");
        when(gateway.query(order)).thenReturn(response);
        assertThatThrownBy(() -> usecase.lookup(id)).isInstanceOfSatisfying(AdminTransactionException.class,
                e -> { assertThat(e.getStatus()).isEqualTo(404); assertThat(e.getCode()).isEqualTo("VNPAY_TRANSACTION_NOT_FOUND"); });
    }

    @Test void missingOrderDoesNotCallProvider() {
        when(repository.find(id)).thenReturn(null);
        assertThatThrownBy(() -> usecase.lookup(id)).isInstanceOfSatisfying(AdminTransactionException.class,
                e -> assertThat(e.getCode()).isEqualTo("ORDER_NOT_FOUND"));
        verifyNoInteractions(gateway);
    }

    @Test void transportAndUnverifiedResponsesRemainUnavailable() {
        when(gateway.query(order)).thenThrow(new IllegalStateException("unverified"));
        assertUnavailable();
        verify(repository).find(id);
        verifyNoMoreInteractions(repository);
    }

    @Test void unavailableConfigurationDoesNotCallGateway() {
        when(properties.available()).thenReturn(false);
        assertUnavailable();
        verifyNoInteractions(gateway);
    }

    @Test void invalidProviderFieldsFailClosed() {
        for (String key : List.of("vnp_TxnRef", "vnp_TmnCode", "vnp_Command", "vnp_Amount", "vnp_TransactionStatus", "vnp_PayDate", "vnp_ResponseCode")) {
            var response = response();
            response.put(key, "invalid");
            when(gateway.query(order)).thenReturn(response);
            assertUnavailable();
        }
    }

    @Test void filtersUseInclusiveVietnamDaysAndFixedPageSize() {
        Instant start = Instant.parse("2026-11-07T17:00:00Z");
        Instant end = Instant.parse("2026-11-08T17:00:00Z");
        when(repository.countTransactions("mail", "PENDING", start, end)).thenReturn(11L);
        when(repository.searchTransactions("mail", "PENDING", start, end, 1)).thenReturn(List.of(order));
        var result = usecase.search(" mail ", "PENDING", "2026-11-08", "2026-11-08", 1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(repository).searchTransactions("mail", "PENDING", start, end, 1);
        verifyNoInteractions(gateway);
    }

    @Test void invalidFiltersFailBeforeReadingDatabase() {
        assertThatThrownBy(() -> usecase.search("", "BAD", "", "", 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("", "", "2026-02-30", "", 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("", "", "2026-11-09", "2026-11-08", 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("", "", "", "", -1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.search("x".repeat(255), "", "", "", 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> usecase.lookup("invalid")).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository, gateway);
    }

    void assertUnavailable() {
        assertThatThrownBy(() -> usecase.lookup(id)).isInstanceOfSatisfying(AdminTransactionException.class,
                e -> { assertThat(e.getStatus()).isEqualTo(503); assertThat(e.getCode()).isEqualTo("TRANSACTION_LOOKUP_UNAVAILABLE"); });
    }
}
