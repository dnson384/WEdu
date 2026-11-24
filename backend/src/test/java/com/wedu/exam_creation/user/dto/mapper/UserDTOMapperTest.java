package com.wedu.exam_creation.user.dto.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.MockedStatic;

class UserDTOMapperTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FUTURE = Instant.parse("2026-01-31T00:00:00Z");
    private final UserDTOMapper mapper = Mappers.getMapper(UserDTOMapper.class);

    @Test
    @DisplayName(
            "trả về hồ sơ FREE cùng hạn PLUS đã lưu khi quyền PLUS hết hạn đúng thời điểm ánh xạ")
    void test_toCommonDTO_001() {
        UserEntity user = paidUser(NOW);
        CommonUserResponseDTO dto;

        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(NOW);
            dto = mapper.toCommonDTO(user);
        }

        assertThat(dto.getId()).isEqualTo("teacher-1");
        assertThat(dto.getAvatarUrl()).isEqualTo("avatars/profile.png");
        assertThat(dto.getAccountType()).isEqualTo("FREE");
        assertThat(dto.getPlusExpiresAt()).isEqualTo(NOW);
        assertThat(user.getAccountType()).isEqualTo("PLUS");
    }

    @Test
    @DisplayName("trả về hồ sơ nội bộ FREE cùng thông tin đăng nhập khi tài khoản đã hết hạn PLUS")
    void test_toCommonAllDTO_002() {
        Instant expiry = NOW.minusSeconds(1);
        UserEntity user = paidUser(expiry);
        CommonUserResponseAllDTO dto;

        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(NOW);
            dto = mapper.toCommonAllDTO(user);
        }

        assertThat(dto.getAccountType()).isEqualTo("FREE");
        assertThat(dto.getPlusExpiresAt()).isEqualTo(expiry);
        assertThat(dto.getHashedPassword()).isEqualTo("stored-hash");
        assertThat(dto.getLoginMethod()).isEqualTo("MIXED");
        assertThat(user.getAccountType()).isEqualTo("PLUS");
    }

    @Test
    @DisplayName("trả về hồ sơ PLUS khi hạn trả phí vẫn còn hiệu lực")
    void test_toCommonDTO_003() {
        UserEntity user = paidUser(FUTURE);
        CommonUserResponseDTO dto;

        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(NOW);
            dto = mapper.toCommonDTO(user);
        }

        assertThat(dto.getAccountType()).isEqualTo("PLUS");
        assertThat(dto.getPlusExpiresAt()).isEqualTo(FUTURE);
    }

    @Test
    @DisplayName("trả về hồ sơ nội bộ PLUS khi tài khoản PLUS cũ không có ngày hết hạn")
    void test_toCommonAllDTO_004() {
        UserEntity user = paidUser(null);
        CommonUserResponseAllDTO dto;

        try (MockedStatic<Instant> time = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            time.when(Instant::now).thenReturn(NOW);
            dto = mapper.toCommonAllDTO(user);
        }

        assertThat(dto.getAccountType()).isEqualTo("PLUS");
        assertThat(dto.getPlusExpiresAt()).isNull();
    }

    @Test
    @DisplayName("trả về entity giữ thông tin đăng nhập và hạn quyền khi ánh xạ hồ sơ nội bộ")
    void test_commonAllToEntity_005() {
        CommonUserResponseAllDTO dto =
                new CommonUserResponseAllDTO(
                        "teacher-1",
                        "teacher@example.com",
                        "stored-hash",
                        "Giáo viên",
                        "ROLE_TEACHER",
                        "MIXED",
                        "avatars/profile.png",
                        true,
                        "PLUS",
                        NOW,
                        NOW,
                        FUTURE);

        UserEntity user = mapper.commonAllToEntity(dto);

        assertThat(user).usingRecursiveComparison().ignoringFields("googleSubject").isEqualTo(dto);
    }

    @Test
    @DisplayName("trả về null khi không có hồ sơ nội bộ để ánh xạ")
    void test_commonAllToEntity_006() {
        assertThat(mapper.commonAllToEntity(null)).isNull();
    }

    private UserEntity paidUser(Instant expiry) {
        return new UserEntity(
                "teacher-1",
                "teacher@example.com",
                "stored-hash",
                "Giáo viên",
                "ROLE_TEACHER",
                "MIXED",
                "avatars/profile.png",
                true,
                "PLUS",
                NOW,
                NOW,
                expiry);
    }
}
