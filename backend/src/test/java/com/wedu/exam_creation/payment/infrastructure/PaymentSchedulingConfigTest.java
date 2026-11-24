package com.wedu.exam_creation.payment.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.payment.infrastructure.document.PaymentEmailDocument;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.*;

class PaymentSchedulingConfigTest {
    @Test
    @DisplayName("trả về không có ngoại lệ và tạo unique sparse index khi khởi tạo kho thanh toán")
    void test_indexes_001() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        IndexOperations orders = mock(IndexOperations.class), emails = mock(IndexOperations.class);
        when(mongo.indexOps(PaymentOrderDocument.class)).thenReturn(orders);
        when(mongo.indexOps(PaymentEmailDocument.class)).thenReturn(emails);
        new PaymentSchedulingConfig(mongo).indexes();
        ArgumentCaptor<IndexDefinition> index = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(orders, times(2)).ensureIndex(index.capture());
        IndexDefinition requestIndex =
                index.getAllValues().stream()
                        .filter(i -> i.getIndexKeys().containsKey("idempotencyKeyHash"))
                        .findFirst()
                        .orElseThrow();
        assertThat(requestIndex.getIndexOptions())
                .containsEntry("unique", true)
                .containsEntry("sparse", true);
    }
}
