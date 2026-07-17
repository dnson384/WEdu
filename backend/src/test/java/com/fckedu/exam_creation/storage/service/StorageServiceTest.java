package com.fckedu.exam_creation.storage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.common.exception.BadRequestException;
import com.fckedu.exam_creation.common.exception.UnAuthorizedException;
import com.fckedu.exam_creation.security.service.SecurityService;
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
public class StorageServiceTest {
    private static final String VALID_AT = "mock-at";
    private static final String INVALID_AT = "invalid-at";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String bucketName = "my-test-bucket";
    private List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png");

    @Mock
    private S3Client s3Client;

    @Mock
    private Tika tika;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private SecurityService securityService;

    private S3Service s3Service;

    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, s3Presigner, bucketName, securityService);

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
    @DisplayName("AT Null - Ném UnAuthorizedException")
    void nullAT() throws Exception {
        assertThatThrownBy(() -> s3Service.uploadFile(validFile, "avatars", null))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("AT không hợp lệ");

        verify(securityService, never()).validateAccessToken(anyString());
        verify(tika, never()).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("AT không hợp lệ")
    void invalidAT() throws Exception {
        when(securityService.validateAccessToken(INVALID_AT))
                .thenThrow(new UnAuthorizedException("AT không hợp lệ"));

        assertThatThrownBy(() -> s3Service.uploadFile(validFile, "avatars", INVALID_AT))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("AT không hợp lệ");

        verify(securityService, times(1)).validateAccessToken(INVALID_AT);
        verify(tika, never()).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }


    @Test
    @DisplayName("File rỗng")
    void emptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);

        assertThatThrownBy(() -> s3Service.uploadFile(emptyFile, "avatars", VALID_AT))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: Đầu vào là file trống");

        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(tika, never()).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("File lớn hơn 5MB")
    void moreThan5MBFile() throws Exception {
        int largeSize = 5 * 1024 * 1024 + 1;
        MockMultipartFile moreThan5MB = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[largeSize]
        );

        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);

        assertThatThrownBy(() -> s3Service.uploadFile(moreThan5MB, "avatars", VALID_AT))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: File lớn hơn 5MB");

        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(tika, never()).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("File .exe giả dạng file .png")
    void fakeImgFile() throws Exception {
        byte[] exeContent = "MZ_fake_binary_executable_content_here".getBytes();

        MockMultipartFile fakeImageFile = new MockMultipartFile(
                "file",
                "hacker.png",
                MediaType.IMAGE_PNG_VALUE,
                exeContent
        );

        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);
        when(tika.detect(any(InputStream.class)))
                .thenReturn("application/x-msdownload");

        assertThatThrownBy(() -> s3Service.uploadFile(fakeImageFile, "avatars", VALID_AT))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: File không đúng định dạng");

        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(tika, times(1)).detect(any(InputStream.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Đuôi file lạ nhưng vẫn là ảnh image")
    void strangeExtensionFile() throws Exception {
        MockMultipartFile strangeExtensionFile = new MockMultipartFile(
                "file",
                "avatar.abc",
                MediaType.IMAGE_PNG_VALUE,
                "test-image-content".getBytes()
        );


        when(securityService.validateAccessToken(VALID_AT))
                .thenReturn(true);
        when(tika.detect(any(InputStream.class)))
                .thenReturn("image/png");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String s3Key = s3Service.uploadFile(strangeExtensionFile, "avatars", VALID_AT);

        assertNotNull(s3Key);
        assertTrue(s3Key.startsWith("avatars/"));
        assertTrue(s3Key.endsWith(".abc"));

        verify(securityService, times(1)).validateAccessToken(VALID_AT);
        verify(tika, times(1)).detect(any(InputStream.class));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("AWS S3 Gặp Sự Cố Kết Nối - Ném IOException")
    void s3ServerError() throws IOException {
        when(securityService.validateAccessToken(VALID_AT)).thenReturn(true);
        when(tika.detect(any(InputStream.class))).thenReturn("image/png");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 Connection Refused"));

        assertThrows(RuntimeException.class, () -> {
            s3Service.uploadFile(validFile, "avatars", VALID_AT);
        });
    }

}
