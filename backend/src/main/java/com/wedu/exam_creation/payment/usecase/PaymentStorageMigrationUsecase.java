package com.wedu.exam_creation.payment.usecase;

import com.wedu.exam_creation.payment.domain.repository.IPaymentStorageMigrationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentStorageMigrationUsecase {
    private final IPaymentStorageMigrationRepository repository;
    private final TransactionTemplate transaction;

    public PaymentStorageMigrationUsecase(
            IPaymentStorageMigrationRepository repository, PlatformTransactionManager manager) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(manager);
    }

    public int migrate() {
        List<String> ids = repository.preflight();
        repository.prepare();
        for (String id : ids)
            transaction.executeWithoutResult(ignored -> repository.migrateOrder(id));
        repository.assertCurrent();
        repository.finish();
        return ids.size();
    }

    public void assertCurrent() {
        repository.assertCurrent();
    }
}
