package com.wedu.exam_creation.chapter.dto.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.chapter.domain.entity.*;
import com.wedu.exam_creation.common.dto.chapter.*;
import com.wedu.exam_creation.common.dto.chapter.response.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ChapterDTOMapperTest {
    private final ChapterDTOMapper mapper = Mappers.getMapper(ChapterDTOMapper.class);

    @Test
    @DisplayName("trả về các trường null khi chương chưa có thông tin tùy chọn")
    void test_entityToDTO_001() {
        assertThat(mapper.entityToDTO(new ChapterEntity()))
                .isEqualTo(new ChapterResponseDTO(null, null, null, null));
    }

    @Test
    @DisplayName("trả về tên và bankStats null khi DTO bài chưa có thông tin")
    void test_newLessonDTOToEntity_002() {
        assertThat(mapper.newLessonDTOToEntity(new NewLessonDataDTO()))
                .isEqualTo(new LessonDataEntity(null, null, null));
    }

    @Test
    @DisplayName("trả về bankStats null khi entity bài có thống kê null")
    void test_lessonEntityToDTO_003() {
        assertThat(mapper.lessonEntityToDTO(new LessonDataEntity("l1", null, null)))
                .isEqualTo(new LessonDataResponseDTO("l1", null, null));
    }

    @Test
    @DisplayName("trả về đầy đủ chương bài và bankStats khi map entity sang response")
    void test_entityToDTO_004() {
        BankStatEntity stat =
                new BankStatEntity(
                        "Dạng 1",
                        List.of("Nhận biết", "Thông hiểu"),
                        List.of("Yêu cầu 1", "Yêu cầu 2"),
                        "Đúng sai",
                        7);
        ChapterEntity entity =
                new ChapterEntity(
                        "c1",
                        "Vật lý",
                        "Chương 1",
                        List.of(new LessonDataEntity("l1", "Bài 1", List.of(stat))),
                        null,
                        null);
        ChapterResponseDTO expected =
                new ChapterResponseDTO(
                        "c1",
                        "Vật lý",
                        "Chương 1",
                        List.of(
                                new LessonDataResponseDTO(
                                        "l1",
                                        "Bài 1",
                                        List.of(
                                                new BankStatResponseDTO(
                                                        "Dạng 1",
                                                        List.of("Nhận biết", "Thông hiểu"),
                                                        List.of("Yêu cầu 1", "Yêu cầu 2"),
                                                        "Đúng sai",
                                                        7)))));
        assertThat(mapper.entityToDTO(entity)).isEqualTo(expected);
    }

    @Test
    @DisplayName("trả về null khi chapter entity đầu vào null")
    void test_entityToDTO_005() {
        assertThat(mapper.entityToDTO(null)).isNull();
    }

    @Test
    @DisplayName("trả về danh sách bài rỗng khi chương không có bài")
    void test_entityToDTO_006() {
        assertThat(mapper.entityToDTO(new ChapterEntity("c1", null, null, List.of(), null, null)))
                .isEqualTo(new ChapterResponseDTO("c1", null, null, List.of()));
    }

    @Test
    @DisplayName("trả về entity bài với ID null và bankStats đầy đủ khi map DTO tạo bài")
    void test_newLessonDTOToEntity_007() {
        NewBankStatDTO stat =
                new NewBankStatDTO(
                        "Dạng 1", List.of("Nhận biết"), List.of("Yêu cầu 1"), "Đúng sai", 3);
        LessonDataEntity expected =
                new LessonDataEntity(
                        null,
                        "Bài 1",
                        List.of(
                                new BankStatEntity(
                                        "Dạng 1",
                                        List.of("Nhận biết"),
                                        List.of("Yêu cầu 1"),
                                        "Đúng sai",
                                        3)));
        assertThat(mapper.newLessonDTOToEntity(new NewLessonDataDTO(List.of(stat), "Bài 1")))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("trả về null khi DTO bài đầu vào null")
    void test_newLessonDTOToEntity_008() {
        assertThat(mapper.newLessonDTOToEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về bankStats rỗng khi DTO bài không có thống kê")
    void test_newLessonDTOToEntity_009() {
        assertThat(mapper.newLessonDTOToEntity(new NewLessonDataDTO(List.of(), "Bài 1")))
                .isEqualTo(new LessonDataEntity(null, "Bài 1", List.of()));
    }

    @Test
    @DisplayName("trả về ID tên và bankStats đầy đủ khi map entity bài sang response")
    void test_lessonEntityToDTO_010() {
        LessonDataEntity entity =
                new LessonDataEntity(
                        "l1",
                        "Bài 1",
                        List.of(
                                new BankStatEntity(
                                        "Dạng 1",
                                        List.of("Thông hiểu"),
                                        List.of("Yêu cầu 1"),
                                        "Trả lời ngắn",
                                        2)));
        LessonDataResponseDTO expected =
                new LessonDataResponseDTO(
                        "l1",
                        "Bài 1",
                        List.of(
                                new BankStatResponseDTO(
                                        "Dạng 1",
                                        List.of("Thông hiểu"),
                                        List.of("Yêu cầu 1"),
                                        "Trả lời ngắn",
                                        2)));
        assertThat(mapper.lessonEntityToDTO(entity)).isEqualTo(expected);
    }

    @Test
    @DisplayName("trả về null khi entity bài đầu vào null")
    void test_lessonEntityToDTO_011() {
        assertThat(mapper.lessonEntityToDTO(null)).isNull();
    }

    @Test
    @DisplayName("trả về bankStats rỗng khi entity bài không có thống kê")
    void test_lessonEntityToDTO_012() {
        assertThat(mapper.lessonEntityToDTO(new LessonDataEntity("l1", "Bài 1", List.of())))
                .isEqualTo(new LessonDataResponseDTO("l1", "Bài 1", List.of()));
    }
}
