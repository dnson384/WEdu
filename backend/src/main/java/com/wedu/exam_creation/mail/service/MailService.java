package com.wedu.exam_creation.mail.service;

import com.wedu.exam_creation.common.exception.InternalServerException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendResetPasswordEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "WEdu Support");
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu WEdu");
            helper.setText(buildHtmlContent(resetLink), true);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new InternalServerException("Không thể gửi email đặt lại mật khẩu");
        }
    }

    private String buildHtmlContent(String resetLink) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2>Yêu cầu đặt lại mật khẩu</h2>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản WEdu của bạn.</p>
                    <p>
                        <a href="%s"
                           style="display:inline-block; padding:12px 24px; background-color:#2563eb;
                                  color:#ffffff; text-decoration:none; border-radius:6px;">
                            Đặt lại mật khẩu
                        </a>
                    </p>
                    <p>Liên kết này sẽ hết hạn sau 15 phút.</p>
                    <p style="color:#666; font-size:13px;">
                        Nếu bạn không yêu cầu điều này, vui lòng bỏ qua email này —
                        mật khẩu của bạn sẽ không thay đổi.
                    </p>
                </div>
                """.formatted(resetLink);
    }

}
