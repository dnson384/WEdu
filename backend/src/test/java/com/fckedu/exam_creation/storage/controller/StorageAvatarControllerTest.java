package com.fckedu.exam_creation.storage.controller;

import com.fckedu.exam_creation.common.exception.BadRequestException;
import com.fckedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.fckedu.exam_creation.storage.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(StorageController.class)
@AutoConfigureMockMvc(addFilters = false)
public class StorageAvatarControllerTest {
    private static final String VALID_AT = "mock-at";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        file = new MockMultipartFile(
                "file",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "test-image-content".getBytes()
        );
    }

    @Test
    @DisplayName("Case 1: Upload ảnh hợp lệ (jpg/png)")
    void success() throws Exception {
        when(s3Service.uploadFile(any(), eq("avatars"), eq(VALID_AT)))
                .thenReturn("avatars/avatar-uuid.jpg");

        mockMvc.perform(multipart("/storage/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("avatars/avatar-uuid.jpg"));

        verify(s3Service, times(1)).uploadFile(file, "avatars", VALID_AT);
    }

    @Test
    @DisplayName("Case 2: Không gửi header Authorization")
    void notSendAuthorization() throws Exception {
        // When
        mockMvc.perform(multipart("/storage/avatar")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Thiếu header Authorization"));

        // Then
        verify(s3Service, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Case 3: Header Authorization rỗng")
    void emptyAuthorization() throws Exception {
        mockMvc.perform(multipart("/storage/avatar")
                        .file(file)
                        .header("Authorization", "")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization rỗng"));

        verify(s3Service, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Case 4: Header Authorization thiếu \"Bearer \" prefix")
    void missingAuthorization() throws Exception {
        mockMvc.perform(multipart("/storage/avatar")
                        .file(file)
                        .header("Authorization", VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(s3Service, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Case 5: Header Authorization chỉ có \"Bearer\"")
    void justBearer() throws Exception {
        mockMvc.perform(multipart("/storage/avatar")
                        .file(file)
                        .header("Authorization", "Bearer")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Authorization sai định dạng Bearer"));

        verify(s3Service, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Case 6: Header Authorization có AT rỗng")
    void emptyAT() throws Exception {
        mockMvc.perform(multipart("/storage/avatar")
                        .file(file)
                        .header("Authorization", "Bearer ")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("AT rỗng"));

        verify(s3Service, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Case 7: Upload khi S3 lỗi")
    void uploadFailure() throws Exception {
        when(s3Service.uploadFile(any(), eq("avatars"), eq(VALID_AT)))
                .thenThrow(new IOException("S3 connection timeout"));

        // When & Then
        mockMvc.perform(multipart("/storage/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Upload avatar thất bại: S3 connection timeout"));
    }


    @Test
    @DisplayName("Case 8: Upload file rỗng")
    void emptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        when(s3Service.uploadFile(any(), eq("avatars"), eq(VALID_AT)))
                .thenThrow(new BadRequestException("Lỗi: Đầu vào là file trống"));

        // When & Then
        mockMvc.perform(multipart("/storage/avatar")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lỗi: Đầu vào là file trống"));

        verify(s3Service, times(1)).uploadFile(emptyFile, "avatars", VALID_AT);
    }

    @Test
    @DisplayName("Case 9: Upload file lớn hon 5MB")
    void moreThan5MBFile() throws Exception {
        int largeSize = 5 * 1024 * 1024 + 1;
        MockMultipartFile moreThan5MB = new MockMultipartFile(
                "file",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[largeSize]
        );

        when(s3Service.uploadFile(any(), eq("avatars"), eq(VALID_AT)))
                .thenThrow(new BadRequestException("Lỗi: File lớn hơn 5MB"));

        // When & Then
        mockMvc.perform(multipart("/storage/avatar")
                        .file(moreThan5MB)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lỗi: File lớn hơn 5MB"));

        verify(s3Service, times(1)).uploadFile(moreThan5MB, "avatars", VALID_AT);
    }

    @Test
    @DisplayName("Case 10: Upload file đúng 5MB")
    void exact5MBFile() throws Exception {
        int largeSize = 5 * 1024 * 1024;
        MockMultipartFile exact5MB = new MockMultipartFile(
                "file",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[largeSize]
        );

        when(s3Service.uploadFile(any(), eq("avatars"), eq(VALID_AT)))
                .thenReturn("avatars/avatar-uuid.jpg");

        // When & Then
        mockMvc.perform(multipart("/storage/avatar")
                        .file(exact5MB)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("avatars/avatar-uuid.jpg"));

        verify(s3Service, times(1)).uploadFile(exact5MB, "avatars", VALID_AT);
    }

    @Test
    @DisplayName("Case 11: Upload file giả dạng ảnh")
    void fakeImgFile() throws Exception {
        byte[] exeContent = "MZ_fake_binary_executable_content_here".getBytes();

        MockMultipartFile fakeImageFile = new MockMultipartFile(
                "file",
                "hacker.png",
                MediaType.IMAGE_PNG_VALUE,
                exeContent
        );

        when(s3Service.uploadFile(any(), eq("avatars"), eq(VALID_AT)))
                .thenThrow(new BadRequestException("Lỗi: File không đúng định dạng"));

        mockMvc.perform(multipart("/storage/avatar")
                        .file(fakeImageFile)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lỗi: File không đúng định dạng"));

        verify(s3Service, times(1)).uploadFile(fakeImageFile, "avatars", VALID_AT);
    }

    @Test
    @DisplayName("Case 12: Upload file ảnh thật nhưng đổi extension thành đuôi lạ")
    void strangeExtensionFile() throws Exception {
        MockMultipartFile strangeExtensionFile = new MockMultipartFile(
                "file",
                "avatar.abc",
                MediaType.IMAGE_PNG_VALUE,
                "test-image-content".getBytes()
        );

        when(s3Service.uploadFile(any(), eq("avatars"), eq(VALID_AT)))
                .thenReturn("avatars/avatar-uuid.jpg");

        mockMvc.perform(multipart("/storage/avatar")
                        .file(strangeExtensionFile)
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("avatars/avatar-uuid.jpg"));

        verify(s3Service, times(1)).uploadFile(strangeExtensionFile, "avatars", VALID_AT);
    }

    @Test
    @DisplayName("Case 13: Gửi req không kèm file")
    void nonBody() throws Exception {
        mockMvc.perform(post("/storage/avatar")
                        .header("Authorization", "Bearer " + VALID_AT)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value("Thiếu file đầu vào"));

        verify(s3Service, times(0)).uploadFile(any(), anyString(), anyString());
    }
}
