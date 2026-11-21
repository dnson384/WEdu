package com.wedu.exam_creation.user.dto.mapper;

import static org.assertj.core.api.Assertions.*;

import com.wedu.exam_creation.user.domain.entity.UserEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserDTOMapperTest {
    @Test
    @DisplayName("trả về trạng thái FREE và ngày hết hạn khi ánh xạ người dùng đã hết PLUS")
    void test_toCommonDTO_001() {
        UserEntity user = new UserEntity();
        user.setAccountType("PLUS");
        user.setPlusExpiresAt(Instant.EPOCH);
        UserDTOMapper mapper = Mappers.getMapper(UserDTOMapper.class);
        assertThat(mapper.toCommonAllDTO(user).getAccountType()).isEqualTo("FREE");
        assertThat(mapper.toCommonDTO(user).getAccountType()).isEqualTo("FREE");
        assertThat(mapper.toCommonDTO(user).getPlusExpiresAt()).isEqualTo(Instant.EPOCH);
        assertThat(user.getAccountType()).isEqualTo("PLUS");
    }
}
