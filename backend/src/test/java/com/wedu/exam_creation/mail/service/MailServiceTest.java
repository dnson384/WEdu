package com.wedu.exam_creation.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.exception.InternalServerException;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class MailServiceTest {
    private static final String FROM = "support@example.com";
    private static final String RECIPIENT = "teacher@example.com";
    private static final String INVALID_RECIPIENT = "teacher@";
    private static final String RESET_LINK = "https://wedu.example/reset?token=test-token";

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final MailService service = new MailService(sender, FROM);
    private final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

    @BeforeEach
    void prepareMessage() {
        when(sender.createMimeMessage()).thenReturn(message);
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và gửi nội dung văn bản thuần khi từ chối tệp có lý do chứa thẻ HTML")
    void test_sendQuestionFileRejectionEmail_001() throws Exception {
        service.sendQuestionFileRejectionEmail(
                RECIPIENT, "Đề thi.docx", "Sai nội dung <b>câu 1</b>");

        assertEnvelope("Tệp câu hỏi của bạn đã bị từ chối");
        assertThat(message.isMimeType("text/plain")).isTrue();
        assertThat(message.getContent())
                .isEqualTo("Tệp câu hỏi: Đề thi.docx\n\nLý do từ chối:\nSai nội dung <b>câu 1</b>");
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và gửi nội dung văn bản thuần khi xác thực email bằng OTP")
    void test_sendVerificationOtp_002() throws Exception {
        service.sendVerificationOtp(RECIPIENT, "012345");

        assertEnvelope("Mã xác thực email WEdu");
        assertThat(message.isMimeType("text/plain")).isTrue();
        assertThat(message.getContent())
                .isEqualTo(
                        "Mã OTP của bạn: 012345\n\nMã có hiệu lực 5 phút. Không chia sẻ mã này với người khác.");
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và gửi nội dung văn bản thuần khi thông báo kết quả thanh toán")
    void test_sendPaymentResultEmail_003() throws Exception {
        service.sendPaymentResultEmail(RECIPIENT, "order-123", "Đã kích hoạt WEdu PLUS.");

        assertEnvelope("Kết quả thanh toán WEdu PLUS");
        assertThat(message.isMimeType("text/plain")).isTrue();
        assertThat(message.getContent()).isEqualTo("Mã đơn: order-123\n\nĐã kích hoạt WEdu PLUS.");
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và gửi HTML chứa liên kết và hạn 15 phút khi yêu cầu đặt lại mật khẩu")
    void test_sendResetPasswordEmail_004() throws Exception {
        service.sendResetPasswordEmail(RECIPIENT, RESET_LINK);

        assertEnvelope("Đặt lại mật khẩu WEdu");
        Multipart mixed = (Multipart) message.getContent();
        Multipart related = (Multipart) mixed.getBodyPart(0).getContent();
        BodyPart html = related.getBodyPart(0);
        assertThat(html.isMimeType("text/html")).isTrue();
        assertThat(html.getContent())
                .isInstanceOfSatisfying(
                        String.class,
                        content ->
                                assertThat(content)
                                        .contains(
                                                "href=\"" + RESET_LINK + "\"",
                                                "Liên kết này sẽ hết hạn sau 15 phút."));
    }

    @Test
    @DisplayName(
            "trả về InternalServerException khi địa chỉ nhận không hợp lệ và từ chối tệp có lý do chứa thẻ HTML")
    void test_sendQuestionFileRejectionEmail_005() {
        assertThatThrownBy(
                        () ->
                                service.sendQuestionFileRejectionEmail(
                                        INVALID_RECIPIENT,
                                        "Đề thi.docx",
                                        "Sai nội dung <b>câu 1</b>"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Không thể gửi email từ chối tệp câu hỏi");

        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName(
            "trả về InternalServerException khi địa chỉ nhận không hợp lệ và xác thực email bằng OTP")
    void test_sendVerificationOtp_006() {
        assertThatThrownBy(() -> service.sendVerificationOtp(INVALID_RECIPIENT, "012345"))
                .isInstanceOf(InternalServerException.class);

        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName(
            "trả về InternalServerException khi địa chỉ nhận không hợp lệ và thông báo kết quả thanh toán")
    void test_sendPaymentResultEmail_007() {
        assertThatThrownBy(
                        () ->
                                service.sendPaymentResultEmail(
                                        INVALID_RECIPIENT, "order-123", "Đã kích hoạt WEdu PLUS."))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Không thể gửi email kết quả thanh toán");

        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName(
            "trả về InternalServerException khi địa chỉ nhận không hợp lệ và yêu cầu đặt lại mật khẩu")
    void test_sendResetPasswordEmail_008() {
        assertThatThrownBy(() -> service.sendResetPasswordEmail(INVALID_RECIPIENT, RESET_LINK))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Không thể gửi email đặt lại mật khẩu");

        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("trả về MailSendException khi mail sender báo lỗi và chỉ thử gửi một lần")
    void test_sendPaymentResultEmail_009() {
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(sender).send(message);

        assertThatThrownBy(
                        () ->
                                service.sendPaymentResultEmail(
                                        RECIPIENT, "order-123", "Đã thanh toán"))
                .isSameAs(failure);

        verify(sender).send(message);
    }

    private void assertEnvelope(String subject) throws Exception {
        verify(sender).send(message);
        message.saveChanges();

        assertThat(message.getSubject()).isEqualTo(subject);
        assertThat(message.getRecipients(Message.RecipientType.TO))
                .containsExactly(new InternetAddress(RECIPIENT));
        assertThat(message.getAllRecipients()).containsExactly(new InternetAddress(RECIPIENT));
        assertThat(message.getFrom())
                .singleElement()
                .isInstanceOfSatisfying(
                        InternetAddress.class,
                        address -> {
                            assertThat(address.getAddress()).isEqualTo(FROM);
                            assertThat(address.getPersonal()).isEqualTo("WEdu Support");
                        });
    }
}
