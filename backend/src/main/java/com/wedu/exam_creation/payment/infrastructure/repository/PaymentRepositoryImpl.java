package com.wedu.exam_creation.payment.infrastructure.repository;

import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentEmailDocument;
import com.wedu.exam_creation.payment.infrastructure.document.PaymentOrderDocument;
import com.wedu.exam_creation.payment.infrastructure.mapper.PaymentMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements IPaymentRepository {
    private final MongoTemplate mongoTemplate;
    private final PaymentMapper paymentMapper;

    private Query transactionQuery(String keyword, String status, Instant from, Instant to) {
        Query query = new Query();
        if (!keyword.isEmpty()) {
            query.addCriteria(
                    new Criteria()
                            .orOperator(
                                    Criteria.where("_id").is(keyword),
                                    Criteria.where("vnpayTransaction.transactionNo").is(keyword),
                                    Criteria.where("email")
                                            .regex(java.util.regex.Pattern.quote(keyword), "i")));
        }
        if (!status.isEmpty()) query.addCriteria(Criteria.where("status").is(status));
        if (from != null || to != null) {
            Criteria dates = Criteria.where("createdAt");
            if (from != null) dates.gte(from);
            if (to != null) dates.lt(to);
            query.addCriteria(dates);
        }
        return query;
    }

    public List<PaymentOrderEntity> searchTransactions(
            String keyword, String status, Instant from, Instant to, int page) {
        Query query =
                transactionQuery(keyword, status, from, to)
                        .with(
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC,
                                        "createdAt",
                                        "_id"))
                        .skip((long) page * 10)
                        .limit(10);
        return assemble(mongoTemplate.find(query, PaymentOrderDocument.class));
    }

    public long countTransactions(String keyword, String status, Instant from, Instant to) {
        return mongoTemplate.count(
                transactionQuery(keyword, status, from, to), PaymentOrderDocument.class);
    }

    public PaymentOrderEntity find(String id) {
        PaymentOrderDocument paymentOrderDocument =
                mongoTemplate.findById(id, PaymentOrderDocument.class);
        return assemble(paymentOrderDocument);
    }

    public PaymentOrderEntity insert(PaymentOrderEntity order) {
        requireProvider(order);
        PaymentOrderDocument paymentOrderDocument =
                mongoTemplate.insert(paymentMapper.toDocument(order));
        return assemble(paymentOrderDocument);
    }

    public PaymentOrderEntity findByIdempotencyKeyHash(String hash) {
        return assemble(
                mongoTemplate.findOne(
                        Query.query(Criteria.where("idempotencyKeyHash").is(hash)),
                        PaymentOrderDocument.class));
    }

    public boolean update(PaymentOrderEntity order, long expectedRevision) {
        requireProvider(order);
        Update update =
                new Update()
                        .set("status", order.getStatus())
                        .set("paidAt", order.getPaidAt())
                        .set("plusExpiresAt", order.getPlusExpiresAt())
                        .set(
                                "vnpayTransaction.transactionNo",
                                order.getVnpayTransaction().getTransactionNo())
                        .set(
                                "vnpayTransaction.responseCode",
                                order.getVnpayTransaction().getResponseCode())
                        .set(
                                "vnpayTransaction.transactionStatus",
                                order.getVnpayTransaction().getTransactionStatus())
                        .inc("revision", 1);
        return mongoTemplate
                        .updateFirst(
                                Query.query(
                                        Criteria.where("_id")
                                                .is(order.getId())
                                                .and("revision")
                                                .is(expectedRevision)),
                                update,
                                PaymentOrderDocument.class)
                        .getModifiedCount()
                == 1;
    }

    public boolean claimVerification(String id, String userId, Instant now) {
        Criteria criteria =
                Criteria.where("_id")
                        .is(id)
                        .and("userId")
                        .is(userId)
                        .and("vnpayTransaction")
                        .ne(null)
                        .orOperator(
                                Criteria.where("vnpayTransaction.lastVerifiedAt").is(null),
                                Criteria.where("vnpayTransaction.lastVerifiedAt")
                                        .lte(now.minusSeconds(30)));
        return mongoTemplate
                        .updateFirst(
                                Query.query(criteria),
                                new Update().set("vnpayTransaction.lastVerifiedAt", now),
                                PaymentOrderDocument.class)
                        .getModifiedCount()
                == 1;
    }

    public List<PaymentOrderEntity> expiredPending(Instant now) {
        return assemble(
                mongoTemplate.find(
                        Query.query(
                                        Criteria.where("status")
                                                .is("PENDING")
                                                .and("expiresAt")
                                                .lte(now))
                                .limit(100),
                        PaymentOrderDocument.class));
    }

    private PaymentOrderEntity assemble(PaymentOrderDocument order) {
        if (order == null) return null;
        if (order.getVnpayTransaction() == null)
            throw new IllegalStateException(
                    "Missing embedded VNPay data; payment storage migration required");
        return paymentMapper.toEntity(order);
    }

    private List<PaymentOrderEntity> assemble(List<PaymentOrderDocument> orders) {
        return orders.stream().map(this::assemble).toList();
    }

    private void requireProvider(PaymentOrderEntity order) {
        if (order.getVnpayTransaction() == null)
            throw new IllegalArgumentException("Missing embedded VNPay data");
    }

    public void enqueueEmail(PaymentEmailEntity email) {
        // Deterministic _id and the order revision ensure one outbox entry per result.
        mongoTemplate.insert(paymentMapper.toDocument(email));
    }

    public PaymentEmailEntity claimEmail(Instant now, String leaseToken) {
        return paymentMapper.toEntity(
                mongoTemplate.findAndModify(
                        Query.query(
                                Criteria.where("refundId")
                                        .is(null)
                                        .and("sentAt")
                                        .is(null)
                                        .and("attempts")
                                        .lt(5)
                                        .and("nextAttemptAt")
                                        .lte(now)),
                        new Update()
                                .inc("attempts", 1)
                                .set("nextAttemptAt", now.plusSeconds(300))
                                .set("leaseToken", leaseToken),
                        FindAndModifyOptions.options().returnNew(true),
                        PaymentEmailDocument.class));
    }

    public void completeEmail(String id, String leaseToken, Instant now, boolean sent) {
        Update update = new Update().unset("leaseToken");
        if (sent) update.set("sentAt", now);
        else update.set("nextAttemptAt", now.plusSeconds(60));
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id).and("leaseToken").is(leaseToken)),
                update,
                PaymentEmailDocument.class);
    }
}
