package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ServiceUnavailableException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import com.wedu.exam_creation.payment.infrastructure.VnpayQueryException;
import com.wedu.exam_creation.payment.usecase.PaymentUsecase;
import com.wedu.exam_creation.payment.dto.PaymentDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentUsecaseTest {
    final String id = "a".repeat(64);
    IPaymentRepository repository;
    UserService users;
    VnpayGateway gateway;
    VnpayProperties properties;
    PaymentUsecase payments;
    PlatformTransactionManager manager;
    PaymentOrderEntity order;
    CommonUserResponseAllDTO teacher;

    @BeforeEach
    void setup() {
        repository = mock(IPaymentRepository.class);
        users = mock(UserService.class);
        manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        properties = new VnpayProperties();
        properties.setEnabled(true);
        properties.setTmnCode("TESTCODE");
        properties.setHashSecret("test-secret-only");
        properties.setReturnUrl("https://example.com/me/bill");
        gateway = spy(new VnpayGateway(properties));
        payments = new PaymentUsecase(repository, users, gateway, properties, manager);
        order = new PaymentOrderEntity();
        order.setId(id);
        order.setUserId("teacher");
        order.setEmail("teacher@example.com");
        order.setAmount(20000);
        order.setDurationDays(30);
        order.setStatus("PENDING");
        order.setCreatedAt(Instant.now());
        order.setExpiresAt(Instant.now().plusSeconds(900));
        order.setClientIp("127.0.0.1");
        when(repository.find(id)).thenReturn(order);
        when(repository.update(any(), anyLong())).thenReturn(true);
        when(users.grantPaidPlus(eq("teacher"), any(), eq(30))).thenReturn(Instant.now().plusSeconds(2592000));
        teacher = new CommonUserResponseAllDTO();
        teacher.setId("teacher");
        teacher.setRole("ROLE_TEACHER");
        teacher.setEmail("teacher@example.com");
        teacher.setIsActive(true);
    }

    Map<String, String> result(String response, String status) {
        Map<String, String> values = new HashMap<>(Map.of("vnp_TmnCode", "TESTCODE", "vnp_TxnRef", id,
                "vnp_Amount", "2000000", "vnp_ResponseCode", response,
                "vnp_TransactionStatus", status, "vnp_TransactionNo", "123456"));
        return signed(values);
    }

    Map<String, String> signed(Map<String, String> values) {
        values.put("vnp_SecureHash", gateway.sign(VnpayGateway.canonical(values)));
        return values;
    }

    PaymentDTO.ReturnConfirmation receipt(Map<String, String> values) {
        var request = new PaymentDTO.ReturnConfirmation();
        request.setParameters(new ArrayList<>(values.entrySet().stream()
                .map(e -> List.of(e.getKey(), e.getValue())).toList()));
        return request;
    }

    @Test
    void signedReturnAndLaterIpnGrantPlusAndQueueEmailOnlyOnce() {
        var values = result("00", "00");
        assertThat(payments.confirmReturn("teacher", id, receipt(values)).getStatus()).isEqualTo("SUCCEEDED");
        assertThat(payments.confirmReturn("teacher", id, receipt(values)).getStatus()).isEqualTo("SUCCEEDED");
        assertThat(payments.ipn(values)).containsEntry("RspCode", "02");
        verify(users, times(1)).grantPaidPlus(eq("teacher"), any(), eq(30));
        verify(repository, times(1)).enqueueEmail(any());
        verify(gateway, never()).query(any());
    }

    @Test
    void validReturnCanConfirmExpiredOrder() {
        order.setStatus("EXPIRED");
        order.setExpiresAt(Instant.now().minusSeconds(60));
        assertThat(payments.confirmReturn("teacher", id, receipt(result("00", "00"))).getStatus())
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void returnWithoutValidSignatureCannotGrantEvenWhenClientSaysSuccess() {
        var values = result("00", "00");
        values.remove("vnp_SecureHash");
        var unsigned = receipt(values);
        assertThatThrownBy(() -> payments.confirmReturn("teacher", id, unsigned))
                .isInstanceOf(BadRequestException.class);
        values = result("00", "00");
        values.put("vnp_Amount", "100");
        var tampered = receipt(values);
        assertThatThrownBy(() -> payments.confirmReturn("teacher", id, tampered))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    void signedReturnMustMatchOwnerReferenceMerchantAndAmount() {
        assertThatThrownBy(() -> payments.confirmReturn("another-teacher", id, receipt(result("00", "00"))))
                .isInstanceOf(NotFoundException.class);
        for (var change : List.of(Map.entry("vnp_TxnRef", "b".repeat(64)),
                Map.entry("vnp_TmnCode", "OTHER"), Map.entry("vnp_Amount", "100"))) {
            var values = result("00", "00");
            values.put(change.getKey(), change.getValue());
            var request = receipt(signed(values));
            assertThatThrownBy(() -> payments.confirmReturn("teacher", id, request))
                    .isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    void duplicateReturnParametersAreRejected() {
        var request = receipt(result("00", "00"));
        request.getParameters().add(List.of("vnp_Amount", "2000000"));
        assertThatThrownBy(() -> payments.confirmReturn("teacher", id, request))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    void signedReturnFailureDoesNotGrantPlus() {
        assertThat(payments.confirmReturn("teacher", id, receipt(result("24", "02"))).getStatus())
                .isEqualTo("CANCELLED");
        verifyNoInteractions(users);
    }

    @Test
    void successGrantsAndQueuesEmailOnceEvenWithRepeatedCallback() {
        var result = result("00", "00");
        assertThat(payments.ipn(result)).containsEntry("RspCode", "00");
        assertThat(payments.ipn(result)).containsEntry("RspCode", "02");
        assertThat(order.getStatus()).isEqualTo("SUCCEEDED");
        verify(users, times(1)).grantPaidPlus(eq("teacher"), any(), eq(30));
        verify(repository, times(1)).enqueueEmail(any());
        verify(manager, times(2)).commit(any());
    }

    @Test
    void fakeSignatureNeverReadsOrder() {
        var result = result("00", "00");
        result.put("vnp_Amount", "100");
        assertThat(payments.ipn(result)).containsEntry("RspCode", "97");
        verify(repository, never()).find(any());
        verifyNoInteractions(users);
    }

    @Test
    void signedWrongAmountDoesNotUpgrade() {
        var result = result("00", "00");
        result.put("vnp_Amount", "100");
        assertThat(payments.ipn(signed(result))).containsEntry("RspCode", "04");
        verifyNoInteractions(users);
        verify(repository, never()).update(any(), anyLong());
    }

    @Test
    void wrongMerchantAndUnknownOrderAreRejected() {
        var result = result("00", "00");
        result.put("vnp_TmnCode", "OTHER");
        assertThat(payments.ipn(signed(result))).containsEntry("RspCode", "97");
        var missing = result("00", "00");
        missing.put("vnp_TxnRef", "b".repeat(64));
        assertThat(payments.ipn(signed(missing))).containsEntry("RspCode", "01");
    }

    @Test
    void lateSuccessUpgradesExpiredOrder() {
        order.setStatus("EXPIRED");
        order.setExpiresAt(Instant.now().minusSeconds(60));
        assertThat(payments.ipn(result("00", "00"))).containsEntry("RspCode", "00");
        assertThat(order.getStatus()).isEqualTo("SUCCEEDED");
        verify(users).grantPaidPlus(eq("teacher"), any(), eq(30));
    }

    @Test
    void failureAndCancellationDoNotGrantPlus() {
        assertThat(payments.ipn(result("24", "02"))).containsEntry("RspCode", "00");
        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        verifyNoInteractions(users);
        ArgumentCaptor<PaymentEmailEntity> email = ArgumentCaptor.forClass(PaymentEmailEntity.class);
        verify(repository).enqueueEmail(email.capture());
        assertThat(email.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void failedPaymentQueuesFailureEmail() {
        payments.ipn(result("51", "02"));
        assertThat(order.getStatus()).isEqualTo("FAILED");
        verify(repository).enqueueEmail(any());
        verifyNoInteractions(users);
    }

    @Test
    void uncertainStatusDoesNotBecomePaidOrFailed() {
        payments.ipn(result("00", "01"));
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verifyNoInteractions(users);
        verify(repository, never()).enqueueEmail(any());
    }

    @Test
    void missingUserRecordsPaidButRequiresManualReview() {
        when(users.grantPaidPlus(any(), any(), anyInt())).thenReturn(null);
        payments.ipn(result("00", "00"));
        assertThat(order.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(order.isManualReview()).isTrue();
    }

    @Test
    void updateFailureRollsBackAndReturnsRetryAcknowledgement() {
        when(repository.update(any(), anyLong())).thenThrow(new IllegalStateException("db unavailable"));
        assertThat(payments.ipn(result("00", "00"))).containsEntry("RspCode", "99");
        verify(manager).rollback(any());
        verify(repository, never()).enqueueEmail(any());
    }

    @Test
    void concurrentWinnerIsReadAfterTransactionConflict() {
        when(repository.update(any(), anyLong())).thenThrow(new OptimisticLockingFailureException("race"));
        // The second read models another transaction having already committed success.
        assertThat(payments.ipn(result("00", "00"))).containsEntry("RspCode", "02");
        verify(manager).rollback(any());
        verify(manager).commit(any());
    }

    @Test
    void ownedOrderCannotBeReadOrVerifiedBySomeoneElse() {
        assertThatThrownBy(() -> payments.get("other", id)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> payments.verify("other", id)).isInstanceOf(NotFoundException.class);
        verify(gateway, never()).query(any());
    }

    @Test
    void expiredOrderQueuesEmailOnceAndDoesNotOfferPaymentUrl() {
        order.setExpiresAt(Instant.now().minusSeconds(1));
        assertThat(payments.get("teacher", id).getStatus()).isEqualTo("EXPIRED");
        assertThat(payments.get("teacher", id).getPaymentUrl()).isNull();
        verify(repository, times(1)).enqueueEmail(any());
    }

    @Test
    void createUsesServerPriceAndReturnsSameOrderForRetry() {
        when(repository.insert(any())).thenAnswer(i -> i.getArgument(0));
        var first = payments.create(teacher, "key-for-test-123456", "127.0.0.1");
        ArgumentCaptor<PaymentOrderEntity> capture = ArgumentCaptor.forClass(PaymentOrderEntity.class);
        verify(repository).insert(capture.capture());
        assertThat(capture.getValue().getExpiresAt())
                .isEqualTo(capture.getValue().getCreatedAt().plusSeconds(300));
        assertThat(first.getOrderId()).matches("[a-f0-9]{64}");
        assertThat(capture.getValue().getIdempotencyKeyHash()).matches("[a-f0-9]{64}")
                .isNotEqualTo(first.getOrderId());
        when(repository.findByIdempotencyKeyHash(capture.getValue().getIdempotencyKeyHash()))
                .thenReturn(capture.getValue());
        var second = payments.create(teacher, "key-for-test-123456", "127.0.0.2");
        assertThat(second.getOrderId()).isEqualTo(first.getOrderId());
        assertThat(second.getExpiresAt()).isEqualTo(first.getExpiresAt());
        assertThat(second.getAmount()).isEqualTo(20000);
        assertThat(second.getPaymentUrl()).contains("vnp_Amount=2000000").doesNotContain("vnp_BankCode=");
        verify(repository, times(1)).insert(any());
    }

    @Test
    void deletedOrderIsRecreatedWithDifferentProviderReferenceForSameKey() {
        when(repository.insert(any())).thenAnswer(i -> i.getArgument(0));
        var first = payments.create(teacher, "key-for-test-123456", "127.0.0.1");
        // The repository remains empty, simulating deletion between requests.
        var second = payments.create(teacher, "key-for-test-123456", "127.0.0.1");
        assertThat(second.getOrderId()).matches("[a-f0-9]{64}").isNotEqualTo(first.getOrderId());
        assertThat(second.getPaymentUrl()).contains("vnp_TxnRef=" + second.getOrderId());
        ArgumentCaptor<PaymentOrderEntity> capture = ArgumentCaptor.forClass(PaymentOrderEntity.class);
        verify(repository, times(2)).insert(capture.capture());
        assertThat(capture.getAllValues().get(0).getIdempotencyKeyHash())
                .isEqualTo(capture.getAllValues().get(1).getIdempotencyKeyHash());
    }

    @Test
    void legacyDeterministicOrderRemainsRetryable() throws Exception {
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest("teacher:key-for-test-123456".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        order.setId(hash);
        when(repository.find(hash)).thenReturn(order);
        assertThat(payments.create(teacher, "key-for-test-123456", "127.0.0.1").getOrderId()).isEqualTo(hash);
        verify(repository, never()).insert(any());
    }

    @Test
    void concurrentCreateReturnsWinningOrderAfterUniqueKeyConflict() {
        when(repository.findByIdempotencyKeyHash(anyString())).thenReturn(null, order);
        when(repository.insert(any())).thenThrow(new DuplicateKeyException("request already inserted"));
        assertThat(payments.create(teacher, "key-for-test-123456", "127.0.0.1").getOrderId()).isEqualTo(id);
        verifyNoInteractions(users);
    }

    @Test
    void unrelatedDuplicateKeyFailureIsNotMistakenForSuccessfulCreate() {
        when(repository.insert(any())).thenThrow(new DuplicateKeyException("other conflict"));
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void createRejectsDisabledConfigurationRoleAndInactiveUser() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(InternalServerException.class);
        properties.setEnabled(true);
        teacher.setRole("ROLE_ADMIN");
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class);
        teacher.setRole("ROLE_TEACHER");
        teacher.setIsActive(false);
        assertThatThrownBy(() -> payments.create(teacher, "key-for-test-123456", "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).insert(any());
    }

    @Test
    void verificationIsThrottledBeforeCallingProvider() {
        assertThat(payments.verify("teacher", id).getStatus()).isEqualTo("PENDING");
        verify(gateway, never()).query(any());
    }

    @Test
    void querySuccessCodeAloneDoesNotUpgrade() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        doReturn(result("00", "01")).when(gateway).query(order);
        assertThat(payments.verify("teacher", id).getStatus()).isEqualTo("PENDING");
        verifyNoInteractions(users);
    }

    @Test
    void verifiedQueryUsesSameGrantPathAndIpnCannotGrantAgain() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        var result = result("00", "00");
        result.put("vnp_TransactionType", "01");
        doReturn(result).when(gateway).query(order);
        assertThat(payments.verify("teacher", id).getStatus()).isEqualTo("SUCCEEDED");
        assertThat(payments.ipn(signed(result))).containsEntry("RspCode", "02");
        verify(users, times(1)).grantPaidPlus(any(), any(), anyInt());
    }

    @Test
    void providerTimeoutLeavesOrderPending() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        doThrow(new IllegalStateException()).when(gateway).query(order);
        assertThatThrownBy(() -> payments.verify("teacher", id)).isInstanceOf(ServiceUnavailableException.class);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verifyNoInteractions(users);
    }

    @Test
    void unverifiedProviderErrorCannotUpgradeOrUpdateOrder() {
        when(repository.claimVerification(any(), any(), any())).thenReturn(true);
        doThrow(new VnpayQueryException("UNVERIFIED_RESPONSE", "99", null)).when(gateway).query(order);
        assertThatThrownBy(() -> payments.verify("teacher", id)).isInstanceOf(ServiceUnavailableException.class);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        verify(repository, never()).update(any(), anyLong());
        verifyNoInteractions(users);
    }
}
