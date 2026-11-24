package com.wedu.exam_creation.refreshToken.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import com.wedu.exam_creation.refreshToken.infrastructure.document.RefreshTokenDocument;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RefreshTokenMapperTest {
    private static final Instant ISSUED_AT = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-29T00:00:00Z");
    private final RefreshTokenMapper mapper = Mappers.getMapper(RefreshTokenMapper.class);

    @Test
    @DisplayName("trả về document giữ đủ metadata khi ánh xạ refresh token entity")
    void test_toDocument_001() {
        RefreshTokenEntity entity =
                new RefreshTokenEntity("id", "jti", "user", EXPIRES_AT, ISSUED_AT);

        assertThat(mapper.toDocument(entity))
                .isEqualTo(new RefreshTokenDocument("id", "jti", "user", EXPIRES_AT, ISSUED_AT));
    }

    @Test
    @DisplayName("trả về null khi entity nguồn không tồn tại")
    void test_toDocument_002() {
        assertThat(mapper.toDocument(null)).isNull();
    }

    @Test
    @DisplayName("trả về entity giữ đủ metadata khi đọc refresh token document")
    void test_toEntity_003() {
        RefreshTokenDocument document =
                new RefreshTokenDocument("id", "jti", "user", EXPIRES_AT, ISSUED_AT);

        assertThat(mapper.toEntity(document))
                .isEqualTo(new RefreshTokenEntity("id", "jti", "user", EXPIRES_AT, ISSUED_AT));
    }

    @Test
    @DisplayName("trả về null khi document nguồn không tồn tại")
    void test_toEntity_004() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
