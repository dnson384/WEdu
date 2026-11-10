package com.wedu.exam_creation.payment.domain.repository;

import com.wedu.exam_creation.payment.domain.entity.*;
import java.time.Instant;
import java.util.List;

public interface IRefundRepository {
    RefundEntity find(String id);
    RefundEntity findAttempt(String userId, String key);
    RefundEntity activeForOrder(String orderId);
    void insert(RefundEntity refund);
    boolean update(RefundEntity refund, long revision);
    List<RefundEntity> search(String userId, String keyword, String status, Instant from, Instant to, int page);
    long count(String userId, String keyword, String status, Instant from, Instant to);
    List<RefundEntity> due(Instant now);
    boolean claimPoll(String id, Instant now);
    RefundAccountEntity account(String userId);
    boolean saveAccount(RefundAccountEntity account, long revision);
    void recordAi(String attempt, String userId, Instant at);
    Instant lastAiUse(String userId);
}
