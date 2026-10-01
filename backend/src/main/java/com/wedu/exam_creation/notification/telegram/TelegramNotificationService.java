package com.wedu.exam_creation.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class TelegramNotificationService {
    private final RestTemplate restTemplate = new RestTemplate();

    private final String questionBotToken;
    private final String s3BotToken;
    private final String chatId;

    public TelegramNotificationService(
            @Value("${telegram.bot.question.token}") String questionBotToken,
            @Value("${telegram.bot.s3.token}") String s3BotToken,
            @Value("${telegram.chat.id}") String chatId
    ) {
        this.questionBotToken = questionBotToken;
        this.s3BotToken = s3BotToken;
        this.chatId = chatId;
    }

    // PENDING DELETE IMAGE
    public void notifyFailedDeleteImage(String s3Key) {
        if (s3Key.trim().isEmpty()) return;

        String message = buildS3Message(s3Key);
        String url = "https://api.telegram.org/bot" + s3BotToken + "/sendMessage";

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "Markdown"
        );

        restTemplate.postForEntity(url, body, String.class);
    }

    private String buildS3Message(String s3Key) {
        return "📋 *Ảnh bị xóa lỗi:*\n" + s3Key;
    }

    // PENDING REVIEW QUESTIONS
    public void notifyPendingReview(List<String> questionIds) {
        if (questionIds.isEmpty()) return;

        String message = buildQuestionMessage(questionIds);
        String url = "https://api.telegram.org/bot" + questionBotToken + "/sendMessage";

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "Markdown"
        );

        restTemplate.postForEntity(url, body, String.class);
    }

    private String buildQuestionMessage(List<String> ids) {
        StringBuilder sb = new StringBuilder("📋 *Câu hỏi mới từ AI cần kiểm duyệt:*\n\n");
        ids.forEach(id -> sb.append("• `").append(id).append("`\n"));
        return sb.toString();
    }

}
