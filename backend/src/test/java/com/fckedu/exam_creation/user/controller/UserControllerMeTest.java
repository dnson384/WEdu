package com.fckedu.exam_creation.user.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.fckedu.exam_creation.common.exception.ForbiddenException;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.fckedu.exam_creation.user.usecase.UserService;
import com.fckedu.exam_creation.user.usecase.UserUsecase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerMeTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";
    private static final String VALID_RT = "mock-rt";
    private static final String INVALID_RT = "invalid-rt";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUsecase userUsecase;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CommonUserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        userResponseDTO = new CommonUserResponseDTO(
                "user-123",
                "anv@gmail.com",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "avatars/default-avatar-user.png",
                true,
                "FREE");
    }

    @Test
    @DisplayName("Case 1: Lấy thông tin cá nhân thành công với AT hợp lệ")
    void success() throws Exception {
        // Given
        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);

        // When
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then
        verify(userService, times(1)).getMe(VALID_AT, VALID_RT);
    }

    @Test
    @DisplayName("Case 2: Không gửi header Authorization")
    void notSendAuthorization() throws Exception {
        // When
        mockMvc.perform(get("/user/me")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Thiếu header Authorization xác thực người dùng"));

        // Then
        verify(userService, never()).getMe(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 3: Header Authorization rỗng")
    void emptyAuthorization() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization rỗng"));

        verify(userService, never()).getMe(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 4: Header Authorization thiếu \"Bearer \" prefix")
    void missingAuthorization() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(userService, never()).getMe(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 5: Header Authorization chỉ có \"Bearer\"")
    void justBearer() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(userService, never()).getMe(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 6: Header Authorization có AT rỗng")
    void emptyAT() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer ")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("AT rỗng"));

        verify(userService, never()).getMe(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 7: AT sai định dạng - không phải JWT")
    void invalidAT() throws Exception {
        when(userService.getMe(INVALID_AT, VALID_RT))
                .thenThrow(new UnAuthorizedException("Cấu trúc Access Token không hợp lệ"));

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + INVALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Cấu trúc Access Token không hợp lệ"));

        verify(userService, times(1)).getMe(INVALID_AT, VALID_RT);
    }

    @Test
    @DisplayName("Case 8: AT hết hạn")
    void expiredAT() throws Exception {
        String expiredAT = "expired-at";
        when(userService.getMe(expiredAT, VALID_RT))
                .thenThrow(new UnAuthorizedException("Access Token đã hết hạn"));

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + expiredAT)
                        .cookie(new Cookie("accessToken", expiredAT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Access Token đã hết hạn"));

        verify(userService, times(1)).getMe(expiredAT, VALID_RT);
    }

    @Test
    @DisplayName("Case 9: AT bị chỉnh sửa")
    void editedAT() throws Exception {
        String editedAT = "edited-at";
        when(userService.getMe(editedAT, VALID_RT))
                .thenThrow(new UnAuthorizedException("Chữ ký Access Token không chính xác"));

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + editedAT)
                        .cookie(new Cookie("accessToken", editedAT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Chữ ký Access Token không chính xác"));

        verify(userService, times(1)).getMe(editedAT, VALID_RT);
    }

    @Test
    @DisplayName("Case 10: AT hợp lệ nhưng userId trong payload không tồn tại trong DB")
    void nonExistedUser() throws Exception {
        when(userService.getMe(VALID_AT, VALID_RT))
                .thenThrow(new NotFoundException("Không tìm thấy tài khoản"));

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message").value("Không tìm thấy tài khoản"));

        verify(userService, times(1)).getMe(VALID_AT, VALID_RT);
    }

    @Test
    @DisplayName("Case 11: Lấy thông tin của tài khoản đã bị khóa nhưng AT vẫn còn hiệu lực")
    void lockedUser() throws Exception {
        when(userService.getMe(VALID_AT, VALID_RT))
                .thenThrow(new ForbiddenException("Tài khoản đã bị khóa"));

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("message").value("Tài khoản đã bị khóa"));

        verify(userService, times(1)).getMe(VALID_AT, VALID_RT);
    }

    @Test
    @DisplayName("Case 12: Gọi API bằng method khác")
    void wrongHttpMethod() throws Exception {
        mockMvc.perform(get("/user/logout")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("message").value("Phương thức HTTP không được hỗ trợ"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("Case 13: Kiểm tra response không lộ thông tin nhạy cảm")
    void checkSensitiveInfo() throws Exception {
        // Given
        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);

        // When
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("hashedPassword").doesNotExist());


        // Then
        verify(userService, times(1)).getMe(VALID_AT, VALID_RT);
    }

    @Test
    @DisplayName("Case 14: Truyền AT của User A phải trả về đúng dữ liệu của User A, không lộn sang User B")
    void getMe_shouldReturnUserDataOfTokenOwner() throws Exception {
        // 1. GIVEN: Chuẩn bị thông tin giả lập cho 2 User khác nhau
        String tokenA = "token-of-user-A";
        String tokenB = "token-of-user-B";
        String rtA = "rt-of-user-A";

        CommonUserResponseDTO responseUserA = new CommonUserResponseDTO(
                "user-A",
                "usera@gmail.com",
                "User A Name",
                "ROLE_USER",
                "avatars/userA.png",
                true,
                "FREE"
        );

        CommonUserResponseDTO responseUserB = new CommonUserResponseDTO(
                "user-B",
                "userb@gmail.com",
                "User B Name",
                "ROLE_USER",
                "avatars/userB.png",
                true,
                "FREE"
        );

        when(userService.getMe(tokenA, rtA)).thenReturn(responseUserA);

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .cookie(new Cookie("accessToken", tokenA))
                        .cookie(new Cookie("refreshToken", rtA))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value("user-A"))
                .andExpect(jsonPath("id").value(org.hamcrest.Matchers.not("user-B")))
                .andExpect(jsonPath("email").value("usera@gmail.com"))
                .andExpect(jsonPath("email").value(org.hamcrest.Matchers.not("userb@gmail.com")))
                .andExpect(jsonPath("username").value("User A Name"));

        verify(userService, times(1)).getMe(tokenA, rtA);
        verify(userService, never()).getMe(eq(tokenB), anyString());
    }

    @Test
    @DisplayName("Case 14: Header Authorization dùng sai scheme")
    void wrongSchemeAuthorization() throws Exception {
        String wrongSchema = "Basic dXNlcjpwYXNz";

        // When
        mockMvc.perform(get("/user/me")
                        .header("Authorization", wrongSchema)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(userService, times(0)).getMe(anyString(), anyString());
    }
}
