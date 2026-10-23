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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class UserControllerUpdateUsernameTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";

    private static final String USER_ID = "user-123";
    private static final String EMAIL = "u123@gmail.com";
    private static final String AVATAR_URL = "avatars/s3key.png";
    private static final String OLD_USERNAME = "old-username";
    private static final String NEW_USERNAME = "new-username-123";

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
                OLD_USERNAME,
                "ROLE_TEACHER",
                "LOCAL",
                AVATAR_URL,
                true,
                "FREE",
                Instant.now(),
                Instant.now()
        );

    }

    @Test
    @DisplayName("200 - Cập nhật username thành công")
    void should_updateUsernameSuccessfully_when_usernameIsValid() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, NEW_USERNAME))
                .thenReturn(true);

        // When
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(userUsecase).updateUsername(USER_ID, NEW_USERNAME);
    }

    @Test
    @DisplayName("400 - Cập nhật với username rỗng")
    void should_returnBadRequest_when_usernameIsEmpty() throws Exception {

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, ""))
                .thenThrow(new BadRequestException("Tên người dùng rỗng"));

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userUsecase).updateUsername(USER_ID, "");
    }

    @Test
    @DisplayName("400 - Cập nhật với username chỉ chứa khoảng trắng")
    void should_returnBadRequest_when_usernameContainsOnlyWhitespace() throws Exception {

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, "      "))
                .thenThrow(new BadRequestException("Tên người dùng rỗng"));

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "      ")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userUsecase).updateUsername(USER_ID, "      ");
    }

    @Test
    @DisplayName("400 - Cập nhật với username quá dài")
    void should_returnBadRequest_when_usernameExceedsMaxLength() throws Exception {
        String longUsername = "a".repeat(260);


        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, longUsername))
                .thenThrow(new BadRequestException("Tên người dùng quá dài"));

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", longUsername)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng quá dài"));

        verify(userUsecase).updateUsername(USER_ID, longUsername);
    }

    @Test
    @DisplayName("200 - Cập nhật với username là có số")
    void should_updateUsernameSuccessfully_when_usernameContainsDigits() throws Exception {


        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, "abc123"))
                .thenReturn(true);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "abc123")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(userUsecase).updateUsername(USER_ID, "abc123");
    }

    @Test
    @DisplayName("200 - Cập nhật với username là có emoji")
    void should_updateUsernameSuccessfully_when_usernameContainsEmojis() throws Exception {


        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, "Nguyen Van A \uD83D\uDE00"))
                .thenReturn(true);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "Nguyen Van A \uD83D\uDE00")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(userUsecase).updateUsername(USER_ID, "Nguyen Van A \uD83D\uDE00");
    }

    @Test
    @DisplayName("200 - Cập nhật với username là có unicode")
    void should_updateUsernameSuccessfully_when_usernameContainsUnicodeCharacters() throws Exception {


        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, "Nguyễn Văn A"))
                .thenReturn(true);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", "Nguyễn Văn A")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(userUsecase).updateUsername(USER_ID, "Nguyễn Văn A");
    }

    @Test
    @DisplayName("400 - không có Authentication")
    void should_returnBadRequest_when_notAuthenticated() throws Exception {
        // When
        mockMvc.perform(patch("/user/update-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        // Then
        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - Header Authorization rỗng")
    void should_returnBadRequest_when_authorizationIsEmpty() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - Authorization thiếu \"Bearer \" prefix")
    void should_returnBadRequest_when_authorizationIsMissing() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - Authorization chỉ có \"Bearer\"")
    void should_returnBadRequest_when_authorizationHeaderContainsOnlyBearer() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("401 - Authorization có AT rỗng")
    void should_returnUnauthorized_when_accessTokenIsEmpty() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
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

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + INVALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
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

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + expiredAT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
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

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + editedAT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Chữ ký Access Token không chính xác"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - Thiếu request parameter username")
    void should_returnBadRequest_when_usernameIsMissing() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("400 - JSON body không thay thế request parameter username")
    void should_returnBadRequest_when_usernameIsOnlyInJsonBody() throws Exception {
        String newBody = "{\"username\": \"Nguyen Van A\", \"role\": \"ROLE_ADMIN\"}";

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newBody)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("404 - AT hợp lệ nhưng userId trong payload không tồn tại trong DB")
    void should_returnNotFound_when_userIdInAccessTokenDoesNotExist() throws Exception {
        ATPayload payload = new ATPayload("parent-jti", "user-1", "anv@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("anv@gmail.com"))
                .thenThrow(new UsernameNotFoundException("Tài khoản chưa tồn tại"));

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tài khoản chưa tồn tại"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("403 - tài khoản đã bị khóa nhưng AT vẫn còn hiệu lực")
    void should_returnForbidden_when_accessTokenIsValidButUserIsLocked() throws Exception {
        CommonUserResponseAllDTO mockUserLock = new CommonUserResponseAllDTO(
                "user-A",
                "usera@gmail.com",
                "a-hashed-password",
                "User A Name",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/userA.png",
                false,
                "FREE",
                Instant.now(),
                Instant.now()
        );

        CustomUserDetails principal = new CustomUserDetails(mockUserLock);
        ATPayload payload = new ATPayload("parent-jti", "user-A", "usera@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("usera@gmail.com")).thenReturn(principal);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("message").value("Tài khoản đã bị khóa"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("405 - gọi API bằng method khác")
    void should_returnMethodNotAllowed_when_httpMethodIsNotSupported() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);

        // When
        mockMvc.perform(post("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("message").value("Phương thức HTTP không được hỗ trợ"));

        verifyNoInteractions(userUsecase);
    }

    @Test
    @DisplayName("500 - Lỗi trong quá trình cập nhật tài khoản")
    void should_returnInternalServerError_when_saveAccountFails() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, NEW_USERNAME))
                .thenThrow(new InternalServerException("Có lỗi trong quá trình cập nhật tài khoản"));

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message").value("Có lỗi trong quá trình cập nhật tài khoản"));

        verify(userUsecase).updateUsername(USER_ID, NEW_USERNAME);
    }

    @Test
    @DisplayName("200 - Trả về false khi usecase không cập nhật username")
    void should_returnFalse_when_usecaseReturnsFalse() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, NEW_USERNAME)).thenReturn(false);

        // When
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("username", NEW_USERNAME)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));

        // Then
        verify(userUsecase).updateUsername(USER_ID, NEW_USERNAME);
    }
}
