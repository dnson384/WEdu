package com.wedu.exam_creation.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.user.dto.request.NewUserRequestDTO;
import com.wedu.exam_creation.user.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.user.dto.response.UserResponseDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import com.wedu.exam_creation.user.usecase.UserUsecase;
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
    @DisplayName("Email")
    class EmailCase {
        @Test
        @DisplayName("Đăng ký thành công - Email chưa tồn tại, Email đúng định dạng")
        void happyCase() throws Exception {
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
        @DisplayName("Đăng ký thành công - Email có ký tự đặc biệt")
        void emailSpecialCharacters() throws Exception {
            NewUserRequestDTO newUserRequestDTO = new NewUserRequestDTO(
                    "anv_@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );
            when(userUsecase.register(any(newUserRequestDTO.getClass())))
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
        @DisplayName("Đăng ký thất bại - Email có ký tự đặc biệt không hợp lệ")
        void emailInvalidSpecialCharacters() throws Exception {
            NewUserRequestDTO invalidRequest = new NewUserRequestDTO(
                    "anv@@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thành công - Email có \".\"  hợp lệ")
        void emailValidDot() throws Exception {
            NewUserRequestDTO validDotReq = new NewUserRequestDTO(
                    "anv.tlu@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            when(userUsecase.register(any(validDotReq.getClass())))
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
        @DisplayName("Đăng ký thất bại - Email có \".\"  không hợp lệ\n")
        void emailInvalidDot() throws Exception {
            NewUserRequestDTO invalidDotRequest = new NewUserRequestDTO(
                    ".anv@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDotRequest)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thành công - Email viết hoa / thường")
        void registerEmailCaseSensitive() throws Exception {
            NewUserRequestDTO upperRequest = new NewUserRequestDTO(
                    "ANV@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            when(userUsecase.register(any(upperRequest.getClass())))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(upperRequest)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email chứa ký tự Unicode")
        void emailUnicode() throws Exception {
            NewUserRequestDTO unicodeRequest = new NewUserRequestDTO(
                    "anguyễnvăn@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unicodeRequest)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email chứa khoảng trắng")
        void emailSpace() throws Exception {
            NewUserRequestDTO spaceRequest = new NewUserRequestDTO(
                    "anv tlu@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(spaceRequest)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email dài hơn 254 ký tự")
        void emailMore254() throws Exception {
            NewUserRequestDTO more254Request = new NewUserRequestDTO(
                    "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmd@a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(more254Request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email có local part dài hơn 64 ký tự")
        void emailLocalPartMore64() throws Exception {
            NewUserRequestDTO localPartRequest = new NewUserRequestDTO(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(localPartRequest)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email rỗng")
        void emailEmpty() throws Exception {
            NewUserRequestDTO empty = new NewUserRequestDTO(
                    "",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(empty)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Email đã tồn tại")
        void emailAlreadyExists() throws Exception {
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenThrow(new BadRequestException("Tài khoản đã tồn tại"));

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("password")
    class PasswordCase {
        @Test
        @DisplayName("Đăng ký thành công - Mật khẩu có đúng 8 ký tự")
        void minPasswordLength() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "boundary@gmail.com",
                    "Passwo1@",
                    "Passwo1@",
                    "Nguyen Van A",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng ký thành công - Mật khẩu có đúng 32 ký tự")
        void maxPasswordLength() throws Exception {
            String pw32 = "A1@" + "a".repeat(29);
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
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng ký thành công - Mật khẩu có chữ hoa, ký tự đặc biệt, số")
        void specialPasswordLength() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "boundary2@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng ký thành công - Mật khẩu không có chữ hoa, ký tự đặc biệt, số")
        void normalPasswordLength() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "boundary2@gmail.com",
                    "passwordabcd",
                    "passwordabcd",
                    "Nguyen Van A",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại - Mật khẩu có khoảng trắng")
        void passwordSpace() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "boundary2@gmail.com",
                    "password abcd",
                    "password abcd",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Mật khẩu có ít hơn 8 ký tự")
        void passwordTooShort() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Pas1@",
                    "Pas1@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Mật khẩu có nhiều hơn 32 ký tự")
        void passwordTooLong() throws Exception {
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

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Mật khẩu rỗng")
        void emptyPassword() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "",
                    "Password123@",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }
    }

    @Nested
    @DisplayName("confirmPassword")
    class ConfirmPasswordCase {
        @Test
        @DisplayName("Đăng ký thất bại - Xác nhận mật khẩu rỗng")
        void emptyConfirmPassword() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "password123@",
                    "",
                    "Nguyen Van A",
                    "LOCAL"
            );

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(0)).register(any());
        }

        @Test
        @DisplayName("Đăng ký thất bại - Xác nhận mật khẩu không trùng khớp")
        void confirmPasswordNotMatch() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "password123@",
                    "password123",
                    "Nguyen Van A",
                    "LOCAL"
            );

            when(userUsecase.register(any(request.getClass())))
                    .thenThrow(new BadRequestException("Mật khẩu xác nhận không trùng khớp"));

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("username")
    class UsernameCase {
        @Test
        @DisplayName("Đăng ký thất bại - Tên người dùng rỗng")
        void blankUsername() throws Exception {
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

            verify(userUsecase, times(0)).register(any());
        }


        @Test
        @DisplayName("Đăng ký thành công - Tên người dùng là tiếng Việt")
        void withVietnameseUsername() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "user.vn@gmail.com",
                    "Password123@",
                    "Password123@",
                    "Nguyễn Văn A",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }

        @Test
        @DisplayName("Đăng ký thành công - Tên người dùng có ký tự đặc biệt")
        void withSpecialCharacterUsername() throws Exception {
            NewUserRequestDTO request = new NewUserRequestDTO(
                    "anv@gmail.com",
                    "Password123@",
                    "Password123@",
                    "anv@123",
                    "LOCAL"
            );
            when(userUsecase.register(any(NewUserRequestDTO.class)))
                    .thenReturn(mockAuthorizedResponse);

            mockMvc.perform(post("/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("accessToken"))
                    .andExpect(cookie().value("accessToken", "mock-at"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().value("refreshToken", "mock-rt"));

            verify(userUsecase, times(1)).register(any(NewUserRequestDTO.class));
        }
    }
}
