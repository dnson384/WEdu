package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.payment.domain.entity.RefundAccountEntity;
import com.wedu.exam_creation.payment.domain.repository.IRefundRepository;
import com.wedu.exam_creation.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundAiService {
    private final IRefundRepository repository;
    private final RefundTransactions transactions;
    private final com.wedu.exam_creation.user.usecase.UserService users;

    public RefundAccountEntity lockedAccount(String userId) {
        RefundAccountEntity account = repository.account(userId);
        if (account == null) {
            account = new RefundAccountEntity();
            account.setId(userId);
            account.setTrackedSince(Instant.now());
            save(account, -1);
        } else save(account, account.getRevision());
        return account;
    }

    public void save(RefundAccountEntity account, long revision) {
        if (!repository.saveAccount(account, revision)) throw new OptimisticLockingFailureException("Refund account changed");
    }

    /** Call inside the exam transaction, immediately before an actual AI request. */
    public Runnable begin(String userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("AI refund evidence requires an exam transaction");
        String attempt = UUID.randomUUID().toString();
        transactions.run(() -> {
            RefundAccountEntity account = lockedAccount(userId);
            if (!account.getHolds().isEmpty()) throw new ConflictException("AI đang tạm khóa trong lúc xử lý yêu cầu hoàn tiền.");
            var user = users.findById(userId);
            if (user == null || !"PLUS".equals(user.getAccountType()) || !Boolean.TRUE.equals(user.getIsActive()))
                throw new ConflictException("Quyền PLUS không còn hiệu lực. Vui lòng tải lại tài khoản.");
            account.getActiveAi().add(attempt);
            save(account, account.getRevision());
            return null;
        });
        boolean[] used = {false};
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                // Unknown transaction outcomes deliberately retain the attempt for verification.
                if (status == STATUS_UNKNOWN) return;
                transactions.run(() -> {
                    RefundAccountEntity account = lockedAccount(userId);
                    account.getActiveAi().remove(attempt);
                    if (status == STATUS_COMMITTED && used[0]) account.setLastAiUsedAt(Instant.now());
                    save(account, account.getRevision());
                    return null;
                });
            }
        });
        return () -> {
            repository.recordAi(attempt, userId, Instant.now());
            used[0] = true;
        };
    }
}
