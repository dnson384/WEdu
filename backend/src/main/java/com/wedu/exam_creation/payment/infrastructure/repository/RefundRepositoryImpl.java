package com.wedu.exam_creation.payment.infrastructure.repository;

import com.wedu.exam_creation.payment.domain.entity.*;
import com.wedu.exam_creation.payment.domain.repository.IRefundRepository;
import com.wedu.exam_creation.payment.infrastructure.document.*;
import com.wedu.exam_creation.payment.infrastructure.mapper.RefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Repository
@RequiredArgsConstructor
public class RefundRepositoryImpl implements IRefundRepository {
    private final MongoTemplate mongo;
    private final RefundMapper mapper;
    private static final List<String> ACTIVE = List.of("PENDING_REVIEW", "NEEDS_VERIFICATION", "PROCESSING", "REFUNDED");

    public void recordAi(String attempt, String userId, Instant at) {
        RefundAiReceiptDocument receipt = new RefundAiReceiptDocument();
        receipt.setId(attempt); receipt.setUserId(userId); receipt.setCreatedAt(at);
        mongo.insert(receipt);
    }
    public Instant lastAiUse(String userId) {
        RefundAiReceiptDocument receipt = mongo.findOne(Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(1), RefundAiReceiptDocument.class);
        return receipt == null ? null : receipt.getCreatedAt();
    }

    public RefundEntity find(String id) { return mapper.toEntity(mongo.findById(id, RefundDocument.class)); }
    public RefundEntity findAttempt(String userId, String key) {
        return mapper.toEntity(mongo.findOne(Query.query(Criteria.where("userId").is(userId)
                .and("idempotencyKey").is(key)), RefundDocument.class));
    }
    public RefundEntity activeForOrder(String orderId) {
        return mapper.toEntity(mongo.findOne(Query.query(Criteria.where("orderId").is(orderId)
                .and("status").in(ACTIVE)), RefundDocument.class));
    }
    public void insert(RefundEntity refund) { mongo.insert(mapper.toDocument(refund)); }
    public boolean update(RefundEntity refund, long revision) {
        refund.setRevision(revision + 1);
        RefundDocument replacement = mapper.toDocument(refund);
        replacement.setId(null); // Mongo carries the matched document identity into the replacement.
        return mongo.findAndReplace(Query.query(Criteria.where("_id").is(refund.getId())
                .and("revision").is(revision)), replacement) != null;
    }
    private Query query(String userId, String keyword, String status, Instant from, Instant to) {
        Query query = new Query();
        if (userId != null) query.addCriteria(Criteria.where("userId").is(userId));
        if (!keyword.isEmpty()) query.addCriteria(new Criteria().orOperator(Criteria.where("_id").is(keyword),
                Criteria.where("orderId").is(keyword), Criteria.where("email").regex(Pattern.quote(keyword), "i")));
        if (!status.isEmpty()) query.addCriteria(Criteria.where("status").is(status));
        if (from != null || to != null) {
            Criteria dates = Criteria.where("createdAt");
            if (from != null) dates.gte(from);
            if (to != null) dates.lt(to);
            query.addCriteria(dates);
        }
        return query;
    }
    public List<RefundEntity> search(String userId, String keyword, String status, Instant from, Instant to, int page) {
        return mongo.find(query(userId, keyword, status, from, to).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"))
                .skip((long) page * 10).limit(10), RefundDocument.class).stream().map(mapper::toEntity).toList();
    }
    public long count(String userId, String keyword, String status, Instant from, Instant to) {
        return mongo.count(query(userId, keyword, status, from, to), RefundDocument.class);
    }
    public List<RefundEntity> due(Instant now) {
        return mongo.find(Query.query(Criteria.where("status").is("PROCESSING").and("pollingStopped").ne(true)
                .orOperator(Criteria.where("lastCheckedAt").is(null), Criteria.where("lastCheckedAt").lte(now.minusSeconds(300))))
                .limit(100), RefundDocument.class).stream().map(mapper::toEntity).toList();
    }
    public boolean claimPoll(String id, Instant now) {
        return mongo.updateFirst(Query.query(Criteria.where("_id").is(id).and("status").is("PROCESSING")
                .orOperator(Criteria.where("lastCheckedAt").is(null), Criteria.where("lastCheckedAt").lte(now.minusSeconds(300)))),
                new Update().set("lastCheckedAt", now).inc("revision", 1), RefundDocument.class).getModifiedCount() == 1;
    }
    public RefundAccountEntity account(String userId) {
        return mapper.toEntity(mongo.findById(userId, RefundAccountDocument.class));
    }
    public boolean saveAccount(RefundAccountEntity account, long revision) {
        if (revision < 0) { mongo.insert(mapper.toDocument(account)); return true; }
        account.setRevision(revision + 1);
        RefundAccountDocument replacement = mapper.toDocument(account);
        replacement.setId(null);
        return mongo.findAndReplace(Query.query(Criteria.where("_id").is(account.getId()).and("revision").is(revision)),
                replacement) != null;
    }
}
