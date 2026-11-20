package com.wedu.exam_creation.exam.dto.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.common.dto.draft.response.*;
import com.wedu.exam_creation.exam.domain.entity.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;

class ExamCommonDraftDTOMapperTest {
    private final ExamCommonDraftDTOMapper mapper =
            Mappers.getMapper(ExamCommonDraftDTOMapper.class);

    @Test
    @DisplayName("trả về snapshot bài đầy đủ đúng thứ tự và giữ số lượng 0 khi map nháp sang đề")
    void test_lessonDraftDTOToLessonExamEntity_001() {
        LessonDraftDTO source =
                new LessonDraftDTO(
                        "l",
                        "Bài",
                        List.of(
                                new MatrixItemDTO("Đúng sai", "Thông hiểu", 0),
                                new MatrixItemDTO("Nhiều lựa chọn", "Nhận biết", 3)),
                        List.of(
                                new MatrixDetailItemDTO(
                                        "Dạng 1", "Thông hiểu", "Yêu cầu 1", "Đúng sai", 0),
                                new MatrixDetailItemDTO(
                                        "Dạng 2", "Nhận biết", "Yêu cầu 2", "Nhiều lựa chọn", 3)));
        LessonExamEntity result = mapper.lessonDraftDTOToLessonExamEntity(source);
        assertThat(result)
                .isEqualTo(
                        new LessonExamEntity(
                                "l",
                                "Bài",
                                List.of(
                                        new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 0),
                                        new ExamMatrixItemEntity("Nhiều lựa chọn", "Nhận biết", 3)),
                                List.of(
                                        new ExamMatrixDetailItemEntity(
                                                "Dạng 1", "Thông hiểu", "Yêu cầu 1", "Đúng sai", 0),
                                        new ExamMatrixDetailItemEntity(
                                                "Dạng 2",
                                                "Nhận biết",
                                                "Yêu cầu 2",
                                                "Nhiều lựa chọn",
                                                3))));
        source.getMatrix().get(0).setSelectedCount(99);
        source.getMatrixDetails().get(0).setSelectedCount(99);
        assertThat(result.getMatrix().get(0).getSelectedCount()).isZero();
        assertThat(result.getMatrixDetails().get(0).getSelectedCount()).isZero();
    }

    @Test
    @DisplayName("trả về null khi DTO bài đầu vào null")
    void test_lessonDraftDTOToLessonExamEntity_002() {
        assertThat(mapper.lessonDraftDTOToLessonExamEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về hai danh sách rỗng khi bài có ma trận và đặc tả rỗng")
    void test_lessonDraftDTOToLessonExamEntity_003() {
        assertThat(
                        mapper.lessonDraftDTOToLessonExamEntity(
                                new LessonDraftDTO("l", "Bài", List.of(), List.of())))
                .isEqualTo(new LessonExamEntity("l", "Bài", List.of(), List.of()));
    }

    @Test
    @DisplayName("trả về các trường null khi DTO bài thiếu dữ liệu tùy chọn")
    void test_lessonDraftDTOToLessonExamEntity_004() {
        assertThat(mapper.lessonDraftDTOToLessonExamEntity(new LessonDraftDTO()))
                .isEqualTo(new LessonExamEntity());
    }
}
