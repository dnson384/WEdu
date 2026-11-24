package com.wedu.exam_creation.payment.infrastructure;

import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentEmailDocument;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "payment.vnpay.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentSchedulingConfig {
    private final MongoTemplate mongo;

    @EventListener(ApplicationReadyEvent.class)
    public void indexes() {
        mongo.indexOps(PaymentOrderDocument.class).ensureIndex(new Index()
                .on("idempotencyKeyHash", Sort.Direction.ASC).unique().sparse());
        mongo.indexOps(PaymentOrderDocument.class).ensureIndex(new Index().on("status", Sort.Direction.ASC).on("expiresAt", Sort.Direction.ASC));
        mongo.indexOps(PaymentEmailDocument.class).ensureIndex(new Index().on("sentAt", Sort.Direction.ASC).on("nextAttemptAt", Sort.Direction.ASC));
    }
}
