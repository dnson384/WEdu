package com.wedu.exam_creation.importer.infrastructure.pandoc.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.importer.dto.parsed.NewQuestionImporterDTO;
import com.wedu.exam_creation.importer.dto.parsed.QuestionContentImporterDTO;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ParaExtractorTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và giữ chữ, xuống dòng, biến toán khi đoạn câu hỏi có nhiều loại token")
    void test_execute_001() {
        NewQuestionImporterDTO question = new NewQuestionImporterDTO();
        QuestionContentImporterDTO content = question.getQuestion();
        content.setTemplate("Existing: ");
        content.getVariables().getMath().put("math_0", "x");
        JsonNode tokens =
                json.readTree(
                        """
                [{"t":"Str","c":"value"},{"t":"Space"},{"t":"Math","c":[{}, "y"]},
                 {"t":"LineBreak"},{"t":"Math","c":[{}, "z"]},{"t":"Unsupported"}]
                """);

        ParaExtractor.execute(tokens, question, Map.of());

        assertThat(content.getTemplate()).isEqualTo("Existing: value <math_1> \n<math_2>");
        assertThat(content.getVariables().getMath())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("math_0", "x", "math_1", "y", "math_2", "z"));
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và liên kết placeholder với S3 key khi ảnh đã được upload")
    void test_execute_002() {
        NewQuestionImporterDTO question = new NewQuestionImporterDTO();
        QuestionContentImporterDTO content = question.getQuestion();
        JsonNode image =
                json.readTree(
                        """
                [{"t":"Image","c":[[],[],["nested/media/diagram.png",""]]}]
                """);

        ParaExtractor.execute(image, question, Map.of("media/diagram.png", "media/stored.png"));

        assertThat(content.getTemplate()).isEqualTo(" \n<img_0>\n ");
        assertThat(content.getVariables().getImage())
                .containsExactlyEntriesOf(Map.of("img_0", "media/stored.png"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"[]", "{}"})
    @DisplayName(
            "trả về không có ngoại lệ và giữ nguyên nội dung khi AST thiếu hoặc không có mảng token")
    void test_execute_003(String input) {
        NewQuestionImporterDTO question = new NewQuestionImporterDTO();
        QuestionContentImporterDTO content = question.getQuestion();
        content.setTemplate("Original");
        JsonNode tokens = input == null ? null : json.readTree(input);

        ParaExtractor.execute(tokens, question, Map.of());

        assertThat(content.getTemplate()).isEqualTo("Original");
        assertThat(content.getVariables().getMath()).isEmpty();
        assertThat(content.getVariables().getImage()).isEmpty();
    }
}
