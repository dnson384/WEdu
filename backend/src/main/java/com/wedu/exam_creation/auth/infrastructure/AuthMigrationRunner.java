package com.wedu.exam_creation.auth.infrastructure;

import com.wedu.exam_creation.auth.usecase.AuthMigrationUsecase;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "auth.migration.enabled", havingValue = "true")
public class AuthMigrationRunner implements ApplicationRunner {
    private final AuthMigrationUsecase migration;

    public AuthMigrationRunner(AuthMigrationUsecase migration) {
        this.migration = migration;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        int processed = migration.migrate();
        log.info("Auth Java migration completed; processed {} users", processed);
    }
}
