package com.wedu.exam_creation.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.auth.dto.request.ChangePasswordRequestDTO;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.usecase.AuthUsecase;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean private AuthUsecase authUsecase;

    private AuthorizedResponseDTO mockAuthorizedResponse;

    @BeforeEach
    void setUp() {
        UserResponseDTO mockUserResponse =
                new UserResponseDTO(
                        "user-123",
                        "anv@gmail.com",
                        "Nguyen Van A",
                        "ROLE_TEACHER",
                        "avatars/default-avatar-user.png");

        mockAuthorizedResponse =
                new AuthorizedResponseDTO(
                        mockUserResponse, "mock-access-token", "mock-refresh-token");
    }

    private LoginUserRequestDTO validRequest() {
        return new LoginUserRequestDTO("anv@gmail.com", "Password123@");
    }

    @Test
    @DisplayName("trả về 200 khi Đăng nhập thành công")
    void test_login_001() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 200 khi Email viết hoa / thường")
    void test_login_002() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("ANV@gmail.com", "Password123@");

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 200 khi Email có nhập ký tự đặc biệt hợp lệ")
    void test_login_003() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv_@gmail.com", "Password123@");

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 400 khi Email có ký tự đặc biệt không hợp lệ")
    void test_login_004() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@@gmail.com", "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 200 khi Email có \".\"  hợp lệ")
    void test_login_005() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv.tlu@gmail.com", "Password123@");

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 400 khi Email có \".\" không hợp lệ")
    void test_login_006() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(".anv@gmail.com", "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email chứa ký tự Unicode")
    void test_login_007() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anguyễnvăn@gmail.com", "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email chứa khoảng trắng")
    void test_login_008() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv tlu@gmail.com", "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email dài hơn 254 ký tự")
    void test_login_009() throws Exception {
        LoginUserRequestDTO req =
                new LoginUserRequestDTO(
                        "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmd@a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.com",
                        "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email có local part dài hơn 64 ký tự")
    void test_login_010() throws Exception {
        LoginUserRequestDTO req =
                new LoginUserRequestDTO(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@gmail.com",
                        "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email rỗng")
    void test_login_011() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("", "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email null")
    void test_login_012() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(null, "Password123@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 404 khi Email chưa tồn tại")
    void test_login_013() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(new NotFoundException("Tài khoản chưa tồn tại"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());

        verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 401 khi Email đăng nhập bằng Google, thử đăng nhập Local")
    void test_login_014() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(new UnAuthorizedException("Sai phương thức đăng nhập"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());

        verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 200 khi Mật khẩu tài khoản chính xác")
    void test_login_015() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "Password123@");

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 401 khi Mật khẩu tài khoản không chính xác")
    void test_login_016() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "Password123");

        when(authUsecase.login(any(req.getClass())))
                .thenThrow(new UnAuthorizedException("Mật khẩu không chính xác"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"));

        verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 200 khi Mật khẩu có đúng 8 ký tự")
    void test_login_017() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "Passwo1@");

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 200 khi Mật khẩu có đúng 32 ký tự")
    void test_login_018() throws Exception {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 32; i++) {
            int index = (int) (Math.random() * characters.length());
            str.append(characters.charAt(index));
        }

        String pw32 = str.toString();
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", pw32);

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 200 khi Mật khẩu có chữ hoa, ký tự, số")
    void test_login_019() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "Password123@");

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 200 khi Mật khẩu không có chữ hoa, ký tự, số")
    void test_login_020() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "password");

        when(authUsecase.login(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mockMvc.perform(
                        post("/auth/login")
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
    @DisplayName("trả về 400 khi Mật khẩu có khoảng trắng")
    void test_login_021() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "password 123");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu có ít hơn 8 ký tự")
    void test_login_022() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "Pas1@");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu có nhiều hơn 32 ký tự")
    void test_login_023() throws Exception {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 40; i++) {
            int index = (int) (Math.random() * characters.length());
            str.append(characters.charAt(index));
        }

        String pw40 = str.toString();

        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", pw40);

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu rỗng")
    void test_login_024() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", "");

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu null")
    void test_login_025() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO("anv@gmail.com", null);

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("trả về 403 khi Tài khoản đã bị khóa")
    void test_login_026() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(
                        new ForbiddenException(
                                "Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verify(authUsecase, times(1)).login(any());
    }

    @Test
    @DisplayName("trả về 500 khi Lỗi trong quá trình đăng nhập")
    void test_login_027() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(new InternalServerException("Lỗi trong quá trình đăng nhập"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError());

        verify(authUsecase, times(1)).login(any());
    }

    private static final String LOGOUT_URL = "/auth/logout";
    private static final String AUTHORIZATION = "Bearer mock-at";

    @Test
    @DisplayName("trả về 200 khi true khi usecase đăng xuất thành công")
    void test_logout_028() throws Exception {
        when(authUsecase.logout(AUTHORIZATION)).thenReturn(true);

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(true));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 200 khi false khi usecase trả về false")
    void test_logout_029() throws Exception {
        when(authUsecase.logout(AUTHORIZATION)).thenReturn(false);

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 400 khi Thiếu header Authorization")
    void test_logout_030() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Thiếu header Authorization"));

        verifyNoInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 404 khi Usecase không tìm thấy refresh token")
    void test_logout_031() throws Exception {
        when(authUsecase.logout(AUTHORIZATION))
                .thenThrow(new NotFoundException("Không tìm thấy RT"));

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy RT"));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 500 khi Usecase gặp lỗi khi xóa refresh token")
    void test_logout_032() throws Exception {
        when(authUsecase.logout(AUTHORIZATION))
                .thenThrow(new InternalServerException("Dữ liệu không nhất quán, xóa dư"));

        mockMvc.perform(post(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Dữ liệu không nhất quán, xóa dư"));

        verify(authUsecase).logout(AUTHORIZATION);
        verifyNoMoreInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 405 khi Gọi logout bằng GET")
    void test_logout_033() throws Exception {
        mockMvc.perform(get(LOGOUT_URL).header("Authorization", AUTHORIZATION))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Phương thức HTTP không được hỗ trợ"));

        verifyNoInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 200 và access token mới khi refresh cookie hợp lệ")
    void test_regenerateAccessToken_034() throws Exception {
        when(authUsecase.regenerateAccessToken("rt")).thenReturn("new-at");
        mockMvc.perform(
                        post("/auth/regenerate-access-token")
                                .cookie(new Cookie("refreshToken", "rt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-at"));
    }

    @Test
    @DisplayName("trả về 400 trước usecase khi thiếu refresh cookie")
    void test_regenerateAccessToken_035() throws Exception {
        mockMvc.perform(post("/auth/regenerate-access-token")).andExpect(status().isBadRequest());
        verifyNoInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 401 khi refresh token đã bị thu hồi")
    void test_regenerateAccessToken_036() throws Exception {
        when(authUsecase.regenerateAccessToken("revoked"))
                .thenThrow(new UnAuthorizedException("revoked"));
        mockMvc.perform(
                        post("/auth/regenerate-access-token")
                                .cookie(new Cookie("refreshToken", "revoked")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("trả về 200 khi usecase nhận yêu cầu quên mật khẩu")
    void test_forgotPassword_037() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").param("email", "teacher@example.com"))
                .andExpect(status().isOk());
        verify(authUsecase).forgotPassword("teacher@example.com");
    }

    @Test
    @DisplayName("trả về 400 trước usecase khi thiếu email quên mật khẩu")
    void test_forgotPassword_038() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")).andExpect(status().isBadRequest());
        verifyNoInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 200 khi token và mật khẩu đặt lại được usecase chấp nhận")
    void test_resetPassword_039() throws Exception {
        mockMvc.perform(
                        post("/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"plainToken\":\"reset-token\",\"newPassword\":\"Password123@\",\"confirmNewPassword\":\"Password123@\"}"))
                .andExpect(status().isOk());
        verify(authUsecase).resetPassword("reset-token", "Password123@", "Password123@");
    }

    @Test
    @DisplayName("trả về 400 trước usecase khi mật khẩu đặt lại rỗng")
    void test_resetPassword_040() throws Exception {
        mockMvc.perform(
                        post("/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 200 và dùng ID principal khi yêu cầu đổi mật khẩu hợp lệ")
    void test_changePassword_041() throws Exception {
        CommonUserResponseAllDTO user = new CommonUserResponseAllDTO();
        user.setId("owner-id");
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        when(authUsecase.changePassword(
                        eq("mock-at"), eq("owner-id"), any(ChangePasswordRequestDTO.class)))
                .thenReturn(true);
        try {
            mockMvc.perform(
                            patch("/auth/change-password")
                                    .header("Authorization", AUTHORIZATION)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"oldPassword\":\"old-password\",\"newPassword\":\"new-password\",\"confirmNewPassword\":\"new-password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
            verify(authUsecase)
                    .changePassword(
                            eq("mock-at"),
                            eq("owner-id"),
                            eq(
                                    new ChangePasswordRequestDTO(
                                            "old-password", "new-password", "new-password")));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("trả về 400 trước usecase khi thiếu Authorization đổi mật khẩu")
    void test_changePassword_042() throws Exception {
        mockMvc.perform(
                        patch("/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authUsecase);
    }

    @Test
    @DisplayName("trả về 401 khi usecase từ chối token đặt lại mật khẩu")
    void test_resetPassword_043() throws Exception {
        doThrow(new UnAuthorizedException("expired"))
                .when(authUsecase)
                .resetPassword("expired", "Password123@", "Password123@");
        mockMvc.perform(
                        post("/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"plainToken\":\"expired\",\"newPassword\":\"Password123@\",\"confirmNewPassword\":\"Password123@\"}"))
                .andExpect(status().isUnauthorized());
    }
}
