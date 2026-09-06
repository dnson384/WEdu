package com.wedu.exam_creation.user.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
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
import com.wedu.exam_creation.user.dto.request.UpdateUserRequestDTO;
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

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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


    private static final String PRESIGNED_AVATAR = "https://s3.amazonaws.com/my-pic-presigned.png";
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    private CommonUserResponseDTO mockUserRes;

    private UpdateUserRequestDTO validRequest() {
        UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
        dto.setUsername(NEW_USERNAME);
        return dto;
    }

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
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        mockUserRes = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                NEW_USERNAME,
                "ROLE_TEACHER",
                PRESIGNED_AVATAR,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
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
                .thenReturn(mockUserRes);

        // When
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk());

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Cập nhật với username rỗng")
    void should_returnBadRequest_when_usernameIsEmpty() throws Exception {
        UpdateUserRequestDTO empty = new UpdateUserRequestDTO("");

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
                        .content(objectMapper.writeValueAsString(empty))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Cập nhật với username null")
    void should_returnBadRequest_when_usernameIsNull() throws Exception {
        UpdateUserRequestDTO nullUsername = new UpdateUserRequestDTO(null);

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, null))
                .thenThrow(new BadRequestException("Tên người dùng rỗng"));

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullUsername))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userUsecase, times(1)).updateUsername(anyString(), isNull());
    }

    @Test
    @DisplayName("400 - Cập nhật với username chỉ chứa khoảng trắng")
    void should_returnBadRequest_when_usernameContainsOnlyWhitespace() throws Exception {
        UpdateUserRequestDTO onlySpace = new UpdateUserRequestDTO("      ");

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
                        .content(objectMapper.writeValueAsString(onlySpace))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng rỗng"));

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Cập nhật với username quá dài")
    void should_returnBadRequest_when_usernameExceedsMaxLength() throws Exception {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < 260; i++) {
            int index = (int) (Math.random() * characters.length());
            str.append(characters.charAt(index));
        }

        String longUsername = str.toString();
        UpdateUserRequestDTO longUsernamePayload = new UpdateUserRequestDTO(longUsername);


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
                        .content(objectMapper.writeValueAsString(longUsernamePayload))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Tên người dùng quá dài"));

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("200 - Cập nhật với username là có số")
    void should_updateUsernameSuccessfully_when_usernameContainsDigits() throws Exception {
        UpdateUserRequestDTO hasNum = new UpdateUserRequestDTO("abc123");

        CommonUserResponseDTO mockNumRes = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                "abc123",
                "ROLE_TEACHER",
                AVATAR_URL,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now());

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, "abc123"))
                .thenReturn(mockNumRes);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hasNum))
                )
                .andExpect(status().isOk());

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("200 - Cập nhật với username là có emoji")
    void should_updateUsernameSuccessfully_when_usernameContainsEmojis() throws Exception {
        UpdateUserRequestDTO emoji = new UpdateUserRequestDTO("Nguyen Van A \uD83D\uDE00");

        CommonUserResponseDTO mockEmojiRes = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                "Nguyen Van A \uD83D\uDE00",
                "ROLE_TEACHER",
                AVATAR_URL,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, "Nguyen Van A \uD83D\uDE00"))
                .thenReturn(mockEmojiRes);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emoji))
                )
                .andExpect(status().isOk());

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("200 - Cập nhật với username là có unicode")
    void should_updateUsernameSuccessfully_when_usernameContainsUnicodeCharacters() throws Exception {
        UpdateUserRequestDTO unicode = new UpdateUserRequestDTO("Nguyễn Văn A");

        CommonUserResponseDTO mockUnicodeCommon = new CommonUserResponseDTO(
                USER_ID,
                EMAIL,
                "Nguyễn Văn A",
                "ROLE_TEACHER",
                AVATAR_URL,
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, "Nguyễn Văn A"))
                .thenReturn(mockUnicodeCommon);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unicode))
                )
                .andExpect(status().isOk());

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - không có Authentication")
    void should_returnBadRequest_when_notAuthenticated() throws Exception {
        // When
        mockMvc.perform(patch("/user/update-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        // Then
        verify(userUsecase, never()).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Header Authorization rỗng")
    void should_returnBadRequest_when_authorizationIsEmpty() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Authorization thiếu \"Bearer \" prefix")
    void should_returnBadRequest_when_authorizationIsMissing() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Authorization chỉ có \"Bearer\"")
    void should_returnBadRequest_when_authorizationHeaderContainsOnlyBearer() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("401 - Authorization có AT rỗng")
    void emptyAT() throws Exception {
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Không có AT"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("401 - AT sai định dạng")
    void should_returnUnauthorized_when_accessTokenIsNotValidJwt() throws Exception {
        doThrow(new UnAuthorizedException("Cấu trúc Access Token không hợp lệ"))
                .when(jwtTokenProvider).validateAccessToken(INVALID_AT);

        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + INVALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Cấu trúc Access Token không hợp lệ"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
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
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Access Token đã hết hạn"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("401 - AT bị chỉnh sửa")
    void should_returnUnauthorized_when_accessTokenIsModified() throws Exception {
        String editedAT = "edited-at";

        doThrow(new UnAuthorizedException("Chữ ký Access Token không chính xác"))
                .when(jwtTokenProvider).validateAccessToken(editedAT);

        mockMvc.perform(get("/user/update-username")
                        .header("Authorization", "Bearer " + editedAT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Chữ ký Access Token không chính xác"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Body rỗng")
    void should_returnBadRequest_when_requestBodyIsEmpty() throws Exception {
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

        verify(userUsecase, times(0)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("400 - Payload lạ")
    void should_returnBadRequest_when_payloadIsInvalid() throws Exception {
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

        verify(userUsecase, times(0)).updateUsername(anyString(), anyString());
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
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tài khoản chưa tồn tại"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
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
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CustomUserDetails principal = new CustomUserDetails(mockUserLock);
        ATPayload payload = new ATPayload("parent-jti", "user-A", "usera@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("usera@gmail.com")).thenReturn(principal);

        mockMvc.perform(get("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("message").value("Tài khoản đã bị khóa"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
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
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("message").value("Phương thức HTTP không được hỗ trợ"));

        verify(userUsecase, never()).updateUsername(anyString(), anyString());
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
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("message").value("Có lỗi trong quá trình cập nhật tài khoản"));

        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }

    @Test
    @DisplayName("200 - Kiểm tra response không lộ thông tin nhạy cảm")
    void should_returnSuccessWithoutSensitiveInformation_when_accessTokenIsValid() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(userUsecase.updateUsername(USER_ID, NEW_USERNAME)).thenReturn(mockUserRes);

        // When
        mockMvc.perform(patch("/user/update-username")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("hashedPassword").doesNotExist());

        // Then
        verify(userUsecase, times(1)).updateUsername(anyString(), anyString());
    }
}
