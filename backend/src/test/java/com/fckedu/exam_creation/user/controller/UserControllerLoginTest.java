package com.fckedu.exam_creation.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.user.dto.request.LoginUserRequestDTO;
import com.fckedu.exam_creation.user.dto.response.AuthorizedResponseDTO;
import com.fckedu.exam_creation.user.dto.response.UserResponseDTO;
import com.fckedu.exam_creation.user.usecase.UserService;
import com.fckedu.exam_creation.user.usecase.UserUsecase;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserUsecase userUsecase;

    @MockitoBean
    private UserService userService;

    private UserResponseDTO mockUserResponse;
    private AuthorizedResponseDTO mockAuthorizedResponse;

    @BeforeEach
    void setUp() {
        mockUserResponse = new UserResponseDTO(
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
    @DisplayName("Success case")
    class SuccessCase {

        @Test
        @DisplayName("Đăng nhập thành công - trả về User DTO và set đúng 2 Cookie")
        void login_Success() throws Exception {
            when(userUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-access-token"))
                    .andExpect(cookie().httpOnly("accessToken", true))
                    .andExpect(cookie().maxAge("accessToken", 15 * 60))
                    .andExpect(cookie().path("accessToken", "/"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-refresh-token"))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().maxAge("refreshToken", 7 * 24 * 60 * 60))
                    .andExpect(cookie().path("refreshToken", "/"))
                    .andExpect(jsonPath("$.id").value("user-123"))
                    .andExpect(jsonPath("$.email").value("anv@gmail.com"));

            verify(userUsecase, times(1)).login(any(LoginUserRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("Usecase throws exception - Controller phải map đúng status")
    class UsecaseExceptionCases {

        @Test
        @DisplayName("404 khi email không tồn tại (NotFoundException)")
        void login_EmailNotFound_ReturnsNotFound() throws Exception {
            when(userUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new NotFoundException("Tài khoản chưa tồn tại"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("401 khi tài khoản đăng ký bằng GOOGLE (UnAuthorizedException)")
        void login_GoogleAccount_ReturnsUnauthorized() throws Exception {
            when(userUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new UnAuthorizedException("Sai phương thức đăng nhập"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 khi tài khoản bị khóa (UnAuthorizedException)")
        void login_LockedAccount_ReturnsUnauthorized() throws Exception {
            when(userUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new UnAuthorizedException("Tài khoản đã bị khóa! Vui lòng liên hệ xxx để được mở khóa"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 khi sai mật khẩu (UnAuthorizedException)")
        void login_WrongPassword_ReturnsUnauthorized() throws Exception {
            when(userUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new UnAuthorizedException("Mật khẩu không chính xác"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized());

            // Đảm bảo không set cookie khi login fail
            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(cookie().doesNotExist("accessToken"))
                    .andExpect(cookie().doesNotExist("refreshToken"));
        }

        @Test
        @DisplayName("500 khi lưu Refresh Token thất bại (InternalServerException)")
        void login_SaveRTFails_ReturnsInternalServerError() throws Exception {
            when(userUsecase.login(any(LoginUserRequestDTO.class)))
                    .thenThrow(new InternalServerException("Lỗi trong quá trình lưu RT!"));

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isInternalServerError());
        }
    }
}
