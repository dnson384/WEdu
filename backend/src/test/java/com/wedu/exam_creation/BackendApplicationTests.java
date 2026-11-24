package com.wedu.exam_creation;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.wedu.exam_creation.auth.infrastructure.AuthIndexConfig;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.payment.infrastructure.AdminTransactionIndexConfig;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import com.wedu.exam_creation.payment.usecase.PaymentStorageMigrationUsecase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        properties = {
            "JWT_ACCESS_SECRET=VmKHS3Ne9qibwd/wEb2JrzzAF4M2Vk4xUpJOU/tHacg=",
            "JWT_REFRESH_SECRET=Pt8ctKox/C1foTSk40+E8CY5vKyOlf0FDzpICNt6dRw=",
            "AWS_ACCESS_KEY_ID=mock-access-key-id",
            "AWS_SECRET_ACCESS_KEY=mock-secret-access-key",
            "VNPAY_ENABLED=false",
            "VNPAY_TMN_CODE=test-tmn",
            "VNPAY_HASH_SECRET=test-vnpay-value",
            "VNPAY_PAYMENT_URL=https://vnpay.example.test/pay",
            "VNPAY_QUERY_URL=https://vnpay.example.test/query",
            "VNPAY_RETURN_URL=https://wedu.example.test/payment/return",
            "OTP_SECRET=test-only-otp-secret-at-least-32-characters",
            "GOOGLE_CLIENT_ID=test-google-id",
            "GOOGLE_CLIENT_SECRET=test-google-value",
            "DEEPSEEK_API_KEY=test-deepseek-value",
            "SESSION_COOKIE_SECURE=false",
            "spring.mongodb.uri=mongodb://localhost:27017/wedu-test",
            "app.frontend.url=http://localhost:3000",
            "export.url=http://localhost:8000/exporter/",
            "spring.mail.host=localhost",
            "spring.mail.port=2525",
            "spring.mail.username=test@example.com",
            "spring.mail.password=test-password"
        })
class BackendApplicationTests {
    @Autowired private VnpayProperties vnpayProperties;

    @MockitoBean private MongoClient mongoClient;

    @MockitoBean private AuthIndexConfig authIndexConfig;

    @MockitoBean private AdminTransactionIndexConfig adminTransactionIndexConfig;

    @MockitoBean private PaymentStorageMigrationUsecase paymentStorageMigration;

    @MockitoBean private TelegramNotificationService telegramNotificationService;

    @MockitoBean private MailService mailService;

    @Test
    void contextLoads() {
        assertThat(vnpayProperties.isEnabled()).isFalse();
        assertThat(vnpayProperties.available()).isFalse();
    }
}
