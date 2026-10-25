package com.wedu.exam_creation.mail.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MailRejectionTest {
    @Test
    void sendsUnicodeReasonAsPlainText() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        new MailService(sender, "support@example.com").sendQuestionFileRejectionEmail(
                "teacher@example.com", "DeThi.docx", "Sai nội dung <script>alert(1)</script>");
        message.saveChanges();
        verify(sender).send(message);
        assertThat(message.isMimeType("text/plain")).isTrue();
        assertThat(message.getContent().toString()).contains("DeThi.docx", "Sai nội dung <script>alert(1)</script>");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("teacher@example.com");
    }
}
