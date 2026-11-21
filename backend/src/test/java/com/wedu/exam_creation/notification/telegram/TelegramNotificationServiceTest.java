package com.wedu.exam_creation.notification.telegram;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

class TelegramNotificationServiceTest {
    private static final String QUESTION_URL =
            "https://api.telegram.org/botfake-question-token/sendMessage";
    private static final String S3_URL = "https://api.telegram.org/botfake-s3-token/sendMessage";

    private final TelegramNotificationService service =
            new TelegramNotificationService("fake-question-token", "fake-s3-token", "test-chat");
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(
                            (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate"))
                    .build();

    @Test
    @DisplayName("trả về không có ngoại lệ khi gửi cảnh báo xóa ảnh với S3 key hợp lệ")
    void test_notifyFailedDeleteImage_001() {
        String key = "media/avatar.png";
        server.expect(requestTo(S3_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(jsonPath("$.chat_id").value("test-chat"))
                .andExpect(jsonPath("$.parse_mode").value("Markdown"))
                .andExpect(jsonPath("$.text").value("📋 *Ảnh bị xóa lỗi:*\n" + key))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        service.notifyFailedDeleteImage(key);

        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " \t\n"})
    @DisplayName(
            "trả về không có ngoại lệ và bỏ qua gửi HTTP khi S3 key rỗng hoặc chỉ có khoảng trắng")
    void test_notifyFailedDeleteImage_002(String key) {
        service.notifyFailedDeleteImage(key);

        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi HTTP 500 khi Telegram không gửi được cảnh báo xóa ảnh")
    void test_notifyFailedDeleteImage_003() {
        server.expect(requestTo(S3_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service.notifyFailedDeleteImage("media/avatar.png"))
                .isInstanceOf(HttpServerErrorException.InternalServerError.class);

        server.verify();
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ khi gửi danh sách ID câu hỏi cần kiểm duyệt theo đúng thứ tự")
    void test_notifyPendingReview_004() {
        server.expect(requestTo(QUESTION_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(jsonPath("$.chat_id").value("test-chat"))
                .andExpect(jsonPath("$.parse_mode").value("Markdown"))
                .andExpect(
                        jsonPath("$.text")
                                .value(
                                        "📋 *Câu hỏi mới từ AI cần kiểm duyệt:*\n\n• `question-1`\n• `question-2`\n"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        service.notifyPendingReview(List.of("question-1", "question-2"));

        server.verify();
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và bỏ qua gửi HTTP khi danh sách câu hỏi rỗng")
    void test_notifyPendingReview_005() {
        service.notifyPendingReview(List.of());

        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi HTTP 500 khi Telegram không gửi được thông báo kiểm duyệt")
    void test_notifyPendingReview_006() {
        server.expect(requestTo(QUESTION_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service.notifyPendingReview(List.of("question-1")))
                .isInstanceOf(HttpServerErrorException.InternalServerError.class);

        server.verify();
    }
}
