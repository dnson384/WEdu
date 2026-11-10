package com.wedu.exam_creation.payment.infrastructure;

import com.wedu.exam_creation.payment.infrastructure.document.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort.Direction;

@Configuration
@RequiredArgsConstructor
public class RefundIndexConfig {
    private final MongoTemplate mongo;
    @EventListener(ApplicationReadyEvent.class)
    public void indexes() {
        var indexes = mongo.indexOps(RefundDocument.class);
        indexes.ensureIndex(new Index().on("userId", Direction.ASC).on("idempotencyKey", Direction.ASC).unique());
        indexes.ensureIndex(new Index().on("orderId", Direction.ASC).on("status", Direction.ASC));
        indexes.ensureIndex(new Index().on("createdAt", Direction.DESC).on("_id", Direction.DESC));
        indexes.ensureIndex(new Index().on("userId", Direction.ASC).on("createdAt", Direction.DESC));
        indexes.ensureIndex(new Index().on("status", Direction.ASC).on("lastCheckedAt", Direction.ASC));
        mongo.indexOps(RefundAiReceiptDocument.class).ensureIndex(new Index().on("userId", Direction.ASC).on("createdAt", Direction.DESC));
        mongo.indexOps(RefundAccountDocument.class).ensureIndex(new Index().on("trackedSince", Direction.ASC));
        mongo.indexOps(PaymentOrderDocument.class).ensureIndex(new Index().on("userId", Direction.ASC).on("paidAt", Direction.ASC));
    }
}
