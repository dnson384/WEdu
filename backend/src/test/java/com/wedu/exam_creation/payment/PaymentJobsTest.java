package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.domain.entity.PaymentEmailEntity;
import com.wedu.exam_creation.payment.domain.repository.IPaymentRepository;
import com.wedu.exam_creation.payment.usecase.*;
import com.wedu.exam_creation.mail.service.MailService;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.mockito.Mockito.*;

class PaymentJobsTest {
    @Test void mailFailureIsRecordedForRetryWithoutTouchingEntitlement() {
        IPaymentRepository repository = mock(IPaymentRepository.class);
        PaymentUsecase payments = mock(PaymentUsecase.class);
        MailService mail = mock(MailService.class);
        PaymentEmailEntity email = new PaymentEmailEntity();
        email.setId("order:SUCCEEDED");
        email.setOrderId("order");
        email.setEmail("test@example.com");
        email.setStatus("SUCCEEDED");
        email.setPlusExpiresAt(Instant.now());
        email.setAttempts(1);
        when(repository.claimEmail(any(), any())).thenReturn(email, (PaymentEmailEntity) null);
        doThrow(new IllegalStateException("mail unavailable")).when(mail).sendPaymentResultEmail(any(), any(), any());
        new PaymentJobs(repository, payments, mail).sendEmails();
        verify(repository).completeEmail(eq(email.getId()), any(), any(), eq(false));
        verifyNoInteractions(payments);
    }
    @Test void successfulMailMarksSentWithLeaseToken() {
        IPaymentRepository repository = mock(IPaymentRepository.class);
        MailService mail = mock(MailService.class);
        PaymentEmailEntity email = new PaymentEmailEntity();
        email.setId("order:FAILED");
        email.setOrderId("order");
        email.setEmail("test@example.com");
        email.setStatus("FAILED");
        when(repository.claimEmail(any(), any())).thenReturn(email, (PaymentEmailEntity) null);
        new PaymentJobs(repository, mock(PaymentUsecase.class), mail).sendEmails();
        verify(mail).sendPaymentResultEmail(eq("test@example.com"), eq("order"), contains("không thành công"));
        verify(repository).completeEmail(eq(email.getId()), any(), any(), eq(true));
    }
}
