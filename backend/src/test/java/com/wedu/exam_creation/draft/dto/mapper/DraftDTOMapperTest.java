package com.wedu.exam_creation.draft.dto.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.common.dto.draft.response.*;
import com.wedu.exam_creation.draft.domain.entity.*;
import com.wedu.exam_creation.draft.infrastructure.document.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DraftDTOMapperTest {
    private final DraftDTOMapper mapper = Mappers.getMapper(DraftDTOMapper.class);

    private DraftEntity entity() {
        return new DraftEntity(
                "draft",
                "owner",
                "Đề vật lý",
                3,
                List.of("Nhiều lựa chọn"),
                List.of(
                        new ChapterDraftEntity(
                                "chapter",
                                "Chương",
                                List.of(
                                        new LessonDraftEntity(
                                                "lesson",
                                                "Bài",
                                                List.of(
                                                        new MatrixItemEntity(
                                                                "Nhiều lựa chọn", "Nhận biết", 3)),
                                                List.of(
                                                        new MatrixDetailItemEntity(
                                                                "Dạng",
                                                                "Nhận biết",
                                                                "Kiến thức",
                                                                "Nhiều lựa chọn",
                                                                3)))))),
                Instant.parse("2026-11-20T00:00:00Z"),
                Instant.parse("2026-11-20T01:00:00Z"));
    }

    @Test
    @DisplayName("trả về đầy đủ dữ liệu lồng nhau và thời gian khi chuyển entity")
    void test_toDTO_001() {

        DraftEntity entity = entity();
        DraftDTO result = mapper.toDTO(entity);
        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
        assertThat(result.getChapters()).isNotSameAs(entity.getChapters());
    }

    @Test
    @DisplayName("trả về null khi entity đầu vào null")
    void test_toDTO_002() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    @Test
    @DisplayName("trả về entity giữ nguyên dữ liệu khi chuyển qua DTO và ngược lại")
    void test_toEntity_003() {

        DraftEntity entity = entity();
        assertThat(mapper.toEntity(mapper.toDTO(entity))).isEqualTo(entity);
    }

    @Test
    @DisplayName("trả về null khi dữ liệu đầu vào null")
    void test_toEntity_004() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về trường tùy chọn null được giữ nguyên khi entity chưa có chương")
    void test_toDTO_005() {

        DraftEntity entity = new DraftEntity();
        entity.setId("draft");
        assertThat(mapper.toDTO(entity)).usingRecursiveComparison().isEqualTo(entity);
    }

    @Test
    @DisplayName("trả về đủ năm trường đặc tả khi DTO hợp lệ")
    void test_detailDTOToEntity_006() {

        MatrixDetailItemDTO dto =
                new MatrixDetailItemDTO("Dạng", "Thông hiểu", "Kiến thức", "Đúng sai", 0);
        assertThat(mapper.detailDTOToEntity(dto))
                .isEqualTo(
                        new MatrixDetailItemEntity(
                                "Dạng", "Thông hiểu", "Kiến thức", "Đúng sai", 0));
    }

    @Test
    @DisplayName("trả về null khi DTO đặc tả null")
    void test_detailDTOToEntity_007() {
        assertThat(mapper.detailDTOToEntity(null)).isNull();
    }
}
