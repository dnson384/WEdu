package com.wedu.exam_creation.refreshToken.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.common.dto.refreshToken.response.RTResponseDTO;
import com.wedu.exam_creation.refreshToken.domain.entity.RefreshTokenEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RefreshTokenDTOMapperTest {
    private final RefreshTokenDTOMapper mapper = Mappers.getMapper(RefreshTokenDTOMapper.class);

    @Test
    @DisplayName("trả về DTO giữ ID, JTI, chủ phiên và thời điểm khi ánh xạ entity")
    void test_toResponseDTO_001() {
        Instant issued = Instant.parse("2026-11-22T00:00:00Z");
        Instant expires = Instant.parse("2026-11-29T00:00:00Z");
        RefreshTokenEntity entity = new RefreshTokenEntity("id", "jti", "user", expires, issued);

        assertThat(mapper.toResponseDTO(entity))
                .isEqualTo(new RTResponseDTO("id", "jti", "user", expires, issued));
    }

    @Test
    @DisplayName("trả về null khi entity nguồn không tồn tại")
    void test_toResponseDTO_002() {
        assertThat(mapper.toResponseDTO(null)).isNull();
    }
}
