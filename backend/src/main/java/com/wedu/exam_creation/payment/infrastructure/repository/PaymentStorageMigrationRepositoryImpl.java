package com.wedu.exam_creation.payment.infrastructure.repository;

import com.wedu.exam_creation.payment.domain.repository.IPaymentStorageMigrationRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentStorageMigrationRepositoryImpl implements IPaymentStorageMigrationRepository {
    private static final String ORDERS = "PaymentOrders";
    private static final String PROVIDERS = "VnpayTransactions";
    private static final List<String> PROVIDER_FIELDS =
            List.of("transactionNo", "responseCode", "transactionStatus", "lastVerifiedAt");
    private static final List<String> REMOVED_FIELDS =
            List.of("clientIp", "manualReview", "refundTracked");
    private final MongoTemplate mongo;

    public List<String> preflight() {
        List<Document> orders = mongo.find(new Query(), Document.class, ORDERS);
        List<String> ids = new ArrayList<>();
        for (Document order : orders) {
            validate(order);
            Document provider = mongo.findById(order.get("_id"), Document.class, PROVIDERS);
            mergedProvider(order, provider);
            if (legacy(order) || provider != null) ids.add(order.getString("_id"));
        }
        return ids;
    }

    public void prepare() {}

    public void migrateOrder(String id) {
        Document order = mongo.findById(id, Document.class, ORDERS);
        if (order == null) throw invalid("Order disappeared during migration");
        validate(order);
        Document provider = mongo.findById(id, Document.class, PROVIDERS);
        Document embedded = mergedProvider(order, provider);
        Update update = new Update().set("userId", userId(order)).set("vnpayTransaction", embedded);
        PROVIDER_FIELDS.forEach(update::unset);
        REMOVED_FIELDS.forEach(update::unset);
        if (mongo.updateFirst(new BasicQuery(order), update, ORDERS).getMatchedCount() != 1) {
            throw new OptimisticLockingFailureException(
                    "Payment changed during migration; stop writers and retry");
        }
        Document stored = mongo.findById(id, Document.class, ORDERS);
        if (stored == null || !embedded.equals(stored.get("vnpayTransaction")))
            throw invalid("Embedded VNPay copy verification failed");
        if (provider != null
                && mongo.remove(new BasicQuery(provider), PROVIDERS).getDeletedCount() != 1)
            throw new OptimisticLockingFailureException("VNPay source changed during migration");
    }

    public void finish() {
        if (!mongo.collectionExists(ORDERS)) return;
        List<Document> indexes = mongo.getCollection(ORDERS).listIndexes().into(new ArrayList<>());
        for (Document index : indexes) {
            if (new Document("transactionNo", 1).equals(index.get("key")))
                mongo.indexOps(ORDERS).dropIndex(index.getString("name"));
        }
    }

    public void assertCurrent() {
        try (java.util.stream.Stream<Document> orders =
                mongo.stream(new Query(), Document.class, ORDERS)) {
            orders.forEach(
                    order -> {
                        if (!(order.get("userId") instanceof ObjectId) || legacy(order)) {
                            throw invalid(
                                    "Run once with --payment.storage-migration.enabled=true while all payment writers are stopped");
                        }
                    });
        }
    }

    private boolean legacy(Document order) {
        return !(order.get("vnpayTransaction") instanceof Document)
                || !(order.get("userId") instanceof ObjectId)
                || PROVIDER_FIELDS.stream().anyMatch(order::containsKey)
                || REMOVED_FIELDS.stream().anyMatch(order::containsKey);
    }

    private void validate(Document order) {
        if (!(order.get("_id") instanceof String)) throw invalid("Order ID must be a string");
        userId(order);
        for (String field : List.of("paidAt", "lastVerifiedAt")) {
            if (order.get(field) != null && !(order.get(field) instanceof Date))
                throw invalid("Invalid payment timestamp");
        }
        if (order.get("manualReview") != null
                && !Objects.equals(
                        order.get("manualReview"),
                        "SUCCEEDED".equals(order.getString("status"))
                                && order.get("plusExpiresAt") == null)) {
            throw invalid("manualReview conflicts with entitlement; resolve before migration");
        }
    }

    private ObjectId userId(Document order) {
        Object value = order.get("userId");
        if (value instanceof ObjectId id) return id;
        if (value instanceof String id && ObjectId.isValid(id)) return new ObjectId(id);
        throw invalid("Payment userId is not a valid ObjectId");
    }

    private Document mergedProvider(Document order, Document provider) {
        Object existing = order.get("vnpayTransaction");
        if (existing != null && !(existing instanceof Document))
            throw invalid("Invalid embedded VNPay data");
        Document merged = new Document();
        for (Document source :
                new Document[] {existing instanceof Document d ? d : null, order, provider}) {
            if (source == null) continue;
            for (String field : PROVIDER_FIELDS) {
                Object value = source.get(field);
                if (value == null) continue;
                if (field.equals("lastVerifiedAt")
                        ? !(value instanceof Date)
                        : !(value instanceof String)) throw invalid("Invalid VNPay field type");
                if (merged.containsKey(field) && !Objects.equals(merged.get(field), value))
                    throw invalid("VNPay data conflicts across storage layouts");
                merged.put(field, value);
            }
        }
        return merged;
    }

    private IllegalStateException invalid(String reason) {
        return new IllegalStateException("Payment storage migration: " + reason);
    }
}
