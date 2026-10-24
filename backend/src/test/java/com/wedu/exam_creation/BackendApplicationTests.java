package com.wedu.exam_creation;

import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.mail.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "JWT_ACCESS_SECRET=VmKHS3Ne9qibwd/wEb2JrzzAF4M2Vk4xUpJOU/tHacg=",
        "JWT_REFRESH_SECRET=Pt8ctKox/C1foTSk40+E8CY5vKyOlf0FDzpICNt6dRw=",
        "AWS_ACCESS_KEY_ID=mock-access-key-id",
        "AWS_SECRET_ACCESS_KEY=mock-secret-access-key",
        "app.frontend.url=http://localhost:3000",
        "export.url=http://localhost:8000/exporter/",
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test@example.com",
        "spring.mail.password=test-password"
})
class BackendApplicationTests {
    @MockitoBean
    private TelegramNotificationService telegramNotificationService;

    @MockitoBean
    private MailService mailService;

    @Test
    void contextLoads() {
    }

}
