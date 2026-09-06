package com.wedu.exam_creation.storage.controller;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(StorageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class StorageAvatarControllerTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";

    private static final String USER_ID = "user-123";
    private static final String EMAIL = "anv@gmail.com";

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
    private S3Service s3Service;

    private CommonUserResponseAllDTO mockUser;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        mockUser = new CommonUserResponseAllDTO(
                USER_ID,
                EMAIL,
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

        file = new MockMultipartFile(
                "file",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "test-image-content".getBytes()
        );
    }

    @Test
    @DisplayName("200 - Upload ảnh hợp lệ (jpg/png)")
    void should_uploadImageSuccessfully_when_imageFormatIsValid() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(s3Service.uploadFile(any(), eq("avatars")))
                .thenReturn("avatars/avatar-uuid.jpg");

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("avatars/avatar-uuid.jpg"));

        verify(s3Service, times(1)).uploadFile(file, "avatars");
    }

    @Test
    @DisplayName("400 - không có Authentication")
    void should_returnBadRequest_when_notAuthenticated() throws Exception {
        // When
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        // Then
        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("400 - Header Authorization rỗng")
    void should_returnBadRequest_when_authorizationIsEmpty() throws Exception {
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("400 - Authorization thiếu \"Bearer \" prefix")
    void should_returnBadRequest_when_authorizationIsMissing() throws Exception {
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("400 - Authorization chỉ có \"Bearer\"")
    void should_returnBadRequest_when_authorizationHeaderContainsOnlyBearer() throws Exception {
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Header Authorization sai định dạng"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("401 - Authorization có AT rỗng")
    void should_returnUnauthorized_when_accessTokenIsEmpty() throws Exception {
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer ")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Không có AT"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("401 - AT sai định dạng")
    void should_returnUnauthorized_when_accessTokenIsNotValidJwt() throws Exception {
        doThrow(new UnAuthorizedException("Cấu trúc Access Token không hợp lệ"))
                .when(jwtTokenProvider).validateAccessToken(INVALID_AT);

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + INVALID_AT)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Cấu trúc Access Token không hợp lệ"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("401 - AT hết hạn")
    void should_returnUnauthorized_when_accessTokenIsExpired() throws Exception {
        String expiredAT = "expired-at";

        doThrow(new UnAuthorizedException("Access Token đã hết hạn"))
                .when(jwtTokenProvider).validateAccessToken(expiredAT);

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + expiredAT)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Access Token đã hết hạn"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("401 - AT bị chỉnh sửa")
    void should_returnUnauthorized_when_accessTokenIsModified() throws Exception {
        String editedAT = "edited-at";

        doThrow(new UnAuthorizedException("Chữ ký Access Token không chính xác"))
                .when(jwtTokenProvider).validateAccessToken(editedAT);

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + editedAT)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("message").value("Chữ ký Access Token không chính xác"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("404 - AT hợp lệ nhưng userId trong payload không tồn tại trong DB")
    void should_returnNotFound_when_userIdInAccessTokenDoesNotExist() throws Exception {
        ATPayload payload = new ATPayload("parent-jti", "user-0", "notexisted@gmail.com", "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername("notexisted@gmail.com"))
                .thenThrow(new UsernameNotFoundException("Tài khoản chưa tồn tại"));

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message").value("Tài khoản chưa tồn tại"));

        verify(s3Service, never()).uploadFile(any(), anyString());
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

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("message").value("Tài khoản đã bị khóa"));

        verify(s3Service, never()).uploadFile(any(), anyString());
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
        mockMvc.perform(get("/storage/upload-avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("message").value("Phương thức HTTP không được hỗ trợ"));

        verify(s3Service, never()).uploadFile(file, "avatars");
    }

    @Test
    @DisplayName("500 - Upload khi S3 lỗi")
    void should_returnInternalServerError_when_s3ServiceFails() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", USER_ID, EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(s3Service.uploadFile(eq(file), eq("avatars")))
                .thenThrow(new IOException("S3 connection timeout"));

        // When & Then
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Upload avatar thất bại: S3 connection timeout"));

        verify(s3Service, times(1)).uploadFile(any(), anyString());
    }


    @Test
    @DisplayName("400 - Upload file rỗng")
    void should_returnBadRequest_when_uploadedFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(s3Service.uploadFile(any(), eq("avatars")))
                .thenThrow(new BadRequestException("Lỗi: Đầu vào là file trống"));

        // When & Then
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lỗi: Đầu vào là file trống"));

        verify(s3Service, times(1)).uploadFile(emptyFile, "avatars");
    }

    @Test
    @DisplayName("400 - Upload file lớn hon 5MB")
    void should_returnBadRequest_when_fileSizeExceedsLimit() throws Exception {
        int largeSize = 5 * 1024 * 1024 + 1;
        MockMultipartFile moreThan5MB = new MockMultipartFile(
                "file",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[largeSize]
        );

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(s3Service.uploadFile(any(), eq("avatars")))
                .thenThrow(new BadRequestException("Lỗi: File lớn hơn 5MB"));

        // When & Then
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(moreThan5MB)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lỗi: File lớn hơn 5MB"));

        verify(s3Service, times(1)).uploadFile(moreThan5MB, "avatars");
    }

    @Test
    @DisplayName("200 - Upload file đúng 5MB")
    void should_uploadImageSuccessfully_when_fileSizeIsExactlyFiveMegabytes() throws Exception {
        int largeSize = 5 * 1024 * 1024;
        MockMultipartFile exact5MB = new MockMultipartFile(
                "file",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[largeSize]
        );

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(s3Service.uploadFile(any(), eq("avatars")))
                .thenReturn("avatars/avatar-uuid.jpg");

        // When & Then
        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(exact5MB)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("avatars/avatar-uuid.jpg"));

        verify(s3Service, times(1)).uploadFile(exact5MB, "avatars");
    }

    @Test
    @DisplayName("400 - Upload file giả dạng ảnh")
    void should_returnBadRequest_when_fileIsSpoofedImage() throws Exception {
        byte[] exeContent = "MZ_fake_binary_executable_content_here".getBytes();

        MockMultipartFile fakeImageFile = new MockMultipartFile(
                "file",
                "hacker.png",
                MediaType.IMAGE_PNG_VALUE,
                exeContent
        );

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(s3Service.uploadFile(any(), eq("avatars")))
                .thenThrow(new BadRequestException("Lỗi: File không đúng định dạng"));

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(fakeImageFile)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lỗi: File không đúng định dạng"));

        verify(s3Service, times(1)).uploadFile(fakeImageFile, "avatars");
    }

    @Test
    @DisplayName("200 - Upload file ảnh thật nhưng đổi extension thành đuôi lạ")
    void should_uploadImageSuccessfully_when_validImageHasUnknownExtension() throws Exception {
        MockMultipartFile strangeExtensionFile = new MockMultipartFile(
                "file",
                "avatar.abc",
                MediaType.IMAGE_PNG_VALUE,
                "test-image-content".getBytes()
        );

        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);
        // 2. Use case
        when(s3Service.uploadFile(any(), eq("avatars")))
                .thenReturn("avatars/avatar-uuid.jpg");

        mockMvc.perform(multipart("/storage/upload-avatar")
                        .file(strangeExtensionFile)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("avatars/avatar-uuid.jpg"));

        verify(s3Service, times(1)).uploadFile(strangeExtensionFile, "avatars");
    }

    @Test
    @DisplayName("400 - Gửi req không kèm file")
    void should_returnBadRequest_when_fileIsMissing() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(mockUser);
        ATPayload payload = new ATPayload("parent-jti", "user-1", EMAIL, "ROLE_TEACHER");

        // Given
        // 1. Sec
        doNothing().when(jwtTokenProvider).validateAccessToken(VALID_AT);
        when(jwtTokenProvider.getPayloadFromAccessToken(VALID_AT)).thenReturn(payload);
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(principal);

        mockMvc.perform(post("/storage/upload-avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Thiếu file đầu vào"));

        verify(s3Service, never()).uploadFile(any(), anyString());
    }
}
