package com.wedu.exam_creation.chapter.infrastructure.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.chapter.domain.entity.*;
import com.wedu.exam_creation.chapter.infrastructure.document.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ChapterMapperTest {
    private final ChapterMapper mapper = Mappers.getMapper(ChapterMapper.class);
    private final Instant created = Instant.parse("2026-11-19T00:00:00Z");
    private final Instant updated = Instant.parse("2026-11-20T00:00:00Z");

    @Test
    @DisplayName("trả về các trường null khi entity thiếu dữ liệu tùy chọn")
    void test_toDocument_004() {
        assertThat(mapper.toDocument(new ChapterEntity()))
                .isEqualTo(new ChapterDocument(null, null, null, null, null, null));
    }

    @Test
    @DisplayName("trả về các trường null khi document thiếu dữ liệu tùy chọn")
    void test_toEntity_004() {
        assertThat(mapper.toEntity(new ChapterDocument(null, null, null, null, null, null)))
                .isEqualTo(new ChapterEntity());
    }

    @Test
    @DisplayName("trả về document đầy đủ dữ liệu lồng nhau và timestamp khi map entity")
    void test_toDocument_001() {
        ChapterEntity entity =
                new ChapterEntity(
                        "c1",
                        "Vật lý",
                        "Chương 1",
                        List.of(
                                new LessonDataEntity(
                                        "l1",
                                        "Bài 1",
                                        List.of(
                                                new BankStatEntity(
                                                        "Dạng 1",
                                                        List.of("Nhận biết"),
                                                        List.of("Yêu cầu 1"),
                                                        "Đúng sai",
                                                        2)))),
                        created,
                        updated);
        ChapterDocument result = mapper.toDocument(entity);
        assertThat(result).usingRecursiveComparison().isEqualTo(entity);
        assertThat(result.getLessons()).isNotSameAs(entity.getLessons());
        assertThat(result.getLessons().get(0).getBankStats())
                .isNotSameAs(entity.getLessons().get(0).getBankStats());
    }

    @Test
    @DisplayName("trả về null khi entity đầu vào null")
    void test_toDocument_002() {
        assertThat(mapper.toDocument(null)).isNull();
    }

    @Test
    @DisplayName("trả về danh sách bài rỗng khi entity không có bài")
    void test_toDocument_003() {
        assertThat(mapper.toDocument(new ChapterEntity("c1", null, null, List.of(), null, null)))
                .isEqualTo(new ChapterDocument("c1", null, null, List.of(), null, null));
    }

    @Test
    @DisplayName("trả về entity đầy đủ dữ liệu lồng nhau và timestamp khi map document")
    void test_toEntity_001() {
        ChapterDocument document =
                new ChapterDocument(
                        "c1",
                        "Vật lý",
                        "Chương 1",
                        List.of(
                                new LessonDataDocument(
                                        "l1",
                                        "Bài 1",
                                        List.of(
                                                new BankStatDocument(
                                                        "Dạng 1",
                                                        List.of("Nhận biết"),
                                                        List.of("Yêu cầu 1"),
                                                        "Đúng sai",
                                                        2)))),
                        created,
                        updated);
        ChapterEntity result = mapper.toEntity(document);
        assertThat(result).usingRecursiveComparison().isEqualTo(document);
        assertThat(result.getLessons()).isNotSameAs(document.getLessons());
    }

    @Test
    @DisplayName("trả về null khi document đầu vào null")
    void test_toEntity_002() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về danh sách bài rỗng khi document không có bài")
    void test_toEntity_003() {
        assertThat(mapper.toEntity(new ChapterDocument("c1", null, null, List.of(), null, null)))
                .isEqualTo(new ChapterEntity("c1", null, null, List.of(), null, null));
    }
}
