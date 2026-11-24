package com.wedu.exam_creation.storage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ContentDisposition;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3ServiceTest {
    private static final String DOCX_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private final S3Client client = mock(S3Client.class);
    private final S3Presigner signer = mock(S3Presigner.class);
    private final S3Service service = new S3Service(client, signer, "test-bucket");

    @Test
    @DisplayName("trả về khóa đã chuẩn hóa và gửi đúng MIME DOCX khi tải tài liệu Word hợp lệ")
    void test_updateDocumentFile_001() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "Đề thi.DOCX", "application/octet-stream", wordPackage());
        String key =
                service.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com");
        assertThat(key).isEqualTo("question-files/math/teacher@example.com-DeThi.docx");
        verify(client)
                .putObject(
                        argThat(
                                (PutObjectRequest request) ->
                                        request.bucket().equals("test-bucket")
                                                && request.key().equals(key)
                                                && request.contentType().equals(DOCX_TYPE)
                                                && "*".equals(request.ifNoneMatch())),
                        any(RequestBody.class));
    }

    @Test
    @DisplayName(
            "trả về khóa có hậu tố tăng dần và giữ nội dung cũ khi tên tài liệu trùng sau chuẩn hóa")
    void test_updateDocumentFile_002() throws Exception {
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        simulateConditionalWrites(objects, null);
        String base = "question-files/physics/teacher@example.com-DeVatLy";
        String[] names = {"Đề vật lý.docx", "DeVatLy.docx", "ĐỀ-VẬT_LÝ.DOCX"};
        List<byte[]> payloads = new ArrayList<byte[]>();
        for (int index = 0; index < names.length; index++) {
            byte[] bytes = wordPackage("document " + index);
            payloads.add(bytes);
            MockMultipartFile file = new MockMultipartFile("file", names[index], DOCX_TYPE, bytes);
            String expectedKey = base + (index == 0 ? "" : "(" + index + ")") + ".docx";
            assertThat(
                            service.updateDocumentFile(
                                    file, "question-files/physics", 50, "teacher@example.com"))
                    .isEqualTo(expectedKey);
            assertThat(objects.get(expectedKey)).isEqualTo(bytes);
        }
        assertThat(objects).hasSize(3);
        assertThat(objects.get(base + ".docx")).isEqualTo(payloads.get(0));
        assertThat(objects.get(base + "(1).docx")).isEqualTo(payloads.get(1));
        verify(client, times(6)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("trả về khóa với hậu tố trống đầu tiên khi trùng tên trong cùng thư mục và email")
    void test_updateDocumentFile_003() throws Exception {
        String base = "question-files/physics/teacher@example.com-DeVatLy";
        byte[] original = wordPackage("original");
        byte[] second = wordPackage("second");
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        objects.put(base + ".docx", original);
        objects.put(base + "(2).docx", second);
        simulateConditionalWrites(objects, null);
        byte[] bytes = wordPackage("new");
        MockMultipartFile file = new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, bytes);

        assertThat(
                        service.updateDocumentFile(
                                file, "question-files/physics", 50, "teacher@example.com"))
                .isEqualTo(base + "(1).docx");
        assertThat(
                        service.updateDocumentFile(
                                file, "question-files/other", 50, "teacher@example.com"))
                .isEqualTo("question-files/other/teacher@example.com-DeVatLy.docx");
        assertThat(
                        service.updateDocumentFile(
                                file, "question-files/physics", 50, "other@example.com"))
                .isEqualTo("question-files/physics/other@example.com-DeVatLy.docx");
        assertThat(objects).hasSize(5);
        assertThat(objects.get(base + ".docx")).isEqualTo(original);
        assertThat(objects.get(base + "(2).docx")).isEqualTo(second);
    }

    @Test
    @DisplayName(
            "trả về các khóa riêng với nội dung tương ứng khi ba instance tải đồng thời tài liệu cùng tên")
    void test_updateDocumentFile_004() throws Exception {
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        simulateConditionalWrites(objects, new CountDownLatch(3));
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<String>> futures = new ArrayList<Future<String>>();
        List<byte[]> payloads = new ArrayList<byte[]>();
        try {
            for (int index = 0; index < 3; index++) {
                byte[] bytes = wordPackage("concurrent " + index);
                payloads.add(bytes);
                MockMultipartFile file =
                        new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, bytes);
                S3Service instance = new S3Service(client, mock(S3Presigner.class), "test-bucket");
                futures.add(
                        executor.submit(
                                () ->
                                        instance.updateDocumentFile(
                                                file,
                                                "question-files/physics",
                                                50,
                                                "teacher@example.com")));
            }
            List<String> keys = new ArrayList<String>();
            for (int index = 0; index < futures.size(); index++) {
                String key = futures.get(index).get(10, TimeUnit.SECONDS);
                keys.add(key);
                assertThat(objects.get(key)).isEqualTo(payloads.get(index));
            }
            assertThat(keys)
                    .containsExactlyInAnyOrder(
                            "question-files/physics/teacher@example.com-DeVatLy.docx",
                            "question-files/physics/teacher@example.com-DeVatLy(1).docx",
                            "question-files/physics/teacher@example.com-DeVatLy(2).docx");
            assertThat(objects).hasSize(3);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("trả về cùng khóa và gửi lại đầy đủ nội dung khi S3 tạm thời trả về 409")
    void test_updateDocumentFile_005() throws Exception {
        byte[] bytes = wordPackage();
        MockMultipartFile file = new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, bytes);
        List<String> attemptedKeys = new ArrayList<String>();
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenAnswer(
                        call -> {
                            PutObjectRequest request = call.getArgument(0);
                            RequestBody body = call.getArgument(1);
                            try (InputStream input = body.contentStreamProvider().newStream()) {
                                assertThat(input.readAllBytes()).isEqualTo(bytes);
                            }
                            assertThat(request.ifNoneMatch()).isEqualTo("*");
                            attemptedKeys.add(request.key());
                            if (attemptedKeys.size() <= 2) {
                                throw s3Failure(409);
                            }
                            return PutObjectResponse.builder().build();
                        });
        String key =
                service.updateDocumentFile(
                        file, "question-files/physics", 50, "teacher@example.com");
        assertThat(key).isEqualTo("question-files/physics/teacher@example.com-DeVatLy.docx");
        assertThat(attemptedKeys).containsExactly(key, key, key);
    }

    @Test
    @DisplayName("trả về lỗi S3 sau ba lần thử lại khi xung đột 409 kéo dài")
    void test_updateDocumentFile_006() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, wordPackage());
        AwsServiceException conflict = s3Failure(409);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(conflict);
        assertThatThrownBy(
                        () ->
                                service.updateDocumentFile(
                                        file, "question-files/physics", 50, "teacher@example.com"))
                .isSameAs(conflict);
        verify(client, times(4))
                .putObject(
                        argThat(
                                (PutObjectRequest request) ->
                                        request.key()
                                                        .equals(
                                                                "question-files/physics/teacher@example.com-DeVatLy.docx")
                                                && "*".equals(request.ifNoneMatch())),
                        any(RequestBody.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 500, 503})
    @DisplayName("trả về lỗi S3 ngay ở lần đầu khi mã lỗi khác 409 và 412")
    void test_updateDocumentFile_007(int status) throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, wordPackage());
        AwsServiceException error = s3Failure(status);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(error);
        assertThatThrownBy(
                        () ->
                                service.updateDocumentFile(
                                        file, "question-files/physics", 50, "teacher@example.com"))
                .isSameAs(error);
        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verifyNoMoreInteractions(client);
    }

    private void simulateConditionalWrites(
            Map<String, byte[]> objects, CountDownLatch firstAttempts) {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenAnswer(
                        call -> {
                            PutObjectRequest request = call.getArgument(0);
                            RequestBody body = call.getArgument(1);
                            assertThat(request.ifNoneMatch()).isEqualTo("*");
                            assertThat(request.contentType()).isEqualTo(DOCX_TYPE);
                            byte[] bytes;
                            try (InputStream input = body.contentStreamProvider().newStream()) {
                                bytes = input.readAllBytes();
                            }
                            if (firstAttempts != null && !request.key().contains("(")) {
                                firstAttempts.countDown();
                                assertThat(firstAttempts.await(5, TimeUnit.SECONDS)).isTrue();
                            }
                            if (objects.putIfAbsent(request.key(), bytes) != null) {
                                throw s3Failure(412);
                            }
                            return PutObjectResponse.builder().build();
                        });
    }

    @Test
    @DisplayName(
            "trả về cùng tên PascalCase khi tên gốc dùng dấu tiếng Việt hoặc dấu phân cách khác nhau")
    void test_updateDocumentFile_008() throws Exception {
        for (String name :
                new String[] {
                    "Đề thi vật lý.docx",
                    "ĐỀ_THI-vật.lý.DOCX",
                    "DeThiVatLy.docx",
                    "C:\\fakepath\\Đề thi vật lý.docx",
                    java.text.Normalizer.normalize(
                            "Đề thi vật lý.docx", java.text.Normalizer.Form.NFD)
                }) {
            MockMultipartFile file = new MockMultipartFile("file", name, DOCX_TYPE, wordPackage());
            assertThat(
                            service.updateDocumentFile(
                                    file, "question-files/math", 50, "teacher@example.com"))
                    .isEqualTo("question-files/math/teacher@example.com-DeThiVatLy.docx");
        }
    }

    @Test
    @DisplayName("trả về BadRequestException khi tên tệp thiếu chữ Latin hoặc chữ số")
    void test_updateDocumentFile_009() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "---.docx", DOCX_TYPE, wordPackage());
        assertThatThrownBy(
                        () ->
                                service.updateDocumentFile(
                                        file, "question-files/math", 50, "teacher@example.com"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về BadRequestException khi phần mở rộng hoặc nội dung sai định dạng Word")
    void test_validateDocumentFile_010() {
        for (String name : new String[] {"photo.png", "fake.docx"}) {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            name,
                            DOCX_TYPE,
                            "fake content".getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> service.validateDocumentFile(file, 50))
                    .isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về BadRequestException khi tệp ZIP thiếu các mục bắt buộc của DOCX")
    void test_validateDocumentFile_011() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(zipEntry("unrelated.txt"));
            zip.write("test".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        MockMultipartFile file =
                new MockMultipartFile("file", "fake.docx", DOCX_TYPE, bytes.toByteArray());
        assertThatThrownBy(() -> service.validateDocumentFile(file, 50))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về BadRequestException trước khi đọc nội dung khi tệp rỗng hoặc vượt 50 MiB")
    void test_validateDocumentFile_012() throws Exception {
        MockMultipartFile empty =
                new MockMultipartFile("file", "empty.docx", DOCX_TYPE, new byte[0]);
        assertThatThrownBy(() -> service.validateDocumentFile(empty, 50))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: Đầu vào là file trống");
        MultipartFile large = mock(MultipartFile.class);
        when(large.getSize()).thenReturn(50L * 1024 * 1024 + 1);
        assertThatThrownBy(() -> service.validateDocumentFile(large, 50))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: File lớn hơn 50MB");
        verify(large, never()).getInputStream();
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về BadRequestException khi tải tài liệu Word vào thư mục avatar")
    void test_uploadImageFile_013() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.docx", DOCX_TYPE, wordPackage());
        assertThatThrownBy(() -> service.uploadImageFile(file, "avatars", 5))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"avatar.png", "avatar.abc"})
    @DisplayName(
            "trả về khóa avatar có đuôi gốc và lưu MIME ảnh khi nội dung PNG hợp lệ ở đúng giới hạn 5 MiB")
    void test_uploadImageFile_014(String name) throws Exception {
        int maxSize = 5 * 1024 * 1024;
        byte[] image = Arrays.copyOf(pngBytes(), maxSize);
        MockMultipartFile file =
                new MockMultipartFile("file", name, "application/octet-stream", image);
        expectUploadWithBytes(image);

        String key = service.uploadImageFile(file, "avatars", 5);

        assertThat(key).startsWith("avatars/").endsWith(name.substring(name.lastIndexOf('.')));
        verify(client)
                .putObject(
                        argThat(
                                (PutObjectRequest request) ->
                                        request.bucket().equals("test-bucket")
                                                && request.key().equals(key)
                                                && request.contentType().equals("image/png")),
                        any(RequestBody.class));
    }

    @Test
    @DisplayName("trả về BadRequestException khi tệp avatar rỗng")
    void test_uploadImageFile_015() {
        MockMultipartFile empty =
                new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadImageFile(empty, "avatars", 5))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: Đầu vào là file trống");
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về BadRequestException khi tệp avatar vượt 5 MiB")
    void test_uploadImageFile_016() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(5L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.uploadImageFile(file, "avatars", 5))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: File lớn hơn 5MB");
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về BadRequestException khi tệp văn bản được khai báo là ảnh PNG")
    void test_uploadImageFile_017() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "avatar.png",
                        "image/png",
                        "Van ban thuong".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.uploadImageFile(file, "avatars", 5))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: File không đúng định dạng");
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về lỗi S3 khi ảnh hợp lệ không thể ghi vào bucket")
    void test_uploadImageFile_018() {
        MockMultipartFile file =
                new MockMultipartFile("file", "avatar.png", "image/png", pngBytes());
        AwsServiceException failure = s3Failure(503);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.uploadImageFile(file, "avatars", 5)).isSameAs(failure);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("trả về BadRequestException khi tải DOCX hợp lệ nhưng thiếu email người tải")
    void test_updateDocumentFile_019(String email) throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.docx", DOCX_TYPE, wordPackage());

        assertThatThrownBy(() -> service.updateDocumentFile(file, "question-files/math", 50, email))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lỗi: Thiếu email người tải lên");
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi kiểm tra tệp DOCX hợp lệ để duyệt")
    void test_validateDocumentFile_020() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.docx", DOCX_TYPE, wordPackage());

        assertThatCode(() -> service.validateDocumentFile(file, 50)).doesNotThrowAnyException();
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về khóa có tên tệp và gửi đúng nội dung khi tải tệp cục bộ")
    void test_uploadLocalFile_021(@TempDir Path directory) throws IOException {
        byte[] bytes = {1, 2, 3};
        Path path = Files.write(directory.resolve("diagram.png"), bytes);
        expectUploadWithBytes(bytes);

        String key = service.uploadLocalFile(path.toFile(), "figures");

        assertThat(key).startsWith("figures/").endsWith("_diagram.png");
        verify(client)
                .putObject(
                        argThat(
                                (PutObjectRequest request) ->
                                        request.bucket().equals("test-bucket")
                                                && request.key().equals(key)),
                        any(RequestBody.class));
    }

    @Test
    @DisplayName("trả về lỗi S3 khi quá trình tải tệp cục bộ thất bại")
    void test_uploadLocalFile_022(@TempDir Path directory) throws IOException {
        Path path = Files.createFile(directory.resolve("diagram.png"));
        AwsServiceException failure = s3Failure(503);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.uploadLocalFile(path.toFile(), "figures"))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("trả về URL xem avatar có thời hạn 60 phút khi có khóa ảnh")
    void test_generatePresignedUrl_023() throws Exception {
        String key = "avatars/teacher.png";
        String url = "https://example.test/view-avatar";
        PresignedGetObjectRequest presigned = presignedRequest(url);
        when(signer.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String result = service.generatePresignedUrl(key);

        assertThat(result).isEqualTo(url);
        GetObjectPresignRequest request = capturedPresignRequest();
        assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(60));
        assertThat(request.getObjectRequest())
                .isEqualTo(GetObjectRequest.builder().bucket("test-bucket").key(key).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("trả về null khi khóa ảnh bị thiếu hoặc chỉ có khoảng trắng")
    void test_generatePresignedUrl_024(String key) {
        assertThat(service.generatePresignedUrl(key)).isNull();
        verifyNoInteractions(signer);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi S3 xóa đối tượng theo khóa yêu cầu")
    void test_deleteFile_025() {
        String key = "question-files/math/test.docx";

        assertThatCode(() -> service.deleteFile(key)).doesNotThrowAnyException();

        verify(client)
                .deleteObject(DeleteObjectRequest.builder().bucket("test-bucket").key(key).build());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("trả về không có ngoại lệ và không gọi S3 khi khóa cần xóa bị thiếu")
    void test_deleteFile_026(String key) {
        assertThatCode(() -> service.deleteFile(key)).doesNotThrowAnyException();
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("trả về lỗi S3 khi bucket từ chối xóa đối tượng")
    void test_deleteFile_027() {
        AwsServiceException failure = s3Failure(403);
        when(client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.deleteFile("question-files/math/test.docx"))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("trả về nguyên nội dung tài liệu khi S3 tìm thấy đối tượng theo khóa")
    void test_readDocumentFile_028() {
        String key = "question-files/math/test.docx";
        byte[] bytes = {1, 2, 3};
        when(client.getObjectAsBytes(
                        GetObjectRequest.builder().bucket("test-bucket").key(key).build()))
                .thenReturn(
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));

        assertThat(service.readDocumentFile(key)).isEqualTo(bytes);
    }

    @Test
    @DisplayName("trả về lỗi S3 khi đối tượng cần đọc không tồn tại")
    void test_readDocumentFile_029() {
        AwsServiceException failure = s3Failure(404);
        when(client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.readDocumentFile("question-files/math/missing.docx"))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("trả về metadata và URL tải của cả hai trang khi S3 phân trang và có mục thư mục")
    void test_listQuestionFiles_030() throws Exception {
        Instant modified = Instant.parse("2026-10-24T00:00:00Z");
        String firstKey = "question-files/math/Đề thi 1.docx";
        String secondKey = "question-files/second.pdf";
        String firstUrl = "https://example.test/download/first";
        String secondUrl = "https://example.test/download/second";
        ListObjectsV2Request firstPage =
                ListObjectsV2Request.builder()
                        .bucket("test-bucket")
                        .prefix("question-files/")
                        .build();
        when(client.listObjectsV2(firstPage))
                .thenReturn(
                        ListObjectsV2Response.builder()
                                .isTruncated(true)
                                .nextContinuationToken("page-2")
                                .contents(
                                        S3Object.builder().key("question-files/").size(0L).build(),
                                        S3Object.builder()
                                                .key(firstKey)
                                                .size(123L)
                                                .lastModified(modified)
                                                .build())
                                .build());
        when(client.listObjectsV2(firstPage.toBuilder().continuationToken("page-2").build()))
                .thenReturn(
                        ListObjectsV2Response.builder()
                                .isTruncated(false)
                                .contents(
                                        S3Object.builder()
                                                .key(secondKey)
                                                .size(45L)
                                                .lastModified(modified)
                                                .build())
                                .build());
        PresignedGetObjectRequest firstDownload = presignedRequest(firstUrl);
        PresignedGetObjectRequest secondDownload = presignedRequest(secondUrl);
        when(signer.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(firstDownload, secondDownload);

        List<S3ObjectInfo> files = service.listQuestionFiles("question-files");

        assertThat(files)
                .containsExactly(
                        new S3ObjectInfo(firstKey, "Đề thi 1.docx", 123L, modified, firstUrl),
                        new S3ObjectInfo(secondKey, "second.pdf", 45L, modified, secondUrl));
        ArgumentCaptor<GetObjectPresignRequest> requests =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(signer, times(2)).presignGetObject(requests.capture());
        assertThat(requests.getAllValues())
                .zipSatisfy(
                        files,
                        (request, file) -> {
                            assertThat(request.signatureDuration())
                                    .isEqualTo(Duration.ofMinutes(60));
                            GetObjectRequest object = request.getObjectRequest();
                            assertThat(object.bucket()).isEqualTo("test-bucket");
                            assertThat(object.key()).isEqualTo(file.getKey());
                            ContentDisposition disposition =
                                    ContentDisposition.parse(object.responseContentDisposition());
                            assertThat(disposition.isAttachment()).isTrue();
                            assertThat(disposition.getFilename()).isEqualTo(file.getFileName());
                        });
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi S3 không có đối tượng dưới prefix yêu cầu")
    void test_listQuestionFiles_031() {
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().isTruncated(false).build());

        assertThat(service.listQuestionFiles("question-files")).isEmpty();
        verifyNoInteractions(signer);
    }

    @Test
    @DisplayName("trả về lỗi ký URL khi trình ký không tạo được liên kết tải xuống")
    void test_listQuestionFiles_032() {
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(
                        ListObjectsV2Response.builder()
                                .contents(
                                        S3Object.builder()
                                                .key("question-files/test.docx")
                                                .size(1L)
                                                .build())
                                .build());
        SdkClientException failure = SdkClientException.create("signing failed");
        when(signer.presignGetObject(any(GetObjectPresignRequest.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.listQuestionFiles("question-files")).isSameAs(failure);
    }

    private void expectUploadWithBytes(byte[] expected) {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenAnswer(
                        call -> {
                            RequestBody body = call.getArgument(1);
                            try (InputStream input = body.contentStreamProvider().newStream()) {
                                assertThat(input.readAllBytes()).isEqualTo(expected);
                            }
                            return PutObjectResponse.builder().build();
                        });
    }

    private PresignedGetObjectRequest presignedRequest(String url) throws Exception {
        PresignedGetObjectRequest result = mock(PresignedGetObjectRequest.class);
        when(result.url()).thenReturn(URI.create(url).toURL());
        return result;
    }

    private GetObjectPresignRequest capturedPresignRequest() {
        ArgumentCaptor<GetObjectPresignRequest> request =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(signer).presignGetObject(request.capture());
        return request.getValue();
    }

    private byte[] pngBytes() {
        return Base64.getDecoder()
                .decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1sAAAAASUVORK5CYII=");
    }

    private byte[] wordPackage() throws Exception {
        return wordPackage("");
    }

    private byte[] wordPackage(String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(zipEntry("[Content_Types].xml"));
            zip.write(
                    ("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                                    + "<Override PartName=\"/word/document.xml\" ContentType=\""
                                    + "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                                    + "</Types>")
                            .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(zipEntry("word/document.xml"));
            zip.write(
                    ("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                                    + "<w:body><w:p><w:r><w:t>"
                                    + content
                                    + "</w:t></w:r></w:p></w:body></w:document>")
                            .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private ZipEntry zipEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setTimeLocal(LocalDateTime.of(2026, 1, 1, 0, 0));
        return entry;
    }

    private AwsServiceException s3Failure(int status) {
        return S3Exception.builder().statusCode(status).build();
    }
}
