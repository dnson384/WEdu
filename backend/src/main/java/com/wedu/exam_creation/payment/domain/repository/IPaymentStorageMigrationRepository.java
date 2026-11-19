package com.wedu.exam_creation.payment.domain.repository;

import java.util.List;

public interface IPaymentStorageMigrationRepository {
    List<String> preflight();

    void prepare();

    void migrateOrder(String id);

    void finish();

    void assertCurrent();
}
