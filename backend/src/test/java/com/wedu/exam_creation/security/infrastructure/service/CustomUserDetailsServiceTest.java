package com.wedu.exam_creation.security.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.user.usecase.UserService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceTest {
    private static final String EMAIL = "teacher@example.test";
    private final UserService users = mock(UserService.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(users);

    @Test
    @DisplayName("trả về principal chứa đúng tài khoản khi tìm thấy theo email")
    void test_loadUserByUsername_001() {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setEmail(EMAIL);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername(EMAIL))
                .isInstanceOfSatisfying(
                        CustomUserDetails.class,
                        details -> assertThat(details.getUser()).isSameAs(user));
    }

    @Test
    @DisplayName("trả về UsernameNotFoundException khi không tìm thấy tài khoản theo email")
    void test_loadUserByUsername_002() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Tài khoản chưa tồn tại");
    }
}
