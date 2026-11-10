package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.usecase.RefundEntitlementService;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundEntitlementTest {
    final Instant now = Instant.parse("2026-11-09T00:00:00Z");
    IPaymentRepository payments = mock(IPaymentRepository.class);
    UserService users = mock(UserService.class);
    RefundEntitlementService service = new RefundEntitlementService(payments, users);
    PaymentOrderEntity grant(String id, Instant start) {
        var o = new PaymentOrderEntity(); o.setId(id); o.setUserId("u"); o.setStatus("SUCCEEDED"); o.setDurationDays(30);
        o.setPaidAt(now.minusSeconds(86400)); o.setPlusStartsAt(start); o.setPlusExpiresAt(start.plusSeconds(2592000)); return o;
    }
    @Test void refundFirstOrderPreservesNextPurchaseAndDoesNotDeductElapsedTime() {
        var first = grant("first", now.minusSeconds(86400)); var next = grant("next", first.getPlusExpiresAt());
        when(payments.ordersForUser("u")).thenReturn(List.of(first, next)); when(users.paidPlusExpiry("u")).thenReturn(next.getPlusExpiresAt());
        when(payments.update(any(), anyLong())).thenReturn(true); when(users.revokePaidTime(anyString(), any())).thenReturn(true);
        assertThat(service.preview(first, now)).isEqualTo(now.plusSeconds(2592000));
        assertThat(service.revoke(first, now)).isTrue();
        assertThat(next.getPlusStartsAt()).isEqualTo(now); assertThat(next.getPlusExpiresAt()).isEqualTo(now.plusSeconds(2592000));
        assertThat(first.getRefundedAt()).isEqualTo(now);
        service.revoke(first, now); verify(users, times(1)).revokePaidTime("u", now.plusSeconds(2592000));
    }
    @Test void refundFutureRenewalRemovesExactlyItsThirtyDays() {
        var first = grant("first", now.minusSeconds(86400)); var next = grant("next", first.getPlusExpiresAt());
        when(payments.ordersForUser("u")).thenReturn(List.of(first, next)); when(users.paidPlusExpiry("u")).thenReturn(next.getPlusExpiresAt());
        assertThat(service.preview(next, now)).isEqualTo(first.getPlusExpiresAt());
    }
    @Test void legacyIndefinitePlusIsNeverDestroyed() {
        var o = grant("first", now); when(users.paidPlusExpiry("u")).thenReturn(null);
        assertThat(service.revoke(o, now)).isFalse(); verify(users, never()).revokePaidTime(anyString(), any());
    }
    @Test void purchaseWithLegacyBaselineRequiresVerificationEvenAfterExpiryWasSet() {
        var o = grant("first", now); o.setPlusLegacyProtected(true);
        when(users.paidPlusExpiry("u")).thenReturn(o.getPlusExpiresAt());
        assertThat(service.revoke(o, now)).isFalse(); verify(users, never()).revokePaidTime(anyString(), any());
    }
    @Test void reconstructsLegacyFiniteIntervalWhenEvidenceMatches() {
        var o = grant("first", now); o.setPlusStartsAt(null);
        when(payments.ordersForUser("u")).thenReturn(List.of(o)); when(users.paidPlusExpiry("u")).thenReturn(o.getPlusExpiresAt());
        assertThat(service.preview(o, now)).isEqualTo(now);
    }
    @Test void inconsistentOrOverlappingGrantsRequireVerification() {
        var first = grant("first", now); var next = grant("next", now.plusSeconds(86400));
        when(payments.ordersForUser("u")).thenReturn(List.of(first, next)); when(users.paidPlusExpiry("u")).thenReturn(next.getPlusExpiresAt());
        assertThat(service.preview(first, now)).isNull();
    }
    @Test void expiredOrderCannotRemoveTimeFromOtherPurchases() {
        var old = grant("old", now.minusSeconds(40 * 86400L)); old.setPaidAt(old.getPlusStartsAt());
        var next = grant("next", now.minusSeconds(86400));
        when(payments.ordersForUser("u")).thenReturn(List.of(old, next)); when(users.paidPlusExpiry("u")).thenReturn(next.getPlusExpiresAt());
        assertThat(service.preview(old, now)).isEqualTo(next.getPlusExpiresAt());
    }
}
