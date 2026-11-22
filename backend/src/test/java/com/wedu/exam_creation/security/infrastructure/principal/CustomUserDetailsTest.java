package com.wedu.exam_creation.security.infrastructure.principal;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;

class CustomUserDetailsTest {
    private final CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
    private final CustomUserDetails details = new CustomUserDetails(user);

    @Test
    @DisplayName("trả về authority đúng vai trò khi Spring Security đọc principal")
    void test_getAuthorities_001() {
        user.setRole("ROLE_MODERATOR");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MODERATOR");
    }

    @Test
    @DisplayName("trả về mật khẩu đã hash khi Spring Security đọc thông tin xác thực")
    void test_getPassword_002() {
        user.setHashedPassword("stored-hash");

        assertThat(details.getPassword()).isEqualTo("stored-hash");
    }

    @Test
    @DisplayName("trả về email khi Spring Security đọc tên đăng nhập")
    void test_getUsername_003() {
        user.setEmail("teacher@example.test");

        assertThat(details.getUsername()).isEqualTo("teacher@example.test");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về trạng thái hoạt động của tài khoản khi kiểm tra principal")
    void test_isEnabled_004(boolean active) {
        user.setIsActive(active);

        assertThat(details.isEnabled()).isEqualTo(active);
    }
}
