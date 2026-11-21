package com.wedu.exam_creation.payment.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

class AdminTransactionIndexConfigTest {
    @Test
    @DisplayName(
            "trả về không có ngoại lệ và tạo index tra cứu khi khởi tạo độc lập với cấu hình thanh toán")
    void test_indexes_001() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        IndexOperations operations = mock(IndexOperations.class);
        when(mongo.indexOps(PaymentOrderDocument.class)).thenReturn(operations);
        new AdminTransactionIndexConfig(mongo).indexes();
        ArgumentCaptor<IndexDefinition> indexes = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(operations, times(3)).ensureIndex(indexes.capture());
        assertThat(indexes.getAllValues().stream().map(IndexDefinition::getIndexKeys).toList())
                .contains(
                        new Document("createdAt", -1).append("_id", -1),
                        new Document("status", 1).append("createdAt", -1).append("_id", -1));
        assertThat(indexes.getValue().getIndexKeys())
                .isEqualTo(new Document("vnpayTransaction.transactionNo", 1));
    }
}
