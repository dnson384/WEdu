package com.wedu.exam_creation.exam.infrastructure.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.exam.domain.entity.*;
import com.wedu.exam_creation.exam.infrastructure.document.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;

class ExamMapperTest {
    private final ExamMapper mapper = Mappers.getMapper(ExamMapper.class);
    private final Instant created = Instant.parse("2026-11-19T00:00:00Z");
    private final Instant updated = Instant.parse("2026-11-20T00:00:00Z");

    @Test
    @DisplayName(
            "trả về document giữ đủ snapshot ID câu hỏi và timestamps khi map dữ liệu lồng nhau")
    void test_entityToDocument_001() {
        ExamEntity source =
                new ExamEntity(
                        "exam",
                        "owner",
                        "Đề",
                        List.of(
                                new ChapterExamEntity(
                                        List.of(
                                                new LessonExamEntity(
                                                        "l1",
                                                        "Bài 1",
                                                        List.of(
                                                                new ExamMatrixItemEntity(
                                                                        "Đúng sai",
                                                                        "Thông hiểu",
                                                                        0),
                                                                new ExamMatrixItemEntity(
                                                                        "Nhiều lựa chọn",
                                                                        "Nhận biết",
                                                                        3)),
                                                        List.of(
                                                                new ExamMatrixDetailItemEntity(
                                                                        "Dạng",
                                                                        "Nhận biết",
                                                                        "Yêu cầu",
                                                                        "Nhiều lựa chọn",
                                                                        3)))),
                                        "c1",
                                        "Chương 1"),
                                new ChapterExamEntity(List.of(), "c2", "Chương 2")),
                        List.of(
                                new QuestionExamEntity(
                                        "Nhiều lựa chọn", "Nhận biết", List.of("q1", "q2")),
                                new QuestionExamEntity("Đúng sai", "Thông hiểu", List.of())),
                        created,
                        updated);
        ExamDocument result = mapper.entityToDocument(source);
        assertThat(result).usingRecursiveComparison().isEqualTo(source);
        assertThat(result.getChapters()).isNotSameAs(source.getChapters());
        assertThat(result.getQuestions().get(0).getQuestionIds())
                .isNotSameAs(source.getQuestions().get(0).getQuestionIds());
        source.getChapters().get(0).getLessons().get(0).getMatrix().get(0).setSelectedCount(99);
        source.getChapters()
                .get(0)
                .getLessons()
                .get(0)
                .getMatrixDetails()
                .get(0)
                .setSelectedCount(99);
        assertThat(
                        result.getChapters()
                                .get(0)
                                .getLessons()
                                .get(0)
                                .getMatrix()
                                .get(0)
                                .getSelectedCount())
                .isZero();
        assertThat(
                        result.getChapters()
                                .get(0)
                                .getLessons()
                                .get(0)
                                .getMatrixDetails()
                                .get(0)
                                .getSelectedCount())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("trả về null khi entityToDocument nhận đầu vào null")
    void test_entityToDocument_002() {
        assertThat(mapper.entityToDocument(null)).isNull();
    }

    @Test
    @DisplayName("trả về document giữ hai danh sách rỗng khi đề không có chương và nhóm câu")
    void test_entityToDocument_003() {
        assertThat(
                        mapper.entityToDocument(
                                new ExamEntity(
                                        "exam", "owner", "Đề", List.of(), List.of(), null, null)))
                .isEqualTo(
                        new ExamDocument("exam", "owner", "Đề", List.of(), List.of(), null, null));
    }

    @Test
    @DisplayName("trả về document giữ các trường null khi đầu vào thiếu thông tin tùy chọn")
    void test_entityToDocument_004() {
        assertThat(mapper.entityToDocument(new ExamEntity())).isEqualTo(new ExamDocument());
    }

    @Test
    @DisplayName("trả về entity giữ đủ snapshot ID câu hỏi và timestamps khi map dữ liệu lồng nhau")
    void test_docToEntity_001() {
        ExamDocument source =
                new ExamDocument(
                        "exam",
                        "owner",
                        "Đề",
                        List.of(
                                new ChapterExamDocument(
                                        "c1",
                                        "Chương 1",
                                        List.of(
                                                new LessonExamDocument(
                                                        "l1",
                                                        "Bài 1",
                                                        List.of(
                                                                new ExamMatrixItemDocument(
                                                                        "Đúng sai",
                                                                        "Thông hiểu",
                                                                        0),
                                                                new ExamMatrixItemDocument(
                                                                        "Nhiều lựa chọn",
                                                                        "Nhận biết",
                                                                        3)),
                                                        List.of(
                                                                new ExamMatrixDetailItemDocument(
                                                                        "Dạng",
                                                                        "Nhận biết",
                                                                        "Yêu cầu",
                                                                        "Nhiều lựa chọn",
                                                                        3))))),
                                new ChapterExamDocument("c2", "Chương 2", List.of())),
                        List.of(
                                new QuestionExamDocument(
                                        "Nhiều lựa chọn", "Nhận biết", List.of("q1", "q2")),
                                new QuestionExamDocument("Đúng sai", "Thông hiểu", List.of())),
                        created,
                        updated);
        ExamEntity result = mapper.docToEntity(source);
        assertThat(result).usingRecursiveComparison().isEqualTo(source);
        assertThat(result.getChapters()).isNotSameAs(source.getChapters());
        assertThat(result.getQuestions().get(0).getQuestionIds())
                .isNotSameAs(source.getQuestions().get(0).getQuestionIds());
        source.getChapters().get(0).getLessons().get(0).getMatrix().get(0).setSelectedCount(99);
        source.getChapters()
                .get(0)
                .getLessons()
                .get(0)
                .getMatrixDetails()
                .get(0)
                .setSelectedCount(99);
        assertThat(
                        result.getChapters()
                                .get(0)
                                .getLessons()
                                .get(0)
                                .getMatrix()
                                .get(0)
                                .getSelectedCount())
                .isZero();
        assertThat(
                        result.getChapters()
                                .get(0)
                                .getLessons()
                                .get(0)
                                .getMatrixDetails()
                                .get(0)
                                .getSelectedCount())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("trả về null khi docToEntity nhận đầu vào null")
    void test_docToEntity_002() {
        assertThat(mapper.docToEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về entity giữ hai danh sách rỗng khi đề không có chương và nhóm câu")
    void test_docToEntity_003() {
        assertThat(
                        mapper.docToEntity(
                                new ExamDocument(
                                        "exam", "owner", "Đề", List.of(), List.of(), null, null)))
                .isEqualTo(new ExamEntity("exam", "owner", "Đề", List.of(), List.of(), null, null));
    }

    @Test
    @DisplayName("trả về entity giữ các trường null khi đầu vào thiếu thông tin tùy chọn")
    void test_docToEntity_004() {
        assertThat(mapper.docToEntity(new ExamDocument())).isEqualTo(new ExamEntity());
    }
}
