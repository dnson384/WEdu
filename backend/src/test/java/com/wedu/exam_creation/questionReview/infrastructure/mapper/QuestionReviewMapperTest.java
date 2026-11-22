package com.wedu.exam_creation.questionReview.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.questionReview.domain.entity.ContentRegeneratedEntity;
import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.domain.entity.RegeneratedQuestionEntity;
import com.wedu.exam_creation.questionReview.domain.entity.VariablesRegeneratedEntity;
import com.wedu.exam_creation.questionReview.infrastructure.document.QuestionReviewDocument;
import com.wedu.exam_creation.questionReview.infrastructure.document.RegeneratedQuestionDocument;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionReviewMapperTest {
    private static final Instant VERSION = Instant.parse("2026-11-22T00:00:00Z");
    private static final String TEMPLATE = "<math_0> <img_0>";
    private final Map<String, String> math = Map.of("math_0", "x+1");
    private final Map<String, String> images = Map.of("img_0", "media/review.png");
    private final QuestionReviewMapper mapper = Mappers.getMapper(QuestionReviewMapper.class);

    @Test
    @DisplayName(
            "trả về snapshot có metadata và giữ placeholder khi ánh xạ DTO có câu và phương án")
    void test_toSnapshot_001() {
        RegeneratedQuestionEntity snapshot = mapper.toSnapshot(dto());

        assertThat(snapshot).isEqualTo(snapshot());
    }

    @Test
    @DisplayName("trả về DTO có metadata và nội dung lồng nhau khi đọc snapshot")
    void test_toDTO_002() {
        assertThat(mapper.toDTO(snapshot())).isEqualTo(dto());
    }

    @Test
    @DisplayName("trả về document giữ revision, lượt sinh lại và dữ liệu lồng nhau khi ghi review")
    void test_toDocument_003() {
        RegeneratedQuestionEntity snapshot = snapshot();
        snapshot.setStatus("PENDING_REVIEW");
        snapshot.setCreatedAt(VERSION);
        snapshot.setUpdatedAt(VERSION);
        QuestionReviewEntity review =
                QuestionReviewEntity.builder()
                        .id("id")
                        .revision(3)
                        .regenerationCount(2)
                        .regeneratedQuestions(List.of(snapshot))
                        .build();

        QuestionReviewDocument document = mapper.toDocument(review);

        assertThat(document.getId()).isEqualTo("id");
        assertThat(document.getRevision()).isEqualTo(3);
        assertThat(document.getRegenerationCount()).isEqualTo(2);
        assertThat(document.getRegeneratedQuestions())
                .singleElement()
                .satisfies(
                        stored -> {
                            assertThat(stored.getStatus()).isEqualTo("PENDING_REVIEW");
                            assertThat(stored.getCreatedAt()).isEqualTo(VERSION);
                            assertThat(stored.getUpdatedAt()).isEqualTo(VERSION);
                            assertThat(stored.getQuestion().getTemplate()).isEqualTo(TEMPLATE);
                            assertThat(stored.getQuestion().getVariables().getMath())
                                    .isEqualTo(math);
                            assertThat(stored.getOptions())
                                    .singleElement()
                                    .satisfies(
                                            option ->
                                                    assertThat(option.getVariables().getImage())
                                                            .isEqualTo(images));
                        });
    }

    @Test
    @DisplayName("trả về entity giữ trạng thái và thời điểm snapshot khi đọc document")
    void test_toEntity_004() {
        RegeneratedQuestionDocument stored =
                RegeneratedQuestionDocument.builder()
                        .id("id")
                        .status("PENDING_REVIEW")
                        .createdAt(VERSION)
                        .updatedAt(VERSION)
                        .build();
        QuestionReviewDocument document =
                QuestionReviewDocument.builder()
                        .id("id")
                        .revision(3)
                        .regenerationCount(2)
                        .regeneratedQuestions(List.of(stored))
                        .build();

        QuestionReviewEntity review = mapper.toEntity(document);

        assertThat(review.getId()).isEqualTo("id");
        assertThat(review.getRevision()).isEqualTo(3);
        assertThat(review.getRegenerationCount()).isEqualTo(2);
        assertThat(review.getRegeneratedQuestions())
                .singleElement()
                .satisfies(
                        snapshot -> {
                            assertThat(snapshot.getId()).isEqualTo("id");
                            assertThat(snapshot.getStatus()).isEqualTo("PENDING_REVIEW");
                            assertThat(snapshot.getCreatedAt()).isEqualTo(VERSION);
                            assertThat(snapshot.getUpdatedAt()).isEqualTo(VERSION);
                        });
    }

    @Test
    @DisplayName("trả về null khi document review nguồn không tồn tại")
    void test_toEntity_005() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    private RegeneratedQuestionEntity snapshot() {
        ContentRegeneratedEntity content =
                new ContentRegeneratedEntity(
                        TEMPLATE, new VariablesRegeneratedEntity(math, images));
        return RegeneratedQuestionEntity.builder()
                .id("id")
                .chapterId("chapter")
                .lessonId("lesson")
                .exerciseType("exercise")
                .difficultyLevel("difficulty")
                .questionType("type")
                .learningOutcomes(List.of("outcome"))
                .question(content)
                .options(List.of(content))
                .build();
    }

    private QuestionDTO dto() {
        ContentDTO content = new ContentDTO(TEMPLATE, new VariablesDTO(math, images));
        return new QuestionDTO(
                "id",
                "chapter",
                "lesson",
                "exercise",
                "difficulty",
                List.of("outcome"),
                "type",
                content,
                List.of(content));
    }
}
