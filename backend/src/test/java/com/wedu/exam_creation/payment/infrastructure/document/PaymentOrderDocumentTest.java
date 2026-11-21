package com.wedu.exam_creation.payment.infrastructure.document;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

class PaymentOrderDocumentTest {
    @Test
    @DisplayName("trả về userId dạng String khi đọc BSON đã lưu dưới dạng ObjectId")
    void test_getUserId_001() throws Exception {
        MongoCustomConversions conversions = MongoCustomConversions.create(config -> {});
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        MappingMongoConverter converter =
                new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        PaymentOrderDocument order = new PaymentOrderDocument();
        order.setId("a".repeat(64));
        order.setUserId("507f1f77bcf86cd799439011");
        order.setCreatedAt(Instant.parse("2026-11-21T00:00:00Z"));
        VnpayTransactionDocument embedded = new VnpayTransactionDocument();
        embedded.setTransactionNo("123");
        order.setVnpayTransaction(embedded);
        Document bson = new Document();
        converter.write(order, bson);
        assertThat(bson.get("vnpayTransaction", Document.class))
                .containsEntry("transactionNo", "123")
                .doesNotContainKeys("id", "_id");
        assertThat(
                        VnpayTransactionDocument.class.isAnnotationPresent(
                                org.springframework.data.mongodb.core.mapping.Document.class))
                .isFalse();
        assertThat(bson.get("userId")).isEqualTo(new ObjectId(order.getUserId()));
        assertThat(converter.read(PaymentOrderDocument.class, bson).getUserId())
                .isEqualTo(order.getUserId());
        assertThat(bson)
                .doesNotContainKeys(
                        "clientIp",
                        "manualReview",
                        "refundTracked",
                        "transactionNo",
                        "lastVerifiedAt");
        Query query = Query.query(Criteria.where("userId").is(order.getUserId()));
        assertThat(
                        new QueryMapper(converter)
                                .getMappedObject(
                                        query.getQueryObject(),
                                        context.getPersistentEntity(PaymentOrderDocument.class)))
                .containsEntry("userId", new ObjectId(order.getUserId()));
    }
}
