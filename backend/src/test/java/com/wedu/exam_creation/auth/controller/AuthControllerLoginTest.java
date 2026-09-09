package com.wedu.exam_creation.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.UserResponseDTO;
import com.wedu.exam_creation.auth.usecase.AuthUsecase;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerLoginTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthUsecase authUsecase;

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
        return new LoginUserRequestDTO("anv@gmail.com", "Password123@");
    }

    @Test
    @DisplayName("200 - Đăng nhập thành công")
    void should_loginSuccessfully_when_credentialsAreValid() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("200 - Email viết hoa / thường")
    void should_loginSuccessfully_when_emailIsCaseInsensitive() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "ANV@gmail.com",
                "Password123@"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("200 - Email có nhập tự đặc biệt hợp lệ")
    void should_loginSuccessfully_when_emailContainsValidSpecialCharacters() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv_@gmail.com",
                "Password123@"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("400 - Email có nhập tự đặc biệt không hợp lệ")
    void should_returnBadRequest_when_emailContainsInvalidSpecialCharacters() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@@gmail.com",
                "Password123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("200 - Email có \".\"  hợp lệ")
    void should_loginSuccessfully_when_emailContainsValidDots() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv.tlu@gmail.com",
                "Password123@"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("400 - Email có \".\" không hợp lệ")
    void should_returnBadRequest_when_emailContainsInvalidDots() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                ".anv@gmail.com",
                "Password123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Email chứa nhập tự Unicode")
    void should_returnBadRequest_when_emailContainsUnicodeCharacters() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anguyễnvăn@gmail.com",
                "Password123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Email chứa khoảng trắng")
    void should_returnBadRequest_when_emailContainsWhitespace() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv tlu@gmail.com",
                "Password123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Email dài hơn 254 nhập tự")
    void should_returnBadRequest_when_emailExceeds254Characters() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmd@a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.com",
                "Password123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Email có local part dài hơn 64 nhập tự")
    void should_returnBadRequest_when_emailLocalPartExceeds64Characters() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@gmail.com",
                "Password123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Email rỗng")
    void should_returnBadRequest_when_emailIsEmpty() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "",
                "Password123@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("404 - Email chưa tồn tại")
    void should_returnNotFound_when_emailDoesNotExist() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(new NotFoundException("Tài khoản chưa tồn tại"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());

        verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
    }

    @Test
    @DisplayName("401 - Email đăng nhập bằng Google, thử đăng nhập Local")
    void should_returnUnauthorized_when_loginWithPasswordForGoogleAccount() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(new UnAuthorizedException("Sai phương thức đăng nhập"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());

        verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
    }


    @Test
    @DisplayName("200 - Mật khẩu tài khoản chính xác")
    void should_loginSuccessfully_when_passwordIsCorrect() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                "Password123@"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("401 - Mật khẩu tài khoản không chính xác")
    void should_returnUnauthorized_when_passwordIsIncorrect() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                "Password123"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenThrow(new UnAuthorizedException("Mật khẩu không chính xác"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"));

        verify(authUsecase, times(1)).login(any(LoginUserRequestDTO.class));
    }

    @Test
    @DisplayName("200 - Mật khẩu có đúng 8 nhập tự")
    void should_loginSuccessfully_when_passwordLengthIsExactlyEightCharacters() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                "Passwo1@"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("200 - Mật khẩu có đúng 32 nhập tự")
    void should_loginSuccessfully_when_passwordLengthIsExactlyThirtyTwoCharacters() throws Exception {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 32; i++) {
            int index = (int) (Math.random() * characters.length());
            str.append(characters.charAt(index));
        }

        String pw32 = str.toString();
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                pw32
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("200 - Mật khẩu có chữ hoa, nhập tự, số")
    void should_loginSuccessfully_when_passwordContainsUppercaseLettersAndNumbers() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                "Password123@"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("200 - Mật khẩu không có chữ hoa, nhập tự, số")
    void should_loginSuccessfully_when_passwordDoesNotContainUppercaseLettersOrNumbers() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                "password"
        );

        when(authUsecase.login(any(req.getClass())))
                .thenReturn(mockAuthorizedResponse);

        mockMvc.perform(post("/auth/login")
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
    @DisplayName("400 - Mật khẩu có khoảng trắng")
    void should_returnBadRequest_when_passwordContainsWhitespace() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                "password 123"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Mật khẩu có ít hơn 8 nhập tự")
    void should_returnBadRequest_when_passwordIsShorterThanEightCharacters() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                "Pas1@"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Mật khẩu có nhiều hơn 32 nhập tự")
    void should_returnBadRequest_when_passwordExceedsThirtyTwoCharacters() throws Exception {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 40; i++) {
            int index = (int) (Math.random() * characters.length());
            str.append(characters.charAt(index));
        }

        String pw40 = str.toString();

        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                pw40
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("400 - Mật khẩu rỗng")
    void should_returnBadRequest_when_passwordIsEmpty() throws Exception {
        LoginUserRequestDTO req = new LoginUserRequestDTO(
                "anv@gmail.com",
                ""
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authUsecase, times(0)).login(any());
    }

    @Test
    @DisplayName("403 - Tài khoản đã bị khóa")
    void should_returnForbidden_when_accountIsLocked() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(new ForbiddenException("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verify(authUsecase, times(1)).login(any());
    }

    @Test
    @DisplayName("500 - Lỗi trong quá trình đăng nhập")
    void should_returnInternalServerError_when_savingRtIsFailed() throws Exception {
        when(authUsecase.login(any(LoginUserRequestDTO.class)))
                .thenThrow(new InternalServerException("Lỗi trong quá trình đăng nhập"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError());

        verify(authUsecase, times(1)).login(any());
    }
}
