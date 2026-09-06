package com.wedu.exam_creation.user.controller;


import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
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

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class UserControllerMeTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";

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

    @BeforeEach
    void setUp() {
        mockUser = new CommonUserResponseAllDTO(
                "user-123",
                "anv@gmail.com",
                "hashed-password",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/default-avatar-user.png",
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        mockUserRes = new CommonUserResponseDTO(
                "user-123",
                "anv@gmail.com",
                "Nguyen Van A",
                "ROLE_TEACHER",
                "avatars/default-avatar-user.png",
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("200 - thông tin user khi đã xác thực")
    void should_returnUserInfo_when_authenticated() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", "anv@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("anv@gmail.com")).thenReturn(principal);
        // 2. Use case
        when(userUsecase.getMe(mockUser)).thenReturn(mockUserRes);

        // When
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType("application/json"))
                .andExpect(status().isOk());

        // Then
        verify(userUsecase, times(1)).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("400 - không có Authentication")
    void should_returnBadRequest_when_notAuthenticated() throws Exception {
        // When
        mockMvc.perform(get("/user/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        // Then
        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("400 - Header Authorization rỗng")
    void should_returnBadRequest_when_authorizationIsEmpty() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("400 - Authorization thiếu \"Bearer \" prefix")
    void should_returnBadRequest_when_authorizationIsMissing() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("400 - Authorization chỉ có \"Bearer\"")
    void should_returnBadRequest_when_authorizationHeaderContainsOnlyBearer() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("401 - Authorization có AT rỗng")
    void should_returnUnauthorized_when_accessTokenIsEmpty() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Không có AT"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("401 - AT sai định dạng")
    void should_returnUnauthorized_when_accessTokenIsNotValidJwt() throws Exception {
        doThrow(new UnAuthorizedException("Cấu trúc Access Token không hợp lệ"))
                .when(jwtTokenProvider).validateAccessToken(INVALID_AT);

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + INVALID_AT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Cấu trúc Access Token không hợp lệ"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("401 - AT hết hạn")
    void should_returnUnauthorized_when_accessTokenIsExpired() throws Exception {
        String expiredAT = "expired-at";

        doThrow(new UnAuthorizedException("Access Token đã hết hạn"))
                .when(jwtTokenProvider).validateAccessToken(expiredAT);

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + expiredAT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Access Token đã hết hạn"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("401 - AT bị chỉnh sửa")
    void should_returnUnauthorized_when_accessTokenIsModified() throws Exception {
        String editedAT = "edited-at";

        doThrow(new UnAuthorizedException("Chữ ký Access Token không chính xác"))
                .when(jwtTokenProvider).validateAccessToken(editedAT);

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + editedAT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Chữ ký Access Token không chính xác"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
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

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message").value("Tài khoản chưa tồn tại"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
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

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("message").value("Tài khoản đã bị khóa"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("405 - gọi API bằng method khác")
    void should_returnMethodNotAllowed_when_httpMethodIsNotSupported() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", "anv@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("anv@gmail.com")).thenReturn(principal);
        // 2. Use case
        when(userUsecase.getMe(mockUser)).thenReturn(mockUserRes);

        // When
        mockMvc.perform(post("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("message").value("Phương thức HTTP không được hỗ trợ"));

        verify(userUsecase, never()).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("200 - Kiểm tra response không lộ thông tin nhạy cảm")
    void should_returnSuccessWithoutSensitiveInformation_when_accessTokenIsValid() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", "anv@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("anv@gmail.com")).thenReturn(principal);
        // 2. Use case
        when(userUsecase.getMe(mockUser)).thenReturn(mockUserRes);

        // When
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("hashedPassword").doesNotExist());

        // Then
        verify(userUsecase, times(1)).getMe(any(CommonUserResponseAllDTO.class));
    }

    @Test
    @DisplayName("200 - truyền AT của User A phải trả về đúng dữ liệu của User A, không lộn sang User B")
    void should_returnTokenOwnerData_when_accessTokenIsValid() throws Exception {
        CommonUserResponseAllDTO mockUserA = new CommonUserResponseAllDTO(
                "user-A",
                "usera@gmail.com",
                "a-hashed-password",
                "User A Name",
                "ROLE_TEACHER",
                "LOCAL",
                "avatars/userA.png",
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CustomUserDetails principalA = new CustomUserDetails(mockUserA);

        CommonUserResponseDTO responseUserA = new CommonUserResponseDTO(
                "user-A",
                "usera@gmail.com",
                "User A Name",
                "ROLE_TEACHER",
                "avatars/userA.png",
                true,
                "FREE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        String VALID_AT_A = "valid-at-a";
        ATPayload payload = new ATPayload("parent-jti", "user-A", "usera@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT_A)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("usera@gmail.com")).thenReturn(principalA);

        when(userUsecase.getMe(argThat(u -> u.getId().equals("user-A"))))
                .thenReturn(responseUserA);

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + VALID_AT_A)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value("user-A"))
                .andExpect(jsonPath("username").value("User A Name"))
                .andExpect(jsonPath("$.email").value("usera@gmail.com"));

        verify(userUsecase, times(1)).getMe(
                argThat(u ->
                        u.getId().equals("user-A")
                ));
        verify(userUsecase, never()).getMe(argThat(u -> u.getId().equals("user-B")));
    }
}
