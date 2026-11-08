package com.wedu.exam_creation.payment.infrastructure;

import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;

@Configuration
@RequiredArgsConstructor
public class AdminTransactionIndexConfig {
    private final MongoTemplate mongo;

    @EventListener(ApplicationReadyEvent.class)
    public void indexes() {
        var indexes = mongo.indexOps(PaymentOrderDocument.class);
        indexes.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC));
        indexes.ensureIndex(new Index().on("status", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC));
        indexes.ensureIndex(new Index().on("transactionNo", Sort.Direction.ASC));
    }
}
