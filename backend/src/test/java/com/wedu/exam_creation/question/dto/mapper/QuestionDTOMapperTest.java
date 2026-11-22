package com.wedu.exam_creation.question.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.question.domain.entity.ContentEntity;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.entity.VariablesEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionDTOMapperTest {
    private static final Instant VERSION = Instant.parse("2026-11-22T00:00:00Z");
    private final QuestionDTOMapper mapper = Mappers.getMapper(QuestionDTOMapper.class);
    private final ContentDTO content =
            new ContentDTO(
                    "<math_0> <img_0>",
                    new VariablesDTO(Map.of("math_0", "x"), Map.of("img_0", "media/image.png")));
    private final ContentEntity mappedContent =
            new ContentEntity(
                    "<math_0> <img_0>",
                    new VariablesEntity(Map.of("math_0", "x"), Map.of("img_0", "media/image.png")));

    @Test
    @DisplayName(
            "trả về nội dung domain giữ nguyên placeholder, công thức và S3 key khi DTO có biến")
    void test_contentDTOToEntity_001() {
        assertThat(mapper.contentDTOToEntity(content)).isEqualTo(mappedContent);
    }

    @Test
    @DisplayName("trả về null khi DTO nội dung nguồn không tồn tại")
    void test_contentDTOToEntity_002() {
        assertThat(mapper.contentDTOToEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về DTO có trường kế thừa và thời điểm cập nhật khi ánh xạ câu chờ duyệt")
    void test_entityToPendingDTO_003() {
        PendingQuestionDTO result = mapper.entityToPendingDTO(entity());

        assertThat(result.getId()).isEqualTo("id");
        assertThat(result.getChapterId()).isEqualTo("chapter");
        assertThat(result.getLessonId()).isEqualTo("lesson");
        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(result.getUpdatedAt()).isEqualTo(VERSION);
        assertThat(result.getQuestion()).isEqualTo(content);
        assertThat(result.getOptions()).containsExactly(content);
    }

    @Test
    @DisplayName("trả về entity có metadata và phương án khi ánh xạ câu mới")
    void test_newQuestionDTOToEntity_004() {
        NewQuestionDTO source =
                NewQuestionDTO.builder()
                        .id("id")
                        .chapterId("chapter")
                        .lessonId("lesson")
                        .question(content)
                        .options(List.of(content))
                        .build();

        QuestionEntity result = mapper.newQuestionDTOToEntity(source);

        assertThat(result.getChapterId()).isEqualTo("chapter");
        assertThat(result.getLessonId()).isEqualTo("lesson");
        assertThat(result.getQuestion()).isEqualTo(mappedContent);
        assertThat(result.getOptions()).containsExactly(mappedContent);
        assertThat(result.getStatus()).isNull();
    }

    @Test
    @DisplayName("trả về DTO giữ metadata và nội dung khi ánh xạ entity vào đề")
    void test_entityToCommonDTO_005() {
        QuestionDTO expected =
                QuestionDTO.builder()
                        .id("id")
                        .chapterId("chapter")
                        .lessonId("lesson")
                        .question(content)
                        .options(List.of(content))
                        .build();

        assertThat(mapper.entityToCommonDTO(entity())).isEqualTo(expected);
    }

    private QuestionEntity entity() {
        return QuestionEntity.builder()
                .id("id")
                .chapterId("chapter")
                .lessonId("lesson")
                .status("PENDING_REVIEW")
                .question(mappedContent)
                .options(List.of(mappedContent))
                .updatedAt(VERSION)
                .build();
    }
}
