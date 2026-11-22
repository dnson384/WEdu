package com.wedu.exam_creation.auth.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AuthDTOMapperTest {
    private final AuthDTOMapper mapper = Mappers.getMapper(AuthDTOMapper.class);

    @Test
    @DisplayName(
            "trả về thông tin hồ sơ công khai khi ánh xạ tài khoản nội bộ sang phản hồi xác thực")
    void test_toUserResponseDTO_001() {
        Instant created = Instant.parse("2026-11-13T00:00:00Z");
        CommonUserResponseAllDTO source =
                new CommonUserResponseAllDTO(
                        "teacher-1",
                        "teacher@example.com",
                        "hash-fixture",
                        "Teacher",
                        "ROLE_TEACHER",
                        "MIXED",
                        "avatars/teacher.png",
                        true,
                        "FREE",
                        created,
                        created,
                        null);

        UserResponseDTO actual = mapper.toUserResponseDTO(source);

        assertThat(actual)
                .isEqualTo(
                        new UserResponseDTO(
                                "teacher-1",
                                "teacher@example.com",
                                "Teacher",
                                "ROLE_TEACHER",
                                "avatars/teacher.png"));
    }

    @Test
    @DisplayName("trả về null khi thiếu tài khoản nội bộ để ánh xạ phản hồi xác thực")
    void test_toUserResponseDTO_002() {
        assertThat(mapper.toUserResponseDTO(null)).isNull();
    }
}
