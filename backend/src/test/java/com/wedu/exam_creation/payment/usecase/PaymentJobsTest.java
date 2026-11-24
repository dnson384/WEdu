package com.wedu.exam_creation.payment.usecase;

import static org.mockito.Mockito.*;

import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentJobsTest {
    @Test
    @DisplayName("trả về không có ngoại lệ và ghi nhận gửi thất bại khi mail service báo lỗi")
    void test_sendEmails_001() {
        IPaymentRepository repository = mock(IPaymentRepository.class);
        PaymentUsecase payments = mock(PaymentUsecase.class);
        MailService mail = mock(MailService.class);
        PaymentEmailEntity email = new PaymentEmailEntity();
        email.setId("order:SUCCEEDED");
        email.setOrderId("order");
        email.setEmail("test@example.com");
        email.setStatus("SUCCEEDED");
        email.setPlusExpiresAt(Instant.parse("2026-11-21T00:00:00Z"));
        email.setAttempts(1);
        when(repository.claimEmail(any(), any())).thenReturn(email, (PaymentEmailEntity) null);
        doThrow(new IllegalStateException("mail unavailable"))
                .when(mail)
                .sendPaymentResultEmail(any(), any(), any());
        new PaymentJobs(repository, payments, mail).sendEmails();
        verify(repository).completeEmail(eq(email.getId()), any(), any(), eq(false));
        verifyNoInteractions(payments);
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và đánh dấu email đã gửi khi mail service gửi thành công")
    void test_sendEmails_002() {
        IPaymentRepository repository = mock(IPaymentRepository.class);
        MailService mail = mock(MailService.class);
        PaymentEmailEntity email = new PaymentEmailEntity();
        email.setId("order:FAILED");
        email.setOrderId("order");
        email.setEmail("test@example.com");
        email.setStatus("FAILED");
        when(repository.claimEmail(any(), any())).thenReturn(email, (PaymentEmailEntity) null);
        new PaymentJobs(repository, mock(PaymentUsecase.class), mail).sendEmails();
        verify(mail)
                .sendPaymentResultEmail(
                        eq("test@example.com"), eq("order"), contains("không thành công"));
        verify(repository).completeEmail(eq(email.getId()), any(), any(), eq(true));
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và tiếp tục xử lý đơn sau khi một đơn hết hạn cập nhật lỗi")
    void test_expireOrders_003() {
        IPaymentRepository repository = mock(IPaymentRepository.class);
        PaymentUsecase payments = mock(PaymentUsecase.class);
        PaymentOrderEntity first = new PaymentOrderEntity();
        first.setId("first");
        PaymentOrderEntity second = new PaymentOrderEntity();
        second.setId("second");
        when(repository.expiredPending(any())).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("test storage failure")).when(payments).expire("first");

        new PaymentJobs(repository, payments, mock(MailService.class)).expireOrders();

        org.mockito.InOrder order = inOrder(payments);
        order.verify(payments).expire("first");
        order.verify(payments).expire("second");
    }
}
