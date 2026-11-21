package com.wedu.exam_creation.passwordResetToken.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.passwordResetToken.domain.entity.PasswordResetTokenEntity;
import com.wedu.exam_creation.passwordResetToken.infrastructure.document.PasswordResetTokenDocument;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PasswordResetTokenMapperTest {
    private static final Instant CREATED_AT = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant EXPIRY = Instant.parse("2026-11-21T00:15:00Z");
    private final PasswordResetTokenMapper mapper =
            Mappers.getMapper(PasswordResetTokenMapper.class);

    @Test
    @DisplayName("trả về entity giữ đủ trường khi đọc document của token đã dùng")
    void test_toEntity_001() {
        PasswordResetTokenDocument document =
                new PasswordResetTokenDocument("id", "hash", "user", EXPIRY, true, CREATED_AT);

        assertThat(mapper.toEntity(document))
                .isEqualTo(
                        new PasswordResetTokenEntity(
                                "id", "hash", "user", EXPIRY, true, CREATED_AT));
    }

    @Test
    @DisplayName("trả về null khi document nguồn không tồn tại")
    void test_toEntity_002() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về document giữ đủ trường khi ghi entity của token chưa dùng")
    void test_toDocument_003() {
        PasswordResetTokenEntity entity =
                new PasswordResetTokenEntity("id", "hash", "user", EXPIRY, false, CREATED_AT);

        assertThat(mapper.toDocument(entity))
                .isEqualTo(
                        new PasswordResetTokenDocument(
                                "id", "hash", "user", EXPIRY, false, CREATED_AT));
    }

    @Test
    @DisplayName("trả về null khi entity nguồn không tồn tại")
    void test_toDocument_004() {
        assertThat(mapper.toDocument(null)).isNull();
    }
}
