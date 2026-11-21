package com.wedu.exam_creation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.common.dto.ai.response.AIListQuestionsResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JacksonConfigTest {
    private final ObjectMapper mapper = new JacksonConfig().objectMapper();

    @Test
    @DisplayName("trả về danh sách câu hỏi có mã chương và mã bài khi đọc phản hồi JSON hợp lệ")
    void test_objectMapper_001() throws JsonProcessingException {
        String response =
                """
                {"questions":[{"chapterId":"chapter-1","lessonId":"lesson-1"}]}
                """;

        AIListQuestionsResponseDTO result =
                mapper.readValue(response, AIListQuestionsResponseDTO.class);

        assertThat(result.getQuestions())
                .singleElement()
                .satisfies(
                        question -> {
                            assertThat(question.getChapterId()).isEqualTo("chapter-1");
                            assertThat(question.getLessonId()).isEqualTo("lesson-1");
                        });
    }

    @Test
    @DisplayName("trả về JsonProcessingException khi mapper đọc JSON bị cắt giữa chừng")
    void test_objectMapper_002() {
        assertThatThrownBy(
                        () ->
                                mapper.readValue(
                                        "{\"questions\":[", AIListQuestionsResponseDTO.class))
                .isInstanceOf(JsonProcessingException.class);
    }
}
