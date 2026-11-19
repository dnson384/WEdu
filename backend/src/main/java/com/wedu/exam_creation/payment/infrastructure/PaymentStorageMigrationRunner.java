package com.wedu.exam_creation.payment.infrastructure;

import com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class PaymentStorageMigrationRunner implements ApplicationRunner {
    private final PaymentStorageMigrationUsecase migration;
    private final boolean enabled;

    public PaymentStorageMigrationRunner(
            PaymentStorageMigrationUsecase migration,
            @Value("${payment.storage-migration.enabled:false}") boolean enabled) {
        this.migration = migration;
        this.enabled = enabled;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (enabled)
            log.info(
                    "Payment storage migration completed; processed {} orders",
                    migration.migrate());
        else migration.assertCurrent();
    }
}
