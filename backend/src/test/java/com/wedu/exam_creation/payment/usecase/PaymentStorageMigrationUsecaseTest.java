package com.wedu.exam_creation.payment.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.payment.domain.repository.IPaymentStorageMigrationRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PaymentStorageMigrationUsecaseTest {
    @Test
    @DisplayName("trả về lỗi gốc và rollback khi sao chép một đơn thất bại")
    void test_migrate_001() {
        String id = "a".repeat(64);
        IPaymentStorageMigrationRepository storage = mock(IPaymentStorageMigrationRepository.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        when(storage.preflight()).thenReturn(List.of(id));
        doThrow(new IllegalStateException("copy failed")).when(storage).migrateOrder(id);
        assertThatThrownBy(() -> new PaymentStorageMigrationUsecase(storage, manager).migrate())
                .hasMessage("copy failed");
        org.mockito.InOrder sequence = inOrder(storage, manager);
        sequence.verify(storage).preflight();
        sequence.verify(storage).prepare();
        sequence.verify(manager).getTransaction(any());
        sequence.verify(storage).migrateOrder(id);
        sequence.verify(manager).rollback(status);
        verify(storage, never()).finish();
        verify(manager, never()).commit(any());
    }

    @Test
    @DisplayName(
            "trả về số đơn đã di trú khi ghi thành công rồi kiểm tra schema trước khi dọn index")
    void test_migrate_002() {
        IPaymentStorageMigrationRepository storage = mock(IPaymentStorageMigrationRepository.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        SimpleTransactionStatus status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        when(storage.preflight()).thenReturn(List.of("order"));

        assertThat(new PaymentStorageMigrationUsecase(storage, manager).migrate()).isEqualTo(1);

        org.mockito.InOrder sequence = inOrder(storage, manager);
        sequence.verify(storage).preflight();
        sequence.verify(storage).prepare();
        sequence.verify(storage).migrateOrder("order");
        sequence.verify(manager).commit(status);
        sequence.verify(storage).assertCurrent();
        sequence.verify(storage).finish();
    }
}
