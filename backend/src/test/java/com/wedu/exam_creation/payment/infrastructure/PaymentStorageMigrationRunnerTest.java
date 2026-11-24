package com.wedu.exam_creation.payment.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class PaymentStorageMigrationRunnerTest {
    private final PaymentStorageMigrationUsecase migration =
            mock(PaymentStorageMigrationUsecase.class);
    private final DefaultApplicationArguments args = new DefaultApplicationArguments();

    @Test
    @DisplayName("trả về không có ngoại lệ và chạy di trú khi cờ bảo trì được bật")
    void test_run_001() {
        new PaymentStorageMigrationRunner(migration, true).run(args);

        verify(migration).migrate();
        verifyNoMoreInteractions(migration);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và chỉ kiểm tra schema khi cờ bảo trì tắt")
    void test_run_002() {
        new PaymentStorageMigrationRunner(migration, false).run(args);

        verify(migration).assertCurrent();
        verifyNoMoreInteractions(migration);
    }

    @Test
    @DisplayName("trả về lỗi gốc khi schema chưa sẵn sàng và di trú chưa được bật")
    void test_run_003() {
        IllegalStateException failure = new IllegalStateException("schema not current");
        doThrow(failure).when(migration).assertCurrent();

        assertThatThrownBy(() -> new PaymentStorageMigrationRunner(migration, false).run(args))
                .isSameAs(failure);
        verify(migration, never()).migrate();
    }
}
