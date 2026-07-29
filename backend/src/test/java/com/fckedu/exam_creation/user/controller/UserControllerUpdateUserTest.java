package com.fckedu.exam_creation.user.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.fckedu.exam_creation.common.exception.BadRequestException;
import com.fckedu.exam_creation.common.exception.ForbiddenException;
import com.fckedu.exam_creation.common.exception.InternalServerException;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.fckedu.exam_creation.user.dto.request.UpdateUserRequestDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerUpdateUserTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";
    private static final String VALID_RT = "mock-rt";
    private static final String INVALID_RT = "invalid-rt";

    private static final String USER_ID = "user-123";
    private static final String EMAIL = "user@gmail.com";
    private static final String OLD_S3KEY = "avatars/old-s3key.png";
    private static final String NEW_S3KEY = "avatars/new-s3key.png";
    private static final String PRESIGNED_AVATAR = "https://s3.amazonaws.com/my-pic-presigned.png";
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

    private UpdateUserRequestDTO validRequest() {
        UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
        dto.setUsername("valid-username");
        return dto;
    }

    @BeforeEach
    void setUp() {
        userResponseDTO = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                "Nguyen Van A",
                "ROLE_TEACHER",
                PRESIGNED_AVATAR,
                true,
                "FREE");
    }

    @Test
    @DisplayName("Case 1: Cập nhật username thành công với dữ liệu hợp lệ - Happy case")
    void happyCase() throws Exception {
        when(userService.getMe(anyString(), anyString()))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(anyString(), anyString()))
                .thenReturn(true);

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 2: Cập nhật với username rỗng")
    void emptyUsername() throws Exception {
        UpdateUserRequestDTO empty = new UpdateUserRequestDTO("");

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(USER_ID, ""))
                .thenThrow(new BadRequestException("Tên người dùng rỗng"));

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empty))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 3: Cập nhật với username = null")
    void nullUsername() throws Exception {
        UpdateUserRequestDTO nullUsername = new UpdateUserRequestDTO(null);

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(anyString(), isNull()))
                .thenThrow(new BadRequestException("Tên người dùng rỗng"));

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullUsername))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userService, times(1)).getMe(anyString(), anyString());
        verify(userUsecase, times(1)).updateUser(anyString(), isNull());
    }

    @Test
    @DisplayName("Case 4: Cập nhật với username chỉ chứa khoảng trắng")
    void onlySpaceUsername() throws Exception {
        UpdateUserRequestDTO onlySpace = new UpdateUserRequestDTO("      ");

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(USER_ID, "      "))
                .thenThrow(new BadRequestException("Tên người dùng rỗng"));

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onlySpace))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 5: Cập nhật với username chỉ chứa khoảng trắng")
    void longUsername() throws Exception {
        String longUsername = "k9#mP$2vL8!xQ6@zW4*bR1&yT7^uN3(cE5)aJ0_oK9-sI8=dF2+gV7~hX1?pM6<bZ3>vN0!tY5@uR8#eW2$iQ7%oO4^pA9&sD1*fG3(hK6)jL8_zX2-cV4=bN7+mM1!kP9@oI3#uY5$tR2%eW8^qQ0&aS4*dF6(gH9)jK1_lZ3-xX5=cV2+bB7!nM9@kO1#iU4$yT8%rE3^wQ6&aS9*dF2(gH5)jK7";
        UpdateUserRequestDTO longUsernamePayload = new UpdateUserRequestDTO(longUsername);

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(USER_ID, longUsername))
                .thenThrow(new BadRequestException("Tên người dùng quá dài"));

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longUsernamePayload))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng quá dài"));

        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 6: Cập nhật với username là có số")
    void hasNumUsername() throws Exception {
        UpdateUserRequestDTO num = new UpdateUserRequestDTO("abc123");

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(USER_ID, "abc123"))
                .thenReturn(true);

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(num))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 7: Cập nhật với username là có emoji")
    void hasEmojiUsername() throws Exception {
        UpdateUserRequestDTO emoji = new UpdateUserRequestDTO("Nguyen Van A \uD83D\uDE00");

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(USER_ID, "Nguyen Van A \uD83D\uDE00"))
                .thenReturn(true);

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emoji))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 8: Cập nhật với username là có unicode")
    void hasUnicodeUsername() throws Exception {
        UpdateUserRequestDTO unicode = new UpdateUserRequestDTO("Nguyễn Văn A");

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(USER_ID, "Nguyễn Văn A"))
                .thenReturn(true);

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unicode))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }


    @Test
    @DisplayName("Case 9: Không gửi header Authorization")
    void notSendAuthorization() throws Exception {
        // When
        mockMvc.perform(put("/user/update-user")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Thiếu header Authorization"));

        // Then
        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 10: Header Authorization rỗng")
    void emptyAuthorization() throws Exception {
        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization rỗng"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 11: Header Authorization thiếu \"Bearer \" prefix")
    void missingAuthorization() throws Exception {
        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 12: Header Authorization chỉ có \"Bearer\"")
    void justBearer() throws Exception {
        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 13: Header Authorization có AT rỗng")
    void emptyAT() throws Exception {
        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer ")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("AT rỗng"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 14: Body rỗng")
    void noBody() throws Exception {
        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());

        verify(userService, times(0)).findById(anyString());
        verify(userUsecase, times(0)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 15: Lỗi trong quá trình cập nhật thông tin tài khoản")
    void saveFailure() throws Exception {
        when(userService.getMe(anyString(), anyString()))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(anyString(), anyString()))
                .thenThrow(new InternalServerException("Có lỗi trong quá trình cập nhật tài khoản"));

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message").value("Có lỗi trong quá trình cập nhật tài khoản"));

        verify(userService, times(1)).getMe(anyString(), anyString());
        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 16: Cập nhật khi userId không tồn tại trong DB")
    void userNotfound() throws Exception {
        when(userService.getMe(anyString(), anyString()))
                .thenThrow(new NotFoundException("Không tìm thấy tài khoản"));

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy tài khoản"));

        verify(userService, times(1)).getMe(anyString(), anyString());
        verify(userUsecase, times(0)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 17: Cập nhật khi tài khoản đã bị khóa")
    void lockedUser() throws Exception {
        when(userService.getMe(anyString(), anyString()))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateUser(anyString(), anyString()))
                .thenThrow(new ForbiddenException("Tài khoản đã bị khóa"));

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("message").value("Tài khoản đã bị khóa"));

        verify(userService, times(1)).getMe(anyString(), anyString());
        verify(userUsecase, times(1)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 18: Gọi bằng method khác")
    void otherMethod() throws Exception {
        mockMvc.perform(post("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("message").value("Phương thức HTTP không được hỗ trợ"));

        verify(userService, times(0)).getMe(anyString(), anyString());
        verify(userUsecase, times(0)).updateUser(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 19: Payload lạ")
    void strangePayload() throws Exception {
        String payload = "{\"username\": \"Nguyen Van A\", \"role\": \"ROLE_ADMIN\"}";

        mockMvc.perform(put("/user/update-user")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                )
                .andExpect(status().isBadRequest());

        verify(userService, times(0)).getMe(anyString(), anyString());
        verify(userUsecase, times(0)).updateUser(anyString(), anyString());
    }
}
