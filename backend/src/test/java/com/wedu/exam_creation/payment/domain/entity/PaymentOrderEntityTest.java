package com.wedu.exam_creation.payment.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentOrderEntityTest {
    @Test
    @DisplayName("trả về true khi đơn thành công nhưng chưa có hạn PLUS")
    void test_isManualReview_001() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setStatus("SUCCEEDED");

        assertThat(order.isManualReview()).isTrue();
    }

    @Test
    @DisplayName("trả về false khi đơn thành công và đã cấp hạn PLUS")
    void test_isManualReview_002() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setStatus("SUCCEEDED");
        order.setPlusExpiresAt(Instant.parse("2026-12-01T00:00:00Z"));

        assertThat(order.isManualReview()).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"PENDING", "FAILED", "CANCELLED", "EXPIRED"})
    @DisplayName("trả về false khi trạng thái đơn chưa xác nhận thanh toán thành công")
    void test_isManualReview_003(String status) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setStatus(status);

        assertThat(order.isManualReview()).isFalse();
    }
}
