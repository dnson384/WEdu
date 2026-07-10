package com.fckedu.exam_creation.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.common.exception.BadRequestException;
import com.fckedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.fckedu.exam_creation.user.dto.request.NewUserRequestDTO;
import com.fckedu.exam_creation.user.dto.response.AuthorizedResponseDTO;
import com.fckedu.exam_creation.user.dto.response.UserResponseDTO;
import com.fckedu.exam_creation.user.usecase.UserService;
import com.fckedu.exam_creation.user.usecase.UserUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
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


@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerRegisterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUsecase userUsecase;

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
                "mock-at",
                "mock-rt"
        );
    }

    private NewUserRequestDTO validRequest() {
        return new NewUserRequestDTO(
                "anv@gmail.com",
                "Password123@",   // 12 ký tự, hợp lệ (8-32)
                "Password123@",
                "Nguyen Van A",
                "LOCAL"
        );
    }

    @Nested
    @DisplayName("Success cases")
    class SuccessCases {
        @Test
        @DisplayName("Đăng ký thành công - trả về User DTO và set đúng 2 Cookie")
        void register_Success() throws Exception {
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng ký thành công - password đúng biên tối thiểu (8 ký tự)")
        void register_Success_MinPasswordLength() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "boundary@gmail.com",
                    "Passwo1@", // đúng 8 ký tự
                    "Passwo1@",
                    "Nguyen Van A",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Đăng ký thành công - password đúng biên tối đa (32 ký tự)")
        void register_Success_MaxPasswordLength() throws Exception {
            String pw32 = "A1@" + "a".repeat(29); // tổng 32 ký tự
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "boundary2@gmail.com",
                    pw32,
                    pw32,
                    "Nguyen Van A",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Đăng ký thành công - loginMethod = GOOGLE")
        void register_Success_GoogleLoginMethod() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "googleuser@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "GOOGLE"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Đăng ký thành công - username có dấu tiếng Việt")
        void register_Success_WithVietnameseUsername() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "user.vn@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Trần Thị Bích Ngọc",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Validation cases - yêu cầu controller có @Valid")
    class ValidationCases {
        @Test
        @DisplayName("400 khi email sai định dạng (@Email)")
        void register_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "not-an-email",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, never()).register(any());
        }

        @Test
        @DisplayName("400 khi email rỗng (@NotBlank)")
        void register_BlankEmail_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi email null (@NotBlank)")
        void register_NullEmail_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    null,
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi password ngắn hơn 8 ký tự (@Size min)")
        void register_PasswordTooShort_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Pas1@",  // 6 ký tự < 8
                    "Pas1@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi password dài hơn 32 ký tự (@Size max)")
        void register_PasswordTooLong_ReturnsBadRequest() throws Exception {
            String pw33 = "A1@" + "a".repeat(30); // tổng 33 ký tự
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    pw33,
                    pw33,
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi password null (@NotBlank)")
        void register_NullPassword_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    null,
                    null,
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi confirmPassword rỗng (@NotBlank) - không liên quan đến match check")
        void register_BlankConfirmPassword_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@",
                    "",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi username rỗng (@NotBlank)")
        void register_BlankUsername_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@",
                    "Password123@",
                    "",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi loginMethod không khớp LOCAL|GOOGLE (@Pattern)")
        void register_InvalidLoginMethod_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "FACEBOOK"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 khi loginMethod sai kiểu chữ (lowercase 'local')")
        void register_LowercaseLoginMethod_ReturnsBadRequest() throws Exception {
            // Pattern là "^(LOCAL|GOOGLE)$" - phân biệt hoa thường, "local" sẽ không khớp
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "local"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Business rule - confirmPassword không khớp")
    class ConfirmPasswordMismatchCase {
        @Test
        @DisplayName("400 khi confirmPassword không khớp plainPassword (BadRequestException từ usecase)")
        void register_ConfirmPasswordMismatch_ReturnsBadRequest() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@",
                    "Password999@", // khác plainPassword, nhưng vẫn hợp lệ theo @Size
                    "Nguyen Van A",
                    "LOCAL"
            );

            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenThrow(new BadRequestException("Mật khẩu xác nhận không trùng khớp"));

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("Business rule - email đã tồn tại")
    class EmailAlreadyExistsCase {
        @Test
        @DisplayName("400 khi email đã tồn tại (BadRequestException từ usecase)")
        void register_EmailAlreadyExists_ReturnsBadRequest() throws Exception {
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenThrow(new BadRequestException("Tài khoản đã tồn tại"));

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }
    }

}
