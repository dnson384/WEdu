package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.domain.entity.*;
import com.wedu.exam_creation.payment.domain.repository.*;
import com.wedu.exam_creation.payment.dto.RefundDTO;
import com.wedu.exam_creation.payment.infrastructure.*;
import com.wedu.exam_creation.payment.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundUsecaseTest {
    IRefundRepository refunds = mock(IRefundRepository.class);
    IPaymentRepository payments = mock(IPaymentRepository.class);
    RefundAiService ai = mock(RefundAiService.class);
    RefundEntitlementService entitlement = mock(RefundEntitlementService.class);
    RefundTransactions transactions = mock(RefundTransactions.class);
    VnpayGateway gateway = mock(VnpayGateway.class);
    VnpayProperties properties = new VnpayProperties();
    RefundSettings settings = new RefundSettings();
    RefundUsecase usecase;
    PaymentOrderEntity order;
    RefundAccountEntity account;
    RefundEntity stored;
    String orderId = "a".repeat(64);
    String key = "test-refund-key-12345";

    @BeforeEach void setup() {
        settings.setEnabled(true); properties.setEnabled(true); properties.setTmnCode("TESTCODE");
        properties.setHashSecret("test-only"); properties.setReturnUrl("https://example.com/me/bill");
        when(transactions.run(any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        account = new RefundAccountEntity(); account.setId("teacher"); account.setTrackedSince(Instant.now().minusSeconds(100000));
        when(ai.lockedAccount("teacher")).thenReturn(account); when(refunds.account("teacher")).thenReturn(account);
        order = new PaymentOrderEntity(); order.setId(orderId); order.setUserId("teacher"); order.setEmail("teacher@example.com");
        order.setAmount(20000); order.setDurationDays(30); order.setStatus("SUCCEEDED"); order.setTransactionNo("123");
        order.setCreatedAt(Instant.now().minusSeconds(3600)); order.setPaidAt(order.getCreatedAt().plusSeconds(20));
        order.setPlusStartsAt(order.getPaidAt()); order.setPlusExpiresAt(order.getPaidAt().plusSeconds(2592000)); order.setRefundTracked(true);
        when(payments.find(orderId)).thenReturn(order);
        when(entitlement.preview(eq(order), any())).thenReturn(Instant.now());
        when(entitlement.revoke(eq(order), any())).thenReturn(true);
        doAnswer(i -> { stored = i.getArgument(0); when(refunds.find(stored.getId())).thenAnswer(x -> stored); return null; }).when(refunds).insert(any());
        when(refunds.update(any(), anyLong())).thenReturn(true);
        when(gateway.query(order)).thenAnswer(i -> response("querydr", "01", "00", "00", "123"));
        when(gateway.refund(eq(order), any())).thenAnswer(i -> response("refund", "02", "00", "05", "456"));
        when(refunds.claimPoll(anyString(), any())).thenReturn(true);
        usecase = new RefundUsecase(refunds, payments, ai, entitlement, transactions, settings, gateway, properties);
    }
    RefundDTO.Create body() { RefundDTO.Create b = new RefundDTO.Create(); b.setReason("Không còn nhu cầu"); b.setPolicyVersion(RefundSettings.POLICY); return b; }
    RefundDTO.Decision decision() { RefundDTO.Decision b = new RefundDTO.Decision(); b.setRevision(stored.getRevision()); return b; }
    RefundDTO.Item create() { return usecase.create("teacher", orderId, key, body()); }
    Map<String, String> response(String command, String type, String code, String status, String number) {
        return new HashMap<>(Map.of("vnp_Command", command, "vnp_TmnCode", "TESTCODE", "vnp_TxnRef", orderId,
                "vnp_Amount", "2000000", "vnp_TransactionType", type, "vnp_ResponseCode", code,
                "vnp_TransactionStatus", status, "vnp_TransactionNo", number));
    }
    @Test void receivesEligibleRequestAndHoldsAi() {
        var result = create();
        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(account.getHolds()).containsExactly(result.getId());
        verify(payments).enqueueEmail(argThat(e -> e.getRefundId().equals(result.getId())));
        verifyNoInteractions(gateway);
    }
    @Test void deadlineUsesSubmissionTimeAndAdminCanApproveLater() {
        create(); stored.setCreatedAt(order.getPaidAt().plusSeconds(172800));
        var result = usecase.decide(null, stored.getId(), "admin", "approve", decision());
        assertThat(result.getStatus()).isEqualTo("PROCESSING");
        verify(gateway).refund(eq(order), any());
    }
    @Test void rejectsAfterDeadlineWithReasonAndNoHold() {
        order.setPaidAt(Instant.now().minusSeconds(172801));
        assertThat(create().getStatus()).isEqualTo("REJECTED");
        assertThat(stored.getDecisionReason()).contains("48 giờ"); assertThat(account.getHolds()).isEmpty();
        verifyNoInteractions(gateway);
    }
    @Test void rejectsPersistedAiUse() {
        when(refunds.lastAiUse("teacher")).thenReturn(Instant.now());
        assertThat(create().getStatus()).isEqualTo("REJECTED"); assertThat(stored.getDecisionReason()).contains("AI");
    }
    @Test void alsoUsesCommitEvidenceWhenAiWasCommittedAfterPurchase() {
        account.setLastAiUsedAt(Instant.now());
        assertThat(create().getStatus()).isEqualTo("REJECTED");
    }
    @Test void activeAiNeedsVerificationAndCannotBeOverridden() {
        account.getActiveAi().add("running"); create();
        var b = decision(); b.setLegacyException(true); b.setReason("Đã xác minh");
        assertThat(stored.getStatus()).isEqualTo("NEEDS_VERIFICATION");
        assertThatThrownBy(() -> usecase.decide(null, stored.getId(), "admin", "approve", b)).isInstanceOf(AdminTransactionException.class);
        verify(gateway, never()).refund(any(), any());
    }
    @Test void legacyRequiresExplicitExceptionAndReason() {
        order.setRefundTracked(false); create();
        assertThat(stored.getStatus()).isEqualTo("NEEDS_VERIFICATION");
        assertThatThrownBy(() -> usecase.decide(null, stored.getId(), "admin", "approve", decision())).isInstanceOf(AdminTransactionException.class);
        var b = decision(); b.setLegacyException(true); b.setReason("Đối chiếu dữ liệu hỗ trợ khách hàng");
        assertThat(usecase.decide(null, stored.getId(), "admin", "approve", b).getStatus()).isEqualTo("PROCESSING");
        assertThat(stored.getHistory()).anyMatch(e -> e.getReason().contains("Ngoại lệ"));
    }
    @Test void cannotOverrideUnknownEntitlement() {
        when(entitlement.preview(eq(order), any())).thenReturn(null); order.setRefundTracked(false); create();
        var b = decision(); b.setLegacyException(true); b.setReason("Đã kiểm tra");
        assertThatThrownBy(() -> usecase.decide(null, stored.getId(), "admin", "approve", b)).isInstanceOf(AdminTransactionException.class);
        verify(gateway, never()).refund(any(), any());
    }
    @Test void ownershipCannotBeBypassed() {
        assertThatThrownBy(() -> usecase.create("other", orderId, key, body())).isInstanceOf(AdminTransactionException.class);
        create(); assertThatThrownBy(() -> usecase.get("other", stored.getId())).isInstanceOf(AdminTransactionException.class);
    }
    @Test void replayReturnsSameRequestAndDoesNotQueueAnotherEmail() {
        create(); when(refunds.findAttempt("teacher", key)).thenAnswer(i -> stored);
        assertThat(create().getId()).isEqualTo(stored.getId()); verify(refunds, times(1)).insert(any());
        verify(payments, times(1)).enqueueEmail(any());
    }
    @Test void differentKeyCannotCreateSecondActiveRequest() {
        create(); when(refunds.activeForOrder(orderId)).thenAnswer(i -> stored);
        assertThatThrownBy(() -> usecase.create("teacher", orderId, "another-key-1234567", body())).isInstanceOf(AdminTransactionException.class);
    }
    @Test void rejectionRequiresReasonAndReleasesOnlyOwnHold() {
        create(); account.getHolds().add("other-refund");
        assertThatThrownBy(() -> usecase.decide(null, stored.getId(), "admin", "reject", decision())).isInstanceOf(AdminTransactionException.class);
        var b = decision(); b.setReason("Chưa đáp ứng hồ sơ xác minh");
        assertThat(usecase.decide(null, stored.getId(), "admin", "reject", b).getStatus()).isEqualTo("REJECTED");
        assertThat(account.getHolds()).containsExactly("other-refund"); verifyNoInteractions(gateway);
    }
    @Test void cancelReleasesHoldAndCannotCancelProcessing() {
        create(); assertThat(usecase.decide("teacher", stored.getId(), "teacher", "cancel", decision()).getStatus()).isEqualTo("CANCELLED");
        assertThat(account.getHolds()).isEmpty();
        assertThatThrownBy(() -> usecase.decide("teacher", stored.getId(), "teacher", "cancel", decision())).isInstanceOf(AdminTransactionException.class);
    }
    @Test void staleRevisionCannotSendRefund() {
        create(); var b = decision(); b.setRevision(99L);
        assertThatThrownBy(() -> usecase.decide(null, stored.getId(), "admin", "approve", b)).isInstanceOf(AdminTransactionException.class);
        verifyNoInteractions(gateway);
    }
    @Test void transportFailureKeepsProcessingAndNeverResends() {
        create(); when(gateway.refund(eq(order), any())).thenThrow(new RuntimeException("timeout"));
        assertThat(usecase.decide(null, stored.getId(), "admin", "approve", decision()).getStatus()).isEqualTo("PROCESSING");
        assertThatThrownBy(() -> usecase.decide(null, stored.getId(), "admin", "approve", decision())).isInstanceOf(AdminTransactionException.class);
        verify(gateway, times(1)).refund(any(), any()); assertThat(account.getHolds()).contains(stored.getId());
    }
    @Test void successfulPaymentQueryIsNotSuccessfulRefund() {
        create(); usecase.decide(null, stored.getId(), "admin", "approve", decision());
        when(gateway.queryRefund(eq(order), any())).thenReturn(response("querydr", "01", "00", "00", "123"));
        assertThat(usecase.reconcile(stored.getId()).getStatus()).isEqualTo("PROCESSING"); verify(entitlement, never()).revoke(any(), any());
    }
    @Test void confirmedRefundRevokesOnceAndFinalStateCannotReconcile() {
        create(); usecase.decide(null, stored.getId(), "admin", "approve", decision());
        when(gateway.queryRefund(eq(order), any())).thenReturn(response("querydr", "02", "00", "00", "456"));
        assertThat(usecase.reconcile(stored.getId()).getStatus()).isEqualTo("REFUNDED");
        assertThat(account.getHolds()).isEmpty();
        assertThatThrownBy(() -> usecase.reconcile(stored.getId())).isInstanceOf(AdminTransactionException.class);
        verify(entitlement, times(1)).revoke(any(), any());
    }
    @Test void mismatchCannotRevokeAndPendingAfter24hStopsPolling() {
        create(); usecase.decide(null, stored.getId(), "admin", "approve", decision()); stored.setSubmittedAt(Instant.now().minusSeconds(86401));
        Map<String, String> bad = response("querydr", "02", "00", "00", "456"); bad.put("vnp_Amount", "1");
        when(gateway.queryRefund(eq(order), any())).thenReturn(bad);
        assertThat(usecase.reconcile(stored.getId()).getStatus()).isEqualTo("PROCESSING"); assertThat(stored.isPollingStopped()).isTrue();
        verify(entitlement, never()).revoke(any(), any());
    }
    @Test void definiteProviderRejectionReleasesHold() {
        create(); when(gateway.refund(eq(order), any())).thenReturn(response("refund", "02", "00", "09", "456"));
        assertThat(usecase.decide(null, stored.getId(), "admin", "approve", decision()).getStatus()).isEqualTo("FAILED");
        assertThat(account.getHolds()).isEmpty(); verify(entitlement, never()).revoke(any(), any());
    }
    @Test void featureIsOffByDefault() {
        settings.setEnabled(false);
        assertThatThrownBy(this::create).isInstanceOf(AdminTransactionException.class); verifyNoInteractions(gateway);
        assertThat(new RefundSettings().isEnabled()).isFalse();
    }
    @Test void originalMismatchDoesNotStartRefund() {
        create(); var bad = response("querydr", "01", "00", "00", "999"); when(gateway.query(order)).thenReturn(bad);
        assertThatThrownBy(() -> usecase.decide(null, stored.getId(), "admin", "approve", decision())).isInstanceOf(AdminTransactionException.class);
        assertThat(stored.getStatus()).isEqualTo("PENDING_REVIEW"); verify(gateway, never()).refund(any(), any());
    }
}
