package com.wedu.exam_creation.payment.usecase;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.TransientDataAccessException;
import java.util.function.Supplier;

@Component
public class RefundTransactions {
    private final TransactionTemplate transaction;
    public RefundTransactions(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public <T> T run(Supplier<T> operation) {
        for (int attempt = 0; ; attempt++) {
            try { return transaction.execute(ignored -> operation.get()); }
            catch (RuntimeException e) { if (attempt >= 2 || !retryable(e)) throw e; }
        }
    }
    private boolean retryable(Throwable error) {
        for (Throwable e = error; e != null; e = e.getCause()) {
            if (e instanceof com.mongodb.MongoException mongo && mongo.hasErrorLabel("UnknownTransactionCommitResult")) return false;
        }
        for (Throwable e = error; e != null; e = e.getCause()) {
            if (e instanceof TransientDataAccessException || e instanceof org.springframework.dao.DuplicateKeyException) return true;
            if (e instanceof com.mongodb.MongoException mongo && mongo.hasErrorLabel("TransientTransactionError")) return true;
        }
        // Unknown commits are not replayed: their send intent must be reconciled.
        return false;
    }
}
