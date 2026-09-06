package com.wedu.exam_creation.storage.service;

import com.wedu.exam_creation.common.exception.BadRequestException;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorageAvatarServiceTest {
    private final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png");

    @Mock
    private S3Client s3Client;

    @Mock
    private Tika tika;

    @Mock
    private S3Presigner s3Presigner;

    private S3Service s3Service;

    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        String bucketName = "my-test-bucket";
        s3Service = new S3Service(s3Client, s3Presigner, bucketName);

        ReflectionTestUtils.setField(s3Service, "tika", tika);
        ReflectionTestUtils.setField(s3Service, "ALLOWED_MIME_TYPES", ALLOWED_MIME_TYPES);

        validFile = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                "valid-image-content-bytes".getBytes()
        );
    }

    @Test
    @DisplayName("200 - Tải avatar lên thành công")
    void should_uploadAvatarSuccessfully_when_fileIsValid() throws Exception {
        when(tika.detect(any(InputStream.class))).thenReturn("image/png");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String s3Key = s3Service.uploadFile(validFile, "avatars");

        assertNotNull(s3Key);
        assertTrue(s3Key.startsWith("avatars/"));

        verify(tika, times(1)).detect(any(InputStream.class));
        verify(s3Client, times(1))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("400 - File rỗng")
    void should_returnBadRequest_when_fileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        assertThatThrownBy(() -> s3Service.uploadFile(emptyFile, "avatars"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: Đầu vào là file trống");

        verify(tika, never()).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("400 - File lớn hơn 5MB")
    void should_returnBadRequest_when_fileSizeExceedsLimit() throws Exception {
        int largeSize = 5 * 1024 * 1024 + 1;
        MockMultipartFile moreThan5MB = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[largeSize]
        );

        assertThatThrownBy(() -> s3Service.uploadFile(moreThan5MB, "avatars"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: File lớn hơn 5MB");

        verify(tika, never()).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("400 - File .exe giả dạng file .png")
    void should_returnBadRequest_when_fileContentIsNotRealImage() throws Exception {
        byte[] exeContent = "MZ_fake_binary_executable_content_here".getBytes();

        MockMultipartFile fakeImageFile = new MockMultipartFile(
                "file",
                "hacker.png",
                MediaType.IMAGE_PNG_VALUE,
                exeContent
        );

        when(tika.detect(any(InputStream.class)))
                .thenReturn("application/x-msdownload");

        assertThatThrownBy(() -> s3Service.uploadFile(fakeImageFile, "avatars"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: File không đúng định dạng");

        verify(tika, times(1)).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("200 - Đuôi file lạ nhưng vẫn là ảnh image")
    void should_uploadAvatarSuccessfully_when_fileExtensionIsUnknownButContentIsImage() throws Exception {
        MockMultipartFile strangeExtensionFile = new MockMultipartFile(
                "file",
                "avatar.abc",
                MediaType.IMAGE_PNG_VALUE,
                "test-image-content".getBytes()
        );

        when(tika.detect(any(InputStream.class)))
                .thenReturn("image/png");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String s3Key = s3Service.uploadFile(strangeExtensionFile, "avatars");

        assertNotNull(s3Key);
        assertTrue(s3Key.startsWith("avatars/"));
        assertTrue(s3Key.endsWith(".abc"));

        verify(tika, times(1)).detect(any(InputStream.class));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("500 - AWS S3 gặp sự cố kết nối")
    void should_returnInternalServerError_when_s3ConnectionFails() throws IOException {
        when(tika.detect(any(InputStream.class))).thenReturn("image/png");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 Connection Refused"));

        assertThrows(RuntimeException.class, () -> {
            s3Service.uploadFile(validFile, "avatars");
        });
    }
}
