package com.wedu.exam_creation.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.usecase.AuthUsecase;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.user.controller.UserController;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerLoginTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUsecase authUsecase;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private AuthorizedResponseDTO mockAuthorizedResponse;

    @BeforeEach
    void setUp() {
        UserResponseDTO mockUserResponse = new UserResponseDTO(
                "user-123",
                "anv@gmail.com",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "avatars/default-avatar-user.png"
        );

        mockAuthorizedResponse = new AuthorizedResponseDTO(
                mockUserResponse,
                "mock-access-token",
                "mock-refresh-token"
        );
    }

    private LoginUserRequestDTO validRequest() {
        LoginUserRequestDTO dto = new LoginUserRequestDTO();
        dto.setEmail("anv@gmail.com");
        dto.setPlainPassword("Password123@");
        return dto;
    }

    @Nested
    @DisplayName("Email")
    class EmailCase {
        @Test
        @DisplayName("Đăng nhập thành công - Happy case")
        void happyCase() throws Exception {
            when(authUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thành công - Email viết hoa / thường")
        void caseSensitive() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "ANV@gmail.com",
                    "Password123@"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thành công - Email có nhập tự đặc biệt hợp lệ")
        void validSpecialCharacter() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv_@gmail.com",
                    "Password123@"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email có nhập tự đặc biệt không hợp lệ")
        void invalidSpecialCharacter() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@@gmail.com",
                    "Password123@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).login(any());
        }

        @Test
        @DisplayName("Đăng nhập thành công - Email có \".\"  hợp lệ")
        void validDot() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv.tlu@gmail.com",
                    "Password123@"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email có \".\"  hợp lệ")
        void invalidDot() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    ".anv@gmail.com",
                    "Password123@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).login(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email chứa nhập tự Unicode")
        void unicode() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anguyễnvăn@gmail.com",
                    "Password123@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).login(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email chứa khoảng trắng")
        void space() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv tlu@gmail.com",
                    "Password123@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).login(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email dài hơn 254 nhập tự")
        void more254() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmd@a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.com",
                    "Password123@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email có local part dài hơn 64 nhập tự")
        void localPartMore64() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@gmail.com",
                    "Password123@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email rỗng")
        void empty() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "",
                    "Password123@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Email chưa tồn tại")
        void login_EmailNotFound_ReturnsNotFound() throws Exception {
            when(authUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new NotFoundException("Tài khoản chưa tồn tại"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isNotFound());

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("Login Method")
    class LoginMethodCase {
        @Test
        @DisplayName("Đăng nhập thất bại - Email đăng nhập bằng Google, thử đăng nhập Local")
        void login_GoogleAccount_ReturnsUnauthorized() throws Exception {
            when(authUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new UnAuthorizedException("Sai phương thức đăng nhập"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Password")
    class PasswordCase {
        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu tài khoản chính xác")
        void correctPassword() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu tài khoản không chính xác")
        void login_WrongPassword_ReturnsUnauthorized() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    "Password123"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenThrow(new UnAuthorizedException("Mật khẩu không chính xác"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(cookie().doesNotExist("accessToken"))
                    .andExpect(cookie().doesNotExist("refreshToken"));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu có đúng 8 nhập tự")
        void exact8Char() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    "Passwo1@"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu có đúng 32 nhập tự")
        void exact32Char() throws Exception {
            String pw32 = "A1@" + "a".repeat(29);
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    pw32
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu có chữ hoa, nhập tự, số")
        void special() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu không có chữ hoa, nhập tự, số")
        void nonSpecial() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    "password"
            );

            when(authUsecase.login(any(req.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"));

            verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu có khoảng trắng")
        void passwordSpace() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    "password 123"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).login(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu có ít hơn 8 nhập tự")
        void passwordTooShort() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    "Pas1@"
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu có nhiều hơn 32 nhập tự")
        void passwordTooLong() throws Exception {
            String pw33 = "A1@" + "a".repeat(30); // tổng 33 nhập tự
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    pw33
            );

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng nhập thất bại - Mật khẩu rỗng")
        void emptyPassword() throws Exception {
            LoginUserRequestDTO req = new LoginUserRequestDTO(
                    "anv@gmail.com",
                    ""
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());

            verify(authUsecase, times(0)).register(any());
        }
    }

    @Nested
    @DisplayName("isActived")
    class IsActivedCase {
        @Test
        @DisplayName("Đăng nhập thất bại - Tài khoản đã bị khóa")
        void lockedAccount() throws Exception {
            when(authUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new UnAuthorizedException("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized());

            verify(authUsecase, times(0)).register(any());
        }
    }
}
