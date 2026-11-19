package com.wedu.exam_creation.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.document.VnpayTransactionDocument;
import com.wedu.exam_creation.payment.infrastructure.mapper.PaymentMapper;
import com.wedu.exam_creation.payment.infrastructure.repository.PaymentRepositoryImpl;
import java.time.Instant;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class PaymentStorageMappingTest {
    @Test
    void mapsProviderEntitySeparatelyAndReassemblesTheOrder() {
        PaymentMapper mapper = Mappers.getMapper(PaymentMapper.class);
        com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity provider =
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        "123", "00", "00", Instant.now());
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setVnpayTransaction(provider);
        PaymentOrderDocument document = mapper.toDocument(order);
        VnpayTransactionDocument providerDocument = mapper.toDocument(provider);
        assertThat(mapper.toEntity(providerDocument)).isEqualTo(provider);
        assertThat(mapper.toEntity(document).getVnpayTransaction()).isEqualTo(provider);
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.findById(order.getId(), PaymentOrderDocument.class)).thenReturn(document);
        PaymentOrderEntity assembled = new PaymentRepositoryImpl(mongo, mapper).find(order.getId());
        assertThat(assembled.getVnpayTransaction()).isEqualTo(provider);
    }

    @Test
    void missingEmbeddedProviderIsRejectedBeforeAnyWrite() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("order");
        assertThatThrownBy(() -> repository.insert(order))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.update(order, 0))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mongo);
    }

    @Test
    void persistsObjectIdButReturnsStringAndMapsOwnerQueries() throws Exception {
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
        order.setCreatedAt(Instant.now());
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

    @Test
    void throttleAtomicallyChecksOwnerAndUpdatesOnlyNestedTimestamp() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(PaymentOrderDocument.class)))
                .thenReturn(
                        UpdateResult.acknowledged(0, 0L, null),
                        UpdateResult.acknowledged(1, 1L, null));
        Instant now = Instant.now();
        assertThat(repository.claimVerification("order", "other", now)).isFalse();
        assertThat(repository.claimVerification("order", "owner", now)).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2))
                .updateFirst(query.capture(), update.capture(), eq(PaymentOrderDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("userId", "owner");
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsOnlyKeys("vnpayTransaction.lastVerifiedAt")
                .containsEntry("vnpayTransaction.lastVerifiedAt", now);
    }

    @Test
    void resultUpdateUsesRevisionAndPreservesConcurrentThrottle() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        PaymentRepositoryImpl repository =
                new PaymentRepositoryImpl(mongo, Mappers.getMapper(PaymentMapper.class));
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("order");
        order.setVnpayTransaction(
                new com.wedu.exam_creation.payment.domain.entity.VnpayTransactionEntity(
                        "123", "00", "00", Instant.now()));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(PaymentOrderDocument.class)))
                .thenReturn(
                        UpdateResult.acknowledged(0, 0L, null),
                        UpdateResult.acknowledged(1, 1L, null));
        assertThat(repository.update(order, 1)).isFalse();
        assertThat(repository.update(order, 1)).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2))
                .updateFirst(query.capture(), update.capture(), eq(PaymentOrderDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("revision", 1L);
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsEntry("vnpayTransaction.transactionNo", "123")
                .doesNotContainKeys("vnpayTransaction", "vnpayTransaction.lastVerifiedAt");
    }
}
