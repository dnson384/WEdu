package com.wedu.exam_creation.auth.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.auth.usecase.AuthMigrationUsecase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.mongodb.core.MongoTemplate;

class AuthMigrationRunnerTest {
    private final AuthMigrationUsecase migration = mock(AuthMigrationUsecase.class);
    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withUserConfiguration(AuthMigrationRunner.class)
                    .withBean(AuthMigrationUsecase.class, () -> migration);

    @Test
    @DisplayName("trả về không chạy migration khi thiếu cờ cấu hình")
    void test_run_001() {
        context.run(
                application -> assertThat(application).doesNotHaveBean(AuthMigrationRunner.class));
        verifyNoInteractions(migration);
    }

    @Test
    @DisplayName("trả về không chạy migration khi cờ cấu hình là false")
    void test_run_002() {
        context.withPropertyValues("auth.migration.enabled=false")
                .run(
                        application ->
                                assertThat(application).doesNotHaveBean(AuthMigrationRunner.class));
        verifyNoInteractions(migration);
    }

    @Test
    @DisplayName("trả về migration chạy trước index guard khi cờ cấu hình là true")
    void test_run_003() {
        when(migration.migrate()).thenReturn(2);
        context.withPropertyValues("auth.migration.enabled=true")
                .run(
                        application -> {
                            assertThat(application).hasSingleBean(AuthMigrationRunner.class);
                            AuthMigrationRunner runner =
                                    application.getBean(AuthMigrationRunner.class);
                            AuthIndexConfig index = new AuthIndexConfig(mock(MongoTemplate.class));
                            List<ApplicationRunner> ordered =
                                    new ArrayList<>(List.of(index, runner));
                            AnnotationAwareOrderComparator.sort(ordered);
                            assertThat(ordered).containsExactly(runner, index);
                            runner.run(new DefaultApplicationArguments());
                            verify(migration).migrate();
                        });
    }
}
