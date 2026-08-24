package com.wedu.exam_creation.user.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.user.dto.request.UpdateAvatarRequestDTO;
import com.wedu.exam_creation.user.usecase.UserService;
import com.wedu.exam_creation.user.usecase.UserUsecase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerUpdateAvatarTest {
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

    private UpdateAvatarRequestDTO validRequest() {
        UpdateAvatarRequestDTO dto = new UpdateAvatarRequestDTO();
        dto.setS3Key("valid-s3Key");
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
    @DisplayName("Case 1: Update avatar thành công với s3Key vừa upload - Happy case")
    void happyCase() throws Exception {
        when(userService.getMe(anyString(), anyString()))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateAvatar(anyString(), anyString()))
                .thenReturn(true);

        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userUsecase, times(1)).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 2: Lưu thông tin người dùng thất bại")
    void saveFailure() throws Exception {
        when(userService.getMe(anyString(), anyString()))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateAvatar(anyString(), anyString()))
                .thenThrow(new InternalServerException("Có lỗi trong quá trình cập nhật avatar"));

        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message").value("Có lỗi trong quá trình cập nhật avatar"));

        verify(userService, times(1)).getMe(anyString(), anyString());
        verify(userUsecase, times(1)).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 3: Xóa ảnh cũ khỏi S3 thất bại")
    void deleteOldFailure() throws Exception {
        when(userService.getMe(anyString(), anyString()))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateAvatar(anyString(), anyString()))
                .thenReturn(true);

        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userUsecase, times(1)).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 4: Update với S3Key rỗng")
    void emptyS3Key() throws Exception {
        UpdateAvatarRequestDTO empty = new UpdateAvatarRequestDTO("");

        when(userService.getMe(VALID_AT, VALID_RT))
                .thenReturn(userResponseDTO);
        when(userUsecase.updateAvatar(USER_ID, ""))
                .thenThrow(new BadRequestException("s3Key rỗng"));

        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empty))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("s3Key rỗng"));

        verify(userService, times(1)).getMe(anyString(), anyString());
        verify(userUsecase, times(1)).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 5: Update không có body")
    void noBody() throws Exception {
        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Case 6: Không gửi header Authorization")
    void notSendAuthorization() throws Exception {
        // When
        mockMvc.perform(put("/user/avatar")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Thiếu header Authorization"));

        // Then
        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 7: Header Authorization rỗng")
    void emptyAuthorization() throws Exception {
        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization rỗng"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 8: Header Authorization thiếu \"Bearer \" prefix")
    void missingAuthorization() throws Exception {
        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", VALID_AT)
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 9: Header Authorization chỉ có \"Bearer\"")
    void justBearer() throws Exception {
        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "Bearer")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateAvatar(anyString(), anyString());
    }

    @Test
    @DisplayName("Case 10: Header Authorization có AT rỗng")
    void emptyAT() throws Exception {
        mockMvc.perform(put("/user/avatar")
                        .header("Authorization", "Bearer ")
                        .cookie(new Cookie("accessToken", VALID_AT))
                        .cookie(new Cookie("refreshToken", VALID_RT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("AT rỗng"));

        verify(userService, never()).getMe(anyString(), anyString());
        verify(userUsecase, never()).updateAvatar(anyString(), anyString());
    }

}
