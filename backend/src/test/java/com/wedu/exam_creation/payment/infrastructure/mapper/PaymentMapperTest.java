package com.wedu.exam_creation.payment.infrastructure.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.document.VnpayTransactionDocument;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PaymentMapperTest {
    @Test
    @DisplayName("trả về document có giao dịch VNPay nhúng khi entity chứa thông tin giao dịch")
    void test_toDocument_001() {
        PaymentMapper mapper = Mappers.getMapper(PaymentMapper.class);
        com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity provider =
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        "123", "00", "00", Instant.parse("2026-11-21T00:00:00Z"));
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setIdempotencyKeyHash("b".repeat(64));
        order.setVnpayTransaction(provider);
        PaymentOrderDocument document = mapper.toDocument(order);
        VnpayTransactionDocument providerDocument = mapper.toDocument(provider);
        assertThat(mapper.toEntity(providerDocument)).isEqualTo(provider);
        assertThat(mapper.toEntity(document)).usingRecursiveComparison().isEqualTo(order);
    }
}
