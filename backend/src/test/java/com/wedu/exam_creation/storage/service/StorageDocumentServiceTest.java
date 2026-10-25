package com.wedu.exam_creation.storage.service;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.storage.controller.StorageController;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageDocumentServiceTest {
    private static final String DOCX_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private final S3Client client = mock(S3Client.class);
    private final S3Service service = new S3Service(client, mock(S3Presigner.class), "test-bucket");

    @Test
    void uploadsWordDocumentWithCorrectContentType() throws Exception {
        var file = new MockMultipartFile("file", "Đề thi.DOCX", "application/octet-stream", wordPackage());
        String key = service.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com");
        assertThat(key).isEqualTo("question-files/math/teacher@example.com-DeThi.docx");
        verify(client).putObject(argThat((PutObjectRequest request) ->
                request.bucket().equals("test-bucket") && request.key().equals(key)
                        && request.contentType().equals(DOCX_TYPE)
                        && "*".equals(request.ifNoneMatch())), any(RequestBody.class));
    }

    @Test
    void preservesExistingDocumentsAndReturnsNumberedKeysAfterNormalization() throws Exception {
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        simulateConditionalWrites(objects, null);
        String base = "question-files/physics/dnson384@gmail.com-DeVatLy";
        String[] names = {"Đề vật lý.docx", "DeVatLy.docx", "ĐỀ-VẬT_LÝ.DOCX"};
        var payloads = new ArrayList<byte[]>();
        for (int index = 0; index < names.length; index++) {
            byte[] bytes = wordPackage("document " + index);
            payloads.add(bytes);
            var file = new MockMultipartFile("file", names[index], DOCX_TYPE, bytes);
            String expectedKey = base + (index == 0 ? "" : "(" + index + ")") + ".docx";
            assertThat(service.updateDocumentFile(file, "question-files/physics", 50, "dnson384@gmail.com"))
                    .isEqualTo(expectedKey);
            assertThat(objects.get(expectedKey)).isEqualTo(bytes);
        }
        assertThat(objects).hasSize(3);
        assertThat(objects.get(base + ".docx")).isEqualTo(payloads.get(0));
        assertThat(objects.get(base + "(1).docx")).isEqualTo(payloads.get(1));
        verify(client, times(6)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void usesFirstAvailableSuffixAndKeepsOtherFoldersAndUploadersIndependent() throws Exception {
        String base = "question-files/physics/teacher@example.com-DeVatLy";
        byte[] original = wordPackage("original");
        byte[] second = wordPackage("second");
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        objects.put(base + ".docx", original);
        objects.put(base + "(2).docx", second);
        simulateConditionalWrites(objects, null);
        byte[] bytes = wordPackage("new");
        var file = new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, bytes);

        assertThat(service.updateDocumentFile(file, "question-files/physics", 50, "teacher@example.com"))
                .isEqualTo(base + "(1).docx");
        assertThat(service.updateDocumentFile(file, "question-files/other", 50, "teacher@example.com"))
                .isEqualTo("question-files/other/teacher@example.com-DeVatLy.docx");
        assertThat(service.updateDocumentFile(file, "question-files/physics", 50, "other@example.com"))
                .isEqualTo("question-files/physics/other@example.com-DeVatLy.docx");
        assertThat(objects).hasSize(5);
        assertThat(objects.get(base + ".docx")).isEqualTo(original);
        assertThat(objects.get(base + "(2).docx")).isEqualTo(second);
    }

    @Test
    void simultaneousUploadsFromDifferentInstancesKeepEveryDocument() throws Exception {
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        simulateConditionalWrites(objects, new CountDownLatch(3));
        var executor = Executors.newFixedThreadPool(3);
        var futures = new ArrayList<Future<String>>();
        var payloads = new ArrayList<byte[]>();
        try {
            for (int index = 0; index < 3; index++) {
                byte[] bytes = wordPackage("concurrent " + index);
                payloads.add(bytes);
                var file = new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, bytes);
                var instance = new S3Service(client, mock(S3Presigner.class), "test-bucket");
                futures.add(executor.submit(() -> instance.updateDocumentFile(
                        file, "question-files/physics", 50, "teacher@example.com")));
            }
            var keys = new ArrayList<String>();
            for (int index = 0; index < futures.size(); index++) {
                String key = futures.get(index).get(10, TimeUnit.SECONDS);
                keys.add(key);
                assertThat(objects.get(key)).isEqualTo(payloads.get(index));
            }
            assertThat(keys).containsExactlyInAnyOrder(
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
    void retriesTemporaryConflictsAtTheSameKeyWithFreshContent() throws Exception {
        byte[] bytes = wordPackage();
        var file = new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, bytes);
        var attemptedKeys = new ArrayList<String>();
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(call -> {
            PutObjectRequest request = call.getArgument(0);
            RequestBody body = call.getArgument(1);
            try (var input = body.contentStreamProvider().newStream()) {
                assertThat(input.readAllBytes()).isEqualTo(bytes);
            }
            assertThat(request.ifNoneMatch()).isEqualTo("*");
            attemptedKeys.add(request.key());
            if (attemptedKeys.size() <= 2) {
                throw S3Exception.builder().statusCode(409).build();
            }
            return PutObjectResponse.builder().build();
        });
        String key = service.updateDocumentFile(file, "question-files/physics", 50, "teacher@example.com");
        assertThat(key).isEqualTo("question-files/physics/teacher@example.com-DeVatLy.docx");
        assertThat(attemptedKeys).containsExactly(key, key, key);
    }

    @Test
    void stopsAfterThreeRetriesOfPersistentConflict() throws Exception {
        var file = new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, wordPackage());
        var conflict = S3Exception.builder().statusCode(409).build();
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(conflict);
        assertThatThrownBy(() -> service.updateDocumentFile(file, "question-files/physics", 50, "teacher@example.com"))
                .isSameAs(conflict);
        verify(client, times(4)).putObject(argThat((PutObjectRequest request) ->
                request.key().equals("question-files/physics/teacher@example.com-DeVatLy.docx")
                        && "*".equals(request.ifNoneMatch())), any(RequestBody.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 500, 503})
    void doesNotTreatOtherStorageErrorsAsNameCollisions(int status) throws Exception {
        var file = new MockMultipartFile("file", "DeVatLy.docx", DOCX_TYPE, wordPackage());
        var error = S3Exception.builder().statusCode(status).build();
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(error);
        assertThatThrownBy(() -> service.updateDocumentFile(file, "question-files/physics", 50, "teacher@example.com"))
                .isSameAs(error);
        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verifyNoMoreInteractions(client);
    }

    private void simulateConditionalWrites(Map<String, byte[]> objects, CountDownLatch firstAttempts) {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(call -> {
            PutObjectRequest request = call.getArgument(0);
            RequestBody body = call.getArgument(1);
            assertThat(request.ifNoneMatch()).isEqualTo("*");
            assertThat(request.contentType()).isEqualTo(DOCX_TYPE);
            byte[] bytes;
            try (var input = body.contentStreamProvider().newStream()) {
                bytes = input.readAllBytes();
            }
            if (firstAttempts != null && !request.key().contains("(")) {
                firstAttempts.countDown();
                assertThat(firstAttempts.await(5, TimeUnit.SECONDS)).isTrue();
            }
            if (objects.putIfAbsent(request.key(), bytes) != null) {
                throw S3Exception.builder().statusCode(412).build();
            }
            return PutObjectResponse.builder().build();
        });
    }

    @Test
    void normalizesVietnameseSeparatorsAndExistingPascalCase() throws Exception {
        for (String name : new String[]{"Đề thi vật lý.docx", "ĐỀ_THI-vật.lý.DOCX",
                "DeThiVatLy.docx", "C:\\fakepath\\Đề thi vật lý.docx",
                java.text.Normalizer.normalize("Đề thi vật lý.docx", java.text.Normalizer.Form.NFD)}) {
            var file = new MockMultipartFile("file", name, DOCX_TYPE, wordPackage());
            assertThat(service.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com"))
                    .isEqualTo("question-files/math/teacher@example.com-DeThiVatLy.docx");
        }
    }

    @Test
    void rejectsNamesWithoutUsableAsciiCharacters() throws Exception {
        var file = new MockMultipartFile("file", "---.docx", DOCX_TYPE, wordPackage());
        assertThatThrownBy(() -> service.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(client);
    }

    @Test
    void rejectsImagesAndFakeWordFiles() {
        for (String name : new String[]{"photo.png", "fake.docx"}) {
            var file = new MockMultipartFile("file", name, DOCX_TYPE, "fake content".getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> service.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com"))
                    .isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(client);
    }

    @Test
    void rejectsGenericZipRenamedToDocx() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("unrelated.txt"));
            zip.write("test".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var file = new MockMultipartFile("file", "fake.docx", DOCX_TYPE, bytes.toByteArray());
        assertThatThrownBy(() -> service.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(client);
    }

    @Test
    void rejectsEmptyAndOversizedDocumentsBeforeReadingContent() throws Exception {
        var empty = new MockMultipartFile("file", "empty.docx", DOCX_TYPE, new byte[0]);
        assertThatThrownBy(() -> service.updateDocumentFile(empty, "question-files/math", 50, "teacher@example.com"))
                .isInstanceOf(BadRequestException.class).hasMessage("Lỗi: Đầu vào là file trống");
        MultipartFile large = mock(MultipartFile.class);
        when(large.getSize()).thenReturn(50L * 1024 * 1024 + 1);
        assertThatThrownBy(() -> service.updateDocumentFile(large, "question-files/math", 50, "teacher@example.com"))
                .isInstanceOf(BadRequestException.class).hasMessage("Lỗi: File lớn hơn 50MB");
        verify(large, never()).getInputStream();
        verifyNoInteractions(client);
    }

    @Test
    void imageUploadStillRejectsWordDocuments() throws Exception {
        var file = new MockMultipartFile("file", "test.docx", DOCX_TYPE, wordPackage());
        assertThatThrownBy(() -> service.uploadImageFile(file, "avatars", 5))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(client);
    }

    @Test
    void questionBankControllerUsesDocumentUpload() throws Exception {
        S3Service storage = mock(S3Service.class);
        var user = mock(CommonUserResponseAllDTO.class);
        when(user.getEmail()).thenReturn("teacher@example.com");
        var principal = new CustomUserDetails(user);
        var file = new MockMultipartFile("file", "test.docx", DOCX_TYPE, wordPackage());
        when(storage.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com")).thenReturn("question-files/math/teacher@example.com-Test(2).docx");
        assertThat(new StorageController(storage).uploadDocumentToQuestionBank(principal, file, "math").getBody())
                .isEqualTo("question-files/math/teacher@example.com-Test(2).docx");
        verify(storage).updateDocumentFile(file, "question-files/math", 50, "teacher@example.com");
        verifyNoMoreInteractions(storage);
    }

    private byte[] wordPackage() throws Exception {
        return wordPackage("");
    }

    private byte[] wordPackage(String content) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Override PartName=\"/word/document.xml\" ContentType=\""
                    + "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                    + "</Types>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                    + "<w:body><w:p><w:r><w:t>" + content
                    + "</w:t></w:r></w:p></w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
