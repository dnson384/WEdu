package com.wedu.exam_creation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

class MongoConfigTest {
    @Test
    @DisplayName("trả về transaction manager gắn với factory đã cấp khi khởi tạo bean MongoDB")
    void test_transactionManager_001() {
        MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);

        MongoTransactionManager manager = new MongoConfig().transactionManager(factory);

        assertThat(manager.getDatabaseFactory()).isSameAs(factory);
        verifyNoInteractions(factory);
    }
}
