package com.wedu.exam_creation.user.controller;


import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.user.usecase.UserUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class UserControllerUpdateAvatarTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";

    private static final String USER_ID = "user-123";
    private static final String EMAIL = "user@gmail.com";
    private static final String OLD_S3KEY = "avatars/old-s3key.png";
    private static final String NEW_S3KEY = "avatars/new-s3key.png";


    @Autowired
    private MockMvc mockMvc;

    // Mock SecurityConfig
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    // Mock JwtAuthenticationFilter
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserUsecase userUsecase;

    private CommonUserResponseAllDTO mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new CommonUserResponseAllDTO(
                USER_ID,
                EMAIL,
                "hashed-password",
                "user123",
                "ROLE_TEACHER",
                "LOCAL",
                OLD_S3KEY,
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("200 - Update avatar thành công")
    void should_updateAvatarSuccessfully_when_requestIsValid() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateAvatar(USER_ID, NEW_S3KEY))
                .thenReturn(true);

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(true));

        verify(userUsecase).updateAvatar(USER_ID, NEW_S3KEY);
    }

    @Test
    @DisplayName("500 - Lưu thông tin người dùng thất bại")
    void should_returnInternalServerError_when_saveUserFails() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateAvatar(USER_ID, NEW_S3KEY))
                .thenThrow(new InternalServerException("Có lỗi trong quá trình cập nhật avatar"));

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message").value("Có lỗi trong quá trình cập nhật avatar"));

        verify(userUsecase).updateAvatar(USER_ID, NEW_S3KEY);
    }

    @Test
    @DisplayName("200 - Trả về false khi usecase không cập nhật avatar")
    void should_returnFalse_when_usecaseReturnsFalse() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateAvatar(USER_ID, NEW_S3KEY))
                .thenReturn(false);

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(false));

        verify(userUsecase).updateAvatar(USER_ID, NEW_S3KEY);
    }

    @Test
    @DisplayName("400 - Update với S3Key rỗng")
    void should_returnBadRequest_when_s3KeyIsEmpty() throws Exception {

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Usecase
        when(userUsecase.updateAvatar(USER_ID, ""))
                .thenThrow(new BadRequestException("s3Key rỗng"));

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", "")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("s3Key rỗng"));

        verify(userUsecase).updateAvatar(USER_ID, "");
    }

    @Test
    @DisplayName("400 - Thiếu request parameter s3Key")
    void should_returnBadRequest_when_s3KeyIsMissing() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - không có Authentication")
    void should_returnBadRequest_when_notAuthenticated() throws Exception {
        // When
        mockMvc.perform(patch("/user/update-avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        // Then
        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - Header Authorization rỗng")
    void should_returnBadRequest_when_authorizationIsEmpty() throws Exception {
        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - Authorization chỉ có \"Bearer\"")
    void should_returnBadRequest_when_authorizationHeaderContainsOnlyBearer() throws Exception {
        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("401 - Authorization có AT rỗng")
    void should_returnUnauthorized_when_accessTokenIsEmpty() throws Exception {
        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Không có AT"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("401 - AT sai định dạng")
    void should_returnUnauthorized_when_accessTokenIsNotValidJwt() throws Exception {
        doThrow(new UnAuthorizedException("Cấu trúc Access Token không hợp lệ"))
                .when(jwtTokenProvider).validateAccessToken(INVALID_AT);

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + INVALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Cấu trúc Access Token không hợp lệ"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("401 - AT hết hạn")
    void should_returnUnauthorized_when_accessTokenIsExpired() throws Exception {
        String expiredAT = "expired-at";

        doThrow(new UnAuthorizedException("Access Token đã hết hạn"))
                .when(jwtTokenProvider).validateAccessToken(expiredAT);

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + expiredAT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Access Token đã hết hạn"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("401 - AT bị chỉnh sửa")
    void should_returnUnauthorized_when_accessTokenIsModified() throws Exception {
        String editedAT = "edited-at";

        doThrow(new UnAuthorizedException("Chữ ký Access Token không chính xác"))
                .when(jwtTokenProvider).validateAccessToken(editedAT);

        mockMvc.perform(patch("/user/update-avatar")
                        .header("Authorization", "Bearer " + editedAT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("s3Key", NEW_S3KEY)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Chữ ký Access Token không chính xác"));

        verifyNoInteractions(userUsecase);
    }

}
