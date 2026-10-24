package com.wedu.exam_creation.storage.service;

import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.storage.controller.StorageController;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
                        && request.contentType().equals(DOCX_TYPE)), any(RequestBody.class));
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
        when(storage.updateDocumentFile(file, "question-files/math", 50, "teacher@example.com")).thenReturn("question-files/math/teacher@example.com-test.docx");
        assertThat(new StorageController(storage).uploadDocumentToQuestionBank(principal, file, "math").getBody())
                .isEqualTo("question-files/math/teacher@example.com-test.docx");
        verify(storage).updateDocumentFile(file, "question-files/math", 50, "teacher@example.com");
        verifyNoMoreInteractions(storage);
    }

    private byte[] wordPackage() throws Exception {
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
                    + "<w:body/></w:document>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
