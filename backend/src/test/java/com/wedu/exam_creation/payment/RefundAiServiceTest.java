package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.domain.entity.RefundAccountEntity;
import com.wedu.exam_creation.payment.domain.repository.IRefundRepository;
import com.wedu.exam_creation.payment.usecase.*;
import com.wedu.exam_creation.common.exception.ConflictException;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundAiServiceTest {
    IRefundRepository repository = mock(IRefundRepository.class);
    RefundTransactions transactions = mock(RefundTransactions.class);
    com.wedu.exam_creation.user.usecase.UserService users = mock(com.wedu.exam_creation.user.usecase.UserService.class);
    RefundAiService service = new RefundAiService(repository, transactions, users);
    RefundAccountEntity account = new RefundAccountEntity();
    @BeforeEach void setup() {
        account.setId("user"); when(repository.account("user")).thenReturn(account);
        var user = new com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO();
        user.setAccountType("PLUS"); user.setIsActive(true); when(users.findById("user")).thenReturn(user);
        when(repository.saveAccount(any(), anyLong())).thenReturn(true);
        when(transactions.run(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        TransactionSynchronizationManager.initSynchronization();
    }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }
    @Test void holdPreventsAiBeforeProviderCall() {
        account.getHolds().add("refund");
        assertThatThrownBy(() -> service.begin("user")).isInstanceOf(ConflictException.class);
        assertThat(account.getActiveAi()).isEmpty(); verify(repository, never()).recordAi(any(), any(), any());
    }
    @Test void staleAuthenticatedPlusCannotStartAiAfterRefundRevokedIt() {
        var user = new com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO();
        user.setAccountType("FREE"); user.setIsActive(true); when(users.findById("user")).thenReturn(user);
        assertThatThrownBy(() -> service.begin("user")).isInstanceOf(ConflictException.class);
        assertThat(account.getActiveAi()).isEmpty();
    }
    @Test void onlyPersistedAiResultCountsAndCommitClearsAttempt() {
        Runnable persisted = service.begin("user"); assertThat(account.getActiveAi()).hasSize(1);
        persisted.run(); verify(repository).recordAi(anyString(), eq("user"), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertThat(account.getActiveAi()).isEmpty(); assertThat(account.getLastAiUsedAt()).isNotNull();
    }
    @Test void rollbackClearsAttemptAndDoesNotCountAsUsage() {
        service.begin("user").run();
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(account.getActiveAi()).isEmpty(); assertThat(account.getLastAiUsedAt()).isNull();
    }
    @Test void examWithoutPersistedAiDoesNotConsumeEligibility() {
        service.begin("user");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertThat(account.getLastAiUsedAt()).isNull(); verify(repository, never()).recordAi(any(), any(), any());
    }
    @Test void unknownCommitRetainsAttemptForVerification() {
        service.begin("user").run();
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN));
        assertThat(account.getActiveAi()).hasSize(1); assertThat(account.getLastAiUsedAt()).isNull();
    }
}
