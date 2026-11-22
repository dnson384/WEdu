package com.wedu.exam_creation.question.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.question.domain.entity.ContentEntity;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.entity.VariablesEntity;
import com.wedu.exam_creation.question.infrastructure.document.ContentDocument;
import com.wedu.exam_creation.question.infrastructure.document.QuestionDocument;
import com.wedu.exam_creation.question.infrastructure.document.VariablesDocument;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class QuestionMapperTest {
    private static final Instant VERSION = Instant.parse("2026-11-22T00:00:00Z");
    private final QuestionMapper mapper = Mappers.getMapper(QuestionMapper.class);
    private final ContentEntity entityContent =
            new ContentEntity(
                    "<img_0>", new VariablesEntity(Map.of(), Map.of("img_0", "media/diagram.png")));
    private final ContentDocument documentContent =
            new ContentDocument(
                    "<img_0>",
                    new VariablesDocument(Map.of(), Map.of("img_0", "media/diagram.png")));

    @Test
    @DisplayName("trả về document giữ trạng thái, nội dung và thời điểm khi ghi câu hỏi chờ duyệt")
    void test_toDocument_001() {
        QuestionEntity entity =
                QuestionEntity.builder()
                        .id("id")
                        .chapterId("chapter")
                        .lessonId("lesson")
                        .status("PENDING_REVIEW")
                        .question(entityContent)
                        .options(List.of(entityContent))
                        .createdAt(VERSION)
                        .updatedAt(VERSION)
                        .build();

        assertThat(mapper.toDocument(entity))
                .isEqualTo(
                        QuestionDocument.builder()
                                .id("id")
                                .chapterId("chapter")
                                .lessonId("lesson")
                                .status("PENDING_REVIEW")
                                .question(documentContent)
                                .options(List.of(documentContent))
                                .createdAt(VERSION)
                                .updatedAt(VERSION)
                                .build());
    }

    @Test
    @DisplayName("trả về entity giữ nội dung và status null khi đọc câu hỏi legacy")
    void test_toEntity_002() {
        QuestionDocument document =
                QuestionDocument.builder()
                        .id("legacy")
                        .question(documentContent)
                        .options(List.of(documentContent))
                        .updatedAt(VERSION)
                        .build();

        assertThat(mapper.toEntity(document))
                .isEqualTo(
                        QuestionEntity.builder()
                                .id("legacy")
                                .learningOutcomes(List.of())
                                .question(entityContent)
                                .options(List.of(entityContent))
                                .updatedAt(VERSION)
                                .build());
    }

    @Test
    @DisplayName("trả về null khi entity nguồn không tồn tại")
    void test_toDocument_003() {
        assertThat(mapper.toDocument(null)).isNull();
    }

    @Test
    @DisplayName("trả về null khi document nguồn không tồn tại")
    void test_toEntity_004() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
