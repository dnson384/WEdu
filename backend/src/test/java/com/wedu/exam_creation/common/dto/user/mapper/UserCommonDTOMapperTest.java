package com.wedu.exam_creation.common.dto.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserCommonDTOMapperTest {
    private final UserCommonDTOMapper mapper = Mappers.getMapper(UserCommonDTOMapper.class);

    @Test
    @DisplayName(
            "trả về đủ dữ liệu hồ sơ công khai khi tài khoản PLUS có thông tin đăng nhập và hạn sử dụng")
    void test_commonAllToCommonDTO_001() {
        Instant createdAt = Instant.parse("2026-11-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-11-02T00:00:00Z");
        Instant plusExpiresAt = Instant.parse("2026-12-02T00:00:00Z");
        CommonUserResponseAllDTO source =
                new CommonUserResponseAllDTO(
                        "user-1",
                        "minh@example.com",
                        "test-hash",
                        "Minh",
                        "ROLE_TEACHER",
                        "LOCAL",
                        "avatar.png",
                        true,
                        "PLUS",
                        createdAt,
                        updatedAt,
                        plusExpiresAt);
        CommonUserResponseDTO expected =
                new CommonUserResponseDTO(
                        "user-1",
                        "minh@example.com",
                        "Minh",
                        "ROLE_TEACHER",
                        "avatar.png",
                        true,
                        "PLUS",
                        createdAt,
                        updatedAt,
                        plusExpiresAt);

        assertThat(mapper.commonAllToCommonDTO(source)).isEqualTo(expected);
    }

    @Test
    @DisplayName("trả về null khi nguồn dữ liệu người dùng bị thiếu")
    void test_commonAllToCommonDTO_002() {
        assertThat(mapper.commonAllToCommonDTO(null)).isNull();
    }
}
