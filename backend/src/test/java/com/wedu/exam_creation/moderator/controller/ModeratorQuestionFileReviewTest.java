package com.wedu.exam_creation.moderator.controller;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.moderator.usecase.ModeratorUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModeratorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ModeratorUsecase.class, S3Service.class})
@TestPropertySource(properties = "aws.bucketName=test-question-bucket")
class ModeratorQuestionFileReviewTest {
    @MockitoBean private com.wedu.exam_creation.chapter.usecase.ChapterService chapters;
    @MockitoBean private com.wedu.exam_creation.question.usecase.QuestionService questions;
    private static final String KEY = "question-files/physics/teacher-name@example-domain.com-DeThi(1).docx";
    @Autowired private MockMvc mvc;
    @Autowired private WebApplicationContext context;
    @MockitoBean private CustomOAuth2UserService oauthService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private ImporterService importer;
    @MockitoBean private MailService mail;
    @MockitoBean private S3Client s3;
    @MockitoBean private S3Presigner presigner;
    private CommonUserResponseAllDTO moderator;
    private byte[] original;

    @BeforeEach
    void setup() throws Exception {
        moderator = new CommonUserResponseAllDTO();
        moderator.setRole("ROLE_MODERATOR");
        moderator.setEmail("moderator@example.com");
        moderator.setIsActive(true);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").header("Authorization", "Bearer test-token")).build();
        lenient().when(tokens.getPayloadFromAccessToken("test-token"))
                .thenReturn(new ATPayload("jti", "moderator", moderator.getEmail(), moderator.getRole()));
        lenient().when(users.loadUserByUsername(moderator.getEmail())).thenReturn(new CustomUserDetails(moderator));
        original = document("original");
        lenient().when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), original));
        lenient().when(importer.parseDocument(any(), eq("physics"))).thenReturn(List.of("q1", "q2"));
    }

    @Test
    void approvesOriginalThenDeletes() throws Exception {
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionIds[0]").value("q1"))
                .andExpect(jsonPath("$.fileDeleted").value(true));
        var order = inOrder(s3, importer);
        order.verify(s3).getObjectAsBytes(any(GetObjectRequest.class));
        order.verify(importer).parseDocument(argThat(file -> matches(file, original)), eq("physics"));
        order.verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("test-question-bucket").key(KEY).build());
        verifyNoInteractions(mail);
    }

    @Test
    void approvesEditedBytesUsingOriginalSubject() throws Exception {
        byte[] edited = document("edited");
        mvc.perform(multipart("/moderator/question-files/approve")
                        .file(new MockMultipartFile("file", "Edited.docx", "application/octet-stream", edited))
                        .param("key", KEY).param("subject", "untrusted-subject"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fileDeleted").value(true));
        verify(importer).parseDocument(argThat(file -> matches(file, edited)), eq("physics"));
    }

    @Test
    void preservesOriginalWhenImportFails() throws Exception {
        when(importer.parseDocument(any(), anyString())).thenThrow(new IllegalArgumentException("bad document"));
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isInternalServerError());
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void reportsImportedWhenDeleteFails() throws Exception {
        when(s3.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(503).build());
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fileDeleted").value(false))
                .andExpect(jsonPath("$.questionIds.length()").value(2));
        verify(importer, times(1)).parseDocument(any(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Empty.docx", "Bad.pdf", "Fake.docx"})
    void rejectsInvalidEditedFile(String name) throws Exception {
        byte[] content = name.startsWith("Empty") ? new byte[0] : new byte[]{1, 2, 3};
        mvc.perform(multipart("/moderator/question-files/approve")
                        .file(new MockMultipartFile("file", name, "application/octet-stream", content)).param("key", KEY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(importer);
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void rejectsOversizedEditedFile() throws Exception {
        mvc.perform(multipart("/moderator/question-files/approve")
                        .file(new MockMultipartFile("file", "Big.docx", "application/octet-stream", new byte[50 * 1024 * 1024 + 1]))
                        .param("key", KEY)).andExpect(status().isBadRequest());
        verifyNoInteractions(importer);
    }

    @Test
    void rejectsOversizedOriginal() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[50 * 1024 * 1024 + 1]));
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY)).andExpect(status().isBadRequest());
        verifyNoInteractions(importer);
    }

    @Test
    void rejectionDeletesBeforeSendingTrimmedPlainReason() throws Exception {
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json")
                        .content(rejection(KEY, "  Sai nội dung <b>  ")))
                .andExpect(status().isNoContent());
        var order = inOrder(s3, mail);
        order.verify(s3).getObjectAsBytes(any(GetObjectRequest.class));
        order.verify(s3).deleteObject(any(DeleteObjectRequest.class));
        order.verify(mail).sendQuestionFileRejectionEmail("teacher-name@example-domain.com", "teacher-name@example-domain.com-DeThi(1).docx", "Sai nội dung <b>");
        verifyNoInteractions(importer);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void mailFailureIsLoggedAndStillReturnsSuccessWithoutRetry(CapturedOutput output) throws Exception {
        doThrow(new org.springframework.mail.MailSendException("private SMTP details"))
                .when(mail).sendQuestionFileRejectionEmail(anyString(), anyString(), anyString());
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json")
                        .content(rejection(KEY, "Sai nội dung")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(mail, times(1)).sendQuestionFileRejectionEmail(anyString(), anyString(), anyString());
        assertThat(output.getOut()).contains("MailSendException")
                .doesNotContain("private SMTP details");
    }

    @Test
    void doesNotNotifyWhenDeleteFails() throws Exception {
        when(s3.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(503).build());
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json").content(rejection(KEY, "Sai")))
                .andExpect(status().isInternalServerError());
        verifyNoInteractions(mail);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void requiresReason(String reason) throws Exception {
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json").content(rejection(KEY, reason)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, mail);
    }

    @Test
    void limitsReasonLength() throws Exception {
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json").content(rejection(KEY, "a".repeat(2001))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, mail);
    }

    @ParameterizedTest
    @ValueSource(strings = {"avatars/user@example.com-X.docx", "question-files/../x.docx", "question-files//x.docx", "question-files/physics/x.pdf", "question-files/physics/sub/x.docx"})
    void rejectsBadReviewKeys(String key) throws Exception {
        mvc.perform(multipart("/moderator/question-files/approve").param("key", key)).andExpect(status().isBadRequest());
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json").content(rejection(key, "Sai")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, importer, mail);
    }

    @ParameterizedTest
    @ValueSource(strings = {"noemail.docx", "bad-email-X.docx", "user@example.com.docx"})
    void doesNotDeleteIfEmailCannotBeExtracted(String fileName) throws Exception {
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json")
                        .content(rejection("question-files/physics/" + fileName, "Sai")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, mail);
    }

    @Test
    void missingOriginalStopsBothActions() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(404).build());
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY)).andExpect(status().isNotFound());
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json").content(rejection(KEY, "Sai")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(importer, mail);
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_TEACHER", "ROLE_ADMIN"})
    void requiresModeratorRole(String role) throws Exception {
        moderator.setRole(role);
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY)).andExpect(status().isForbidden());
        mvc.perform(post("/moderator/question-files/reject").contentType("application/json").content(rejection(KEY, "Sai")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(s3, importer, mail);
    }

    @Test
    void requiresToken() throws Exception {
        mvc.perform(multipart("/moderator/question-files/approve").header("Authorization", "Bearer ").param("key", KEY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/moderator/question-files/reject").header("Authorization", "Bearer ")
                        .contentType("application/json").content(rejection(KEY, "Sai"))).andExpect(status().isUnauthorized());
    }

    private static String rejection(String key, String reason) throws Exception {
        return new tools.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("key", key, "reason", reason));
    }

    private static boolean matches(MultipartFile file, byte[] expected) {
        try { return java.util.Arrays.equals(file.getBytes(), expected); }
        catch (Exception e) { return false; }
    }

    private static byte[] document(String content) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String name : List.of("[Content_Types].xml", "word/document.xml")) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
