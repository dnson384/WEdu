package com.wedu.exam_creation.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class TelegramNotificationService {
    private final RestTemplate restTemplate = new RestTemplate();

    private final String botToken;

    private final String chatId;

    public TelegramNotificationService(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.chat.id}") String chatId
    ) {
        this.botToken = botToken;
        this.chatId = chatId;
    }


    public void notifyPendingReview(List<String> questionIds) {
        if (questionIds.isEmpty()) return;

        String message = buildMessage(questionIds);
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "Markdown"
        );

        restTemplate.postForEntity(url, body, String.class);
    }

    private String buildMessage(List<String> ids) {
        StringBuilder sb = new StringBuilder("📋 *Câu hỏi mới từ AI cần kiểm duyệt:*\n\n");
        ids.forEach(id -> sb.append("• `").append(id).append("`\n"));
        return sb.toString();
    }

}
