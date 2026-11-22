package com.wedu.exam_creation.user.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.infrastructure.document.UserDocument;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-02-01T00:00:00Z");

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName(
            "trả về entity giữ liên kết Google, thông tin đăng nhập và quyền PLUS khi đọc document")
    void test_toEntity_001() {
        UserDocument document =
                new UserDocument(
                        "teacher-1",
                        "teacher@example.com",
                        "hash",
                        "Giáo viên",
                        "ROLE_TEACHER",
                        "MIXED",
                        "avatars/teacher.png",
                        true,
                        "PLUS",
                        CREATED,
                        UPDATED,
                        EXPIRES,
                        "google-subject");

        UserEntity result = mapper.toEntity(document);

        assertThat(result).usingRecursiveComparison().isEqualTo(document);
    }

    @Test
    @DisplayName("trả về null khi không có document người dùng để ánh xạ")
    void test_toEntity_002() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName(
            "trả về document giữ liên kết Google, thông tin đăng nhập và quyền PLUS khi ánh xạ entity")
    void test_toDocument_003() {
        UserEntity user =
                new UserEntity(
                        "teacher-1",
                        "teacher@example.com",
                        "hash",
                        "Giáo viên",
                        "ROLE_TEACHER",
                        "MIXED",
                        "avatars/teacher.png",
                        true,
                        "PLUS",
                        CREATED,
                        UPDATED,
                        EXPIRES,
                        "google-subject");

        UserDocument result = mapper.toDocument(user);

        assertThat(result).usingRecursiveComparison().isEqualTo(user);
    }

    @Test
    @DisplayName("trả về null khi không có entity người dùng để ánh xạ")
    void test_toDocument_004() {
        assertThat(mapper.toDocument(null)).isNull();
    }
}
