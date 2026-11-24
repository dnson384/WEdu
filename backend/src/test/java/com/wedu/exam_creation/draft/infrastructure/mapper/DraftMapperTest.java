package com.wedu.exam_creation.draft.infrastructure.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.common.dto.draft.response.*;
import com.wedu.exam_creation.draft.domain.entity.*;
import com.wedu.exam_creation.draft.infrastructure.document.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DraftMapperTest {
    private final DraftMapper mapper = Mappers.getMapper(DraftMapper.class);

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
    void test_toDocument_001() {

        DraftEntity entity = entity();
        DraftDocument result = mapper.toDocument(entity);
        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
        assertThat(result.getChapters()).isNotSameAs(entity.getChapters());
    }

    @Test
    @DisplayName("trả về null khi entity đầu vào null")
    void test_toDocument_002() {
        assertThat(mapper.toDocument(null)).isNull();
    }

    @Test
    @DisplayName("trả về entity giữ nguyên dữ liệu khi chuyển qua document và ngược lại")
    void test_toEntity_003() {

        DraftEntity entity = entity();
        assertThat(mapper.toEntity(mapper.toDocument(entity))).isEqualTo(entity);
    }

    @Test
    @DisplayName("trả về null khi dữ liệu đầu vào null")
    void test_toEntity_004() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về questionTypes rỗng và trường còn lại null khi entity chưa có chương")
    void test_toDocument_005() {

        DraftEntity entity = new DraftEntity();
        entity.setId("draft");
        DraftDocument expected =
                new DraftDocument("draft", null, null, null, List.of(), null, null, null);
        assertThat(mapper.toDocument(entity)).isEqualTo(expected);
    }
}
