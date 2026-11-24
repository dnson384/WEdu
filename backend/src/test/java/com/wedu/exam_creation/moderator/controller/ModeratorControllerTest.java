package com.wedu.exam_creation.moderator.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.moderator.usecase.EditPendingQuestionUsecase;
import com.wedu.exam_creation.moderator.usecase.ModeratorUsecase;
import com.wedu.exam_creation.moderator.usecase.ReviewPendingQuestionUsecase;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import com.wedu.exam_creation.storage.service.S3Service;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@WebMvcTest(ModeratorController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    ModeratorUsecase.class,
    S3Service.class,
    EditPendingQuestionUsecase.class
})
@TestPropertySource(properties = "aws.bucketName=test-question-bucket")
class ModeratorControllerTest {
    private static final String KEY =
            "question-files/physics/teacher-name@example-domain.com-DeThi(1).docx";
    private static final String ID = "507f1f77bcf86cd799439011";
    @Autowired private WebApplicationContext context;
    @MockitoBean private ChapterService chapters;
    @MockitoBean private QuestionService questions;
    @MockitoBean private ReviewPendingQuestionUsecase reviewer;
    @MockitoBean private CustomOAuth2UserService oauthService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private ImporterService importer;
    @MockitoBean private MailService mail;
    @MockitoBean private S3Client s3;
    @MockitoBean private S3Presigner presigner;
    @MockitoSpyBean private S3Service storage;
    private MockMvc mvc;
    private MockMvc mvcWithoutToken;
    private CommonUserResponseAllDTO moderator;
    private byte[] original;

    @BeforeEach
    void setup() throws Exception {
        moderator = new CommonUserResponseAllDTO();
        moderator.setRole("ROLE_MODERATOR");
        moderator.setEmail("moderator@example.com");
        moderator.setIsActive(true);
        mvcWithoutToken =
                MockMvcBuilders.webAppContextSetup(context)
                        .apply(SecurityMockMvcConfigurers.springSecurity())
                        .build();
        mvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .apply(SecurityMockMvcConfigurers.springSecurity())
                        .defaultRequest(get("/").header("Authorization", "Bearer test-token"))
                        .build();
        when(tokens.getPayloadFromAccessToken("test-token"))
                .thenReturn(
                        new ATPayload(
                                "jti", "moderator", moderator.getEmail(), moderator.getRole()));
        when(users.loadUserByUsername(moderator.getEmail()))
                .thenReturn(new CustomUserDetails(moderator));
        original = document("original");
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), original));
        when(importer.parseDocument(any(), eq("physics"))).thenReturn(List.of("q1", "q2"));
        when(chapters.getAll()).thenReturn(List.of());
        PendingQuestionDTO pending = new PendingQuestionDTO();
        pending.setUpdatedAt(Instant.parse("2026-10-29T00:00:00Z"));
        pending.setQuestion(
                new ContentDTO(
                        "<img_0>", new VariablesDTO(Map.of(), Map.of("img_0", "AI description"))));
        pending.setOptions(List.of());
        when(questions.getPendingForEdit(ID)).thenReturn(pending);
    }

    @Test
    @DisplayName("trả về HTTP 200 với IDs đã nhập và xác nhận xóa khi duyệt tệp gốc")
    void test_approveQuestionFile_001() throws Exception {
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionIds[0]").value("q1"))
                .andExpect(jsonPath("$.fileDeleted").value(true));
        InOrder order = inOrder(s3, importer);
        order.verify(s3).getObjectAsBytes(any(GetObjectRequest.class));
        order.verify(importer)
                .parseDocument(argThat(file -> matches(file, original)), eq("physics"));
        order.verify(s3)
                .deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket("test-question-bucket")
                                .key(KEY)
                                .build());
        verifyNoInteractions(mail);
    }

    @Test
    @DisplayName("trả về HTTP 200 khi duyệt tệp sửa theo môn học của tệp gốc")
    void test_approveQuestionFile_002() throws Exception {
        byte[] edited = document("edited");
        mvc.perform(
                        multipart("/moderator/question-files/approve")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "Edited.docx",
                                                "application/octet-stream",
                                                edited))
                                .param("key", KEY)
                                .param("subject", "untrusted-subject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileDeleted").value(true));
        verify(importer).parseDocument(argThat(file -> matches(file, edited)), eq("physics"));
    }

    @Test
    @DisplayName("trả về HTTP 500 và giữ tệp gốc khi import thất bại")
    void test_approveQuestionFile_003() throws Exception {
        when(importer.parseDocument(any(), anyString()))
                .thenThrow(new IllegalArgumentException("bad document"));
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isInternalServerError());
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("trả về HTTP 200 với fileDeleted false khi đã nhập nhưng xóa tệp gốc thất bại")
    void test_approveQuestionFile_004() throws Exception {
        when(s3.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(503).build());
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileDeleted").value(false))
                .andExpect(jsonPath("$.questionIds.length()").value(2));
        verify(importer, times(1)).parseDocument(any(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Empty.docx", "Bad.pdf", "Fake.docx"})
    @DisplayName("trả về HTTP 400 khi tệp sửa rỗng, sai đuôi hoặc sai định dạng")
    void test_approveQuestionFile_005(String name) throws Exception {
        byte[] content = name.startsWith("Empty") ? new byte[0] : new byte[] {1, 2, 3};
        mvc.perform(
                        multipart("/moderator/question-files/approve")
                                .file(
                                        new MockMultipartFile(
                                                "file", name, "application/octet-stream", content))
                                .param("key", KEY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(importer);
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("trả về HTTP 400 khi tệp sửa vượt quá 50 MiB")
    void test_approveQuestionFile_006() throws Exception {
        mvc.perform(
                        multipart("/moderator/question-files/approve")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "Big.docx",
                                                "application/octet-stream",
                                                new byte[50 * 1024 * 1024 + 1]))
                                .param("key", KEY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(importer);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi tệp gốc vượt quá 50 MiB")
    void test_approveQuestionFile_007() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(
                        ResponseBytes.fromByteArray(
                                GetObjectResponse.builder().build(),
                                new byte[50 * 1024 * 1024 + 1]));
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(importer);
    }

    @Test
    @DisplayName("trả về HTTP 204 khi xóa tệp trước khi gửi lý do đã trim")
    void test_rejectQuestionFile_008() throws Exception {
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(KEY, "  Sai nội dung <b>  ")))
                .andExpect(status().isNoContent());
        InOrder order = inOrder(s3, mail);
        order.verify(s3).getObjectAsBytes(any(GetObjectRequest.class));
        order.verify(s3).deleteObject(any(DeleteObjectRequest.class));
        order.verify(mail)
                .sendQuestionFileRejectionEmail(
                        "teacher-name@example-domain.com",
                        "teacher-name@example-domain.com-DeThi(1).docx",
                        "Sai nội dung <b>");
        verifyNoInteractions(importer);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("trả về HTTP 204 khi gửi email thất bại sau khi xóa và chỉ ghi loại lỗi")
    void test_rejectQuestionFile_009(CapturedOutput output) throws Exception {
        doThrow(new org.springframework.mail.MailSendException("private SMTP details"))
                .when(mail)
                .sendQuestionFileRejectionEmail(anyString(), anyString(), anyString());
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(KEY, "Sai nội dung")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(mail, times(1))
                .sendQuestionFileRejectionEmail(anyString(), anyString(), anyString());
        assertThat(output.getOut())
                .contains("MailSendException")
                .doesNotContain("private SMTP details");
    }

    @Test
    @DisplayName("trả về HTTP 500 và không gửi email khi xóa tệp thất bại")
    void test_rejectQuestionFile_010() throws Exception {
        when(s3.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(503).build());
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(KEY, "Sai")))
                .andExpect(status().isInternalServerError());
        verifyNoInteractions(mail);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("trả về HTTP 400 khi lý do từ chối rỗng hoặc chỉ có khoảng trắng")
    void test_rejectQuestionFile_011(String reason) throws Exception {
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(KEY, reason)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, mail);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi lý do từ chối vượt 2000 ký tự")
    void test_rejectQuestionFile_012() throws Exception {
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(KEY, "a".repeat(2001))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, mail);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "avatars/user@example.com-X.docx",
                "question-files/../x.docx",
                "question-files//x.docx",
                "question-files/physics/x.pdf",
                "question-files/physics/sub/x.docx"
            })
    @DisplayName("trả về HTTP 400 khi key duyệt hoặc từ chối tệp không hợp lệ")
    void test_approveQuestionFile_013(String key) throws Exception {
        mvc.perform(multipart("/moderator/question-files/approve").param("key", key))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(key, "Sai")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, importer, mail);
    }

    @ParameterizedTest
    @ValueSource(strings = {"noemail.docx", "bad-email-X.docx", "user@example.com.docx"})
    @DisplayName("trả về HTTP 400 và giữ tệp khi tên tệp không chứa email hợp lệ")
    void test_rejectQuestionFile_014(String fileName) throws Exception {
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection("question-files/physics/" + fileName, "Sai")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3, mail);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi tệp gốc cần duyệt hoặc từ chối đã biến mất")
    void test_approveQuestionFile_015() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(KEY, "Sai")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(importer, mail);
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_TEACHER", "ROLE_ADMIN"})
    @DisplayName("trả về HTTP 403 khi vai trò không phải moderator gọi API duyệt hoặc từ chối tệp")
    void test_approveQuestionFile_016(String role) throws Exception {
        moderator.setRole(role);
        mvc.perform(multipart("/moderator/question-files/approve").param("key", KEY))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .contentType("application/json")
                                .content(rejection(KEY, "Sai")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(s3, importer, mail);
    }

    @Test
    @DisplayName("trả về HTTP 401 khi yêu cầu duyệt hoặc từ chối tệp có Bearer token rỗng")
    void test_approveQuestionFile_017() throws Exception {
        mvc.perform(
                        multipart("/moderator/question-files/approve")
                                .header("Authorization", "Bearer ")
                                .param("key", KEY))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/moderator/question-files/reject")
                                .header("Authorization", "Bearer ")
                                .contentType("application/json")
                                .content(rejection(KEY, "Sai")))
                .andExpect(status().isUnauthorized());
    }

    private static String rejection(String key, String reason) throws Exception {
        return new tools.jackson.databind.ObjectMapper()
                .writeValueAsString(java.util.Map.of("key", key, "reason", reason));
    }

    private static boolean matches(MultipartFile file, byte[] expected) {
        try {
            return java.util.Arrays.equals(file.getBytes(), expected);
        } catch (Exception e) {
            return false;
        }
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

    @Test
    @DisplayName("trả về HTTP 204 khi xóa đúng key chứa Unicode và ký tự đặc biệt")
    void test_deleteQuestionFile_018() throws Exception {
        String key = "question-files/vật lý/user+1@example.com-Đề thi #1%.docx";
        mvc.perform(delete("/moderator/question-files").queryParam("key", key))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(s3)
                .deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket("test-question-bucket")
                                .key(key)
                                .build());
        verifyNoInteractions(presigner, importer);
    }

    @Test
    @DisplayName("trả về HTTP 204 khi xóa cùng key nhiều lần")
    void test_deleteQuestionFile_019() throws Exception {
        String key = "question-files/math/test.docx";
        for (int i = 0; i < 2; i++) {
            mvc.perform(delete("/moderator/question-files").queryParam("key", key))
                    .andExpect(status().isNoContent());
        }
        verify(s3, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                " ",
                "avatars/user.png",
                "media/test.png",
                "question-files",
                "question-files/",
                "question-files/math/",
                "question-files-other/test.docx",
                "question-files/../avatars/test.png",
                "question-files/./test.docx"
            })
    @DisplayName("trả về HTTP 400 khi key xóa tệp không hợp lệ")
    void test_deleteQuestionFile_020(String key) throws Exception {
        MockHttpServletRequestBuilder request = delete("/moderator/question-files");
        if (key != null) request.queryParam("key", key);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(s3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    @DisplayName("trả về HTTP 403 khi người gọi xóa tệp có vai trò khác moderator")
    void test_deleteQuestionFile_021(String role) throws Exception {
        when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(User.withUsername("user").password("unused").roles(role).build());
        mvc.perform(
                        delete("/moderator/question-files")
                                .queryParam("key", "question-files/test.docx"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(s3);
    }

    @Test
    @DisplayName("trả về HTTP 401 khi Bearer token của yêu cầu xóa tệp rỗng")
    void test_deleteQuestionFile_022() throws Exception {
        mvc.perform(
                        delete("/moderator/question-files")
                                .header("Authorization", "Bearer ")
                                .queryParam("key", "question-files/test.docx"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(s3);
    }

    @Test
    @DisplayName("trả về HTTP 500 với thông báo chung khi S3 từ chối xóa tệp")
    void test_deleteQuestionFile_023() throws Exception {
        when(s3.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(
                        S3Exception.builder()
                                .statusCode(403)
                                .message("private AWS details")
                                .build());
        mvc.perform(
                        delete("/moderator/question-files")
                                .queryParam("key", "question-files/test.docx"))
                .andExpect(status().isInternalServerError())
                .andExpect(
                        jsonPath("$.message")
                                .value("Không thể xóa tệp tin câu hỏi. Vui lòng thử lại"));
    }

    @Test
    @DisplayName("trả về HTTP 200 kèm bytes DOCX, no-store và nosniff khi xem trước tệp")
    void test_getQuestionFileContent_024() throws Exception {
        String key = "question-files/vật lý/user+1-Đề #1%.docx";
        byte[] bytes = {80, 75, 3, 4};
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));
        mvc.perform(get("/moderator/question-files/content").queryParam("key", key))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(
                        content()
                                .contentType(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        verify(s3)
                .getObjectAsBytes(
                        GetObjectRequest.builder().bucket("test-question-bucket").key(key).build());
        verifyNoInteractions(importer, presigner);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                " ",
                "avatars/test.docx",
                "question-files/",
                "question-files/../x.docx",
                "question-files/./x.docx",
                "question-files/x.pdf",
                "question-files-other/x.docx"
            })
    @DisplayName("trả về HTTP 400 khi key xem trước thiếu, sai prefix hoặc sai định dạng DOCX")
    void test_getQuestionFileContent_025(String key) throws Exception {
        MockHttpServletRequestBuilder request = get("/moderator/question-files/content");
        if (key != null) request.queryParam("key", key);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(s3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    @DisplayName("trả về HTTP 403 khi người xem trước tệp có vai trò khác moderator")
    void test_getQuestionFileContent_026(String role) throws Exception {
        when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(User.withUsername("user").password("unused").roles(role).build());
        mvc.perform(
                        get("/moderator/question-files/content")
                                .queryParam("key", "question-files/a.docx"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(s3);
    }

    @Test
    @DisplayName("trả về HTTP 401 khi Bearer token của yêu cầu xem trước rỗng")
    void test_getQuestionFileContent_027() throws Exception {
        mvc.perform(
                        get("/moderator/question-files/content")
                                .header("Authorization", "Bearer ")
                                .queryParam("key", "question-files/a.docx"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(s3);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi tệp xem trước không còn trên S3")
    void test_getQuestionFileContent_028() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());
        mvc.perform(
                        get("/moderator/question-files/content")
                                .queryParam("key", "question-files/a.docx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tệp tin không còn tồn tại"));
    }

    @Test
    @DisplayName("trả về HTTP 500 với thông báo chung khi S3 không cho đọc tệp")
    void test_getQuestionFileContent_029() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(
                        S3Exception.builder()
                                .statusCode(403)
                                .message("private AWS details")
                                .build());
        mvc.perform(
                        get("/moderator/question-files/content")
                                .queryParam("key", "question-files/a.docx"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không thể tải nội dung tệp tin câu hỏi"));
    }

    @Test
    @DisplayName("trả về HTTP 204 khi duyệt câu và HTTP 200 khi từ chối câu, cùng no-store")
    void test_approvePendingQuestion_030() throws Exception {
        String id = "507f1f77bcf86cd799439011";
        String body = "{\"updatedAt\":null,\"reviewRevision\":0}";
        mvc.perform(
                        post("/moderator/questions/" + id + "/approve")
                                .contentType("application/json")
                                .content(body)
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "private, no-store"));
        verify(reviewer).approve(eq(id), argThat(input -> input.getReviewRevision() == 0));
        when(reviewer.reject(eq(id), any()))
                .thenReturn(
                        new com.wedu.exam_creation.moderator.dto.RejectPendingQuestionResponseDTO(
                                "NEEDS_CONSIDERATION", 5));
        mvc.perform(
                        post("/moderator/questions/" + id + "/reject")
                                .contentType("application/json")
                                .content(body)
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$.status").value("NEEDS_CONSIDERATION"))
                .andExpect(jsonPath("$.regenerationCount").value(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    @DisplayName(
            "trả về lỗi xác thực hoặc HTTP 403 khi người gọi không có quyền duyệt hoặc từ chối câu")
    void test_rejectPendingQuestion_031(String action) throws Exception {
        MockHttpServletRequestBuilder request =
                post("/moderator/questions/507f1f77bcf86cd799439011/" + action)
                        .contentType("application/json")
                        .content("{\"updatedAt\":null,\"reviewRevision\":0}");
        mvcWithoutToken.perform(request).andExpect(status().is4xxClientError());
        when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(
                        User.withUsername("teacher").password("unused").roles("TEACHER").build());
        mvc.perform(request.header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reviewer);
    }

    @Test
    @DisplayName("trả về revision và ảnh đã ký khi bản sinh lại có cả key S3 và mô tả AI")
    void test_listPendingQuestions_032() throws Exception {
        PendingQuestionDTO question = new PendingQuestionDTO();
        question.setReviewRevision(2);
        question.setRegenerationCount(1);
        QuestionDTO latest = new QuestionDTO();
        latest.setQuestion(
                new ContentDTO(
                        "<img_0> <img_1>",
                        new VariablesDTO(
                                Map.of(),
                                Map.of("img_0", "media/latest.png", "img_1", "Đồ thị cần vẽ"))));
        question.setLatestRegeneratedQuestion(latest);
        doReturn("https://example.test/latest")
                .when(storage)
                .generatePresignedUrl("media/latest.png");
        when(questions.findPending(0, "", "", List.of(), List.of()))
                .thenReturn(new PendingQuestionPageDTO(List.of(question), 0, 10, 1, 1));
        mvc.perform(
                        get("/moderator/questions/pending")
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reviewRevision").value(2))
                .andExpect(
                        jsonPath(
                                        "$.items[0].latestRegeneratedQuestion.question.variables.image.img_0")
                                .value("https://example.test/latest"))
                .andExpect(
                        jsonPath(
                                        "$.items[0].latestRegeneratedQuestion.question.variables.image.img_1")
                                .value("Đồ thị cần vẽ"));
        verify(storage, times(1)).generatePresignedUrl(anyString());
    }

    @Test
    @DisplayName("trả về mô tả ảnh giữ nguyên khi câu hỏi chỉ có mô tả từ AI")
    void test_listPendingQuestions_033() throws Exception {
        PendingQuestionDTO question = new PendingQuestionDTO();
        question.setQuestion(
                new ContentDTO(
                        "<img_0>", new VariablesDTO(Map.of(), Map.of("img_0", "Đồ thị dao động"))));
        when(questions.findPending(0, "", "", List.of(), List.of()))
                .thenReturn(new PendingQuestionPageDTO(List.of(question), 0, 10, 1, 1));
        mvc.perform(
                        get("/moderator/questions/pending")
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items[0].question.variables.image.img_0")
                                .value("Đồ thị dao động"));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về chín câu trong một trang khi có chín kết quả và ảnh lưu trên S3")
    void test_listPendingQuestions_034() throws Exception {
        doReturn("https://example.test/image")
                .when(storage)
                .generatePresignedUrl("media/image-key.png");
        List<PendingQuestionDTO> items =
                java.util.stream.IntStream.range(0, 9)
                        .mapToObj(
                                index -> {
                                    PendingQuestionDTO entry = new PendingQuestionDTO();
                                    entry.setId("id-" + index);
                                    entry.setStatus("PENDING_REVIEW");
                                    entry.setQuestion(
                                            new ContentDTO(
                                                    "<img_1>",
                                                    new VariablesDTO(
                                                            Map.of(),
                                                            Map.of(
                                                                    "img_1",
                                                                    "media/image-key.png"))));
                                    return entry;
                                })
                        .toList();
        when(questions.findPending(0, "", "", List.of(), List.of()))
                .thenReturn(new PendingQuestionPageDTO(items, 0, 10, 9, 1));
        mvc.perform(
                        get("/moderator/questions/pending")
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$.items.length()").value(9))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(9))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].chapter").value("Chương không còn tồn tại"))
                .andExpect(
                        jsonPath("$.items[0].question.variables.image.img_1")
                                .value("https://example.test/image"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc", "1.5", "2147483648", ""})
    @DisplayName("trả về HTTP 400 khi page không phải số nguyên không âm hợp lệ")
    void test_listPendingQuestions_035(String page) throws Exception {
        mvc.perform(
                        get("/moderator/questions/pending")
                                .param("page", page)
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(questions);
    }

    @Test
    @DisplayName("trả về HTTP 403 khi giáo viên đọc danh sách chờ duyệt")
    void test_listPendingQuestions_036() throws Exception {
        when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(
                        User.withUsername("teacher").password("unused").roles("TEACHER").build());
        mvc.perform(
                        get("/moderator/questions/pending")
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(questions);
    }

    @Test
    @DisplayName("trả về lỗi xác thực khi yêu cầu danh sách chờ duyệt thiếu token")
    void test_listPendingQuestions_037() throws Exception {
        mvcWithoutToken
                .perform(get("/moderator/questions/pending"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(questions);
    }

    @Test
    @DisplayName("trả về trang kết quả đúng khi tìm không phân biệt hoa thường theo chương và bài")
    void test_listPendingQuestions_038() throws Exception {
        com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO lesson =
                new com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO(
                        "lesson", "Sóng cơ", List.of());
        com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO chapter =
                new com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO(
                        "chapter", "Vật lý", "Sóng", List.of(lesson));
        when(chapters.getAll()).thenReturn(List.of(chapter));
        when(questions.findPending(1, "SÓNG", "Đúng sai", List.of("chapter"), List.of("lesson")))
                .thenReturn(new PendingQuestionPageDTO(List.of(), 1, 10, 9, 1));
        mvc.perform(
                        get("/moderator/questions/pending")
                                .header("Authorization", "Bearer test-token")
                                .param("page", "1")
                                .param("search", " SÓNG ")
                                .param("questionType", "Đúng sai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(9));
        verify(questions).findPending(1, "SÓNG", "Đúng sai", List.of("chapter"), List.of("lesson"));
    }

    @Test
    @DisplayName("trả về HTTP 204 khi PATCH có JSON và ảnh thay thế hợp lệ")
    void test_editPendingQuestion_039() throws Exception {
        doReturn("media/questions/new.png")
                .when(storage)
                .uploadImageFile(any(), eq("media/questions/" + ID), eq(5));
        mvc.perform(
                        multipart(HttpMethod.PATCH, "/moderator/questions/" + ID)
                                .file(payload())
                                .file(
                                        new MockMultipartFile(
                                                "question.img_0",
                                                "image.png",
                                                "image/png",
                                                new byte[] {1, 2}))
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "private, no-store"));
        verify(questions)
                .updatePendingContent(
                        eq(ID),
                        argThat(
                                content ->
                                        "media/questions/new.png"
                                                .equals(
                                                        content.getVariables()
                                                                .getImage()
                                                                .get("img_0"))),
                        eq(List.of()),
                        eq(Instant.parse("2026-10-29T00:00:00Z")),
                        eq(0L));
    }

    @Test
    @DisplayName("trả về HTTP 403 khi giáo viên chỉnh sửa câu chờ duyệt")
    void test_editPendingQuestion_040() throws Exception {
        when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(
                        User.withUsername("teacher").password("unused").roles("TEACHER").build());
        mvc.perform(
                        multipart(HttpMethod.PATCH, "/moderator/questions/" + ID)
                                .file(payload())
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về lỗi xác thực khi yêu cầu chỉnh sửa thiếu token")
    void test_editPendingQuestion_041() throws Exception {
        mvcWithoutToken
                .perform(multipart(HttpMethod.PATCH, "/moderator/questions/" + ID).file(payload()))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi thiếu ảnh cần thay thế")
    void test_editPendingQuestion_042() throws Exception {
        mvc.perform(
                        multipart(HttpMethod.PATCH, "/moderator/questions/" + ID)
                                .file(payload())
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        verify(questions, never()).updatePendingContent(any(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("trả về HTTP 409 khi câu hỏi đã hết trạng thái chờ duyệt")
    void test_editPendingQuestion_043() throws Exception {
        when(questions.getPendingForEdit(ID)).thenThrow(new ConflictException("no longer pending"));
        mvc.perform(
                        multipart(HttpMethod.PATCH, "/moderator/questions/" + ID)
                                .file(payload())
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isConflict());
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi payload JSON hoặc ID chỉnh sửa không hợp lệ")
    void test_editPendingQuestion_044() throws Exception {
        mvc.perform(
                        multipart(HttpMethod.PATCH, "/moderator/questions/" + ID)
                                .file(
                                        new MockMultipartFile(
                                                "payload",
                                                "payload.json",
                                                "application/json",
                                                "{".getBytes(StandardCharsets.UTF_8)))
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(HttpMethod.PATCH, "/moderator/questions/invalid")
                                .file(payload())
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về HTTP 200 cùng danh sách tệp khi moderator mở danh sách tài liệu")
    void test_listQuestionFiles_045() throws Exception {
        doReturn(List.of(new S3ObjectInfo(KEY, "DeThi.docx", 10, null, null)))
                .when(storage)
                .listQuestionFiles("question-files");

        mvc.perform(get("/moderator/question-files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value(KEY))
                .andExpect(jsonPath("$[0].fileName").value("DeThi.docx"));
    }

    @Test
    @DisplayName("trả về HTTP 200 cùng ID câu hỏi khi moderator import tài liệu qua multipart")
    void test_parseDocument_046() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "input.docx", "application/octet-stream", original);

        mvc.perform(multipart("/moderator/parse").file(file).param("subject", "physics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("q1"))
                .andExpect(jsonPath("$[1]").value("q2"));
        verify(importer)
                .parseDocument(argThat(document -> matches(document, original)), eq("physics"));
    }

    @Test
    @DisplayName("trả về HTTP 400 khi yêu cầu import thiếu part file")
    void test_parseDocument_047() throws Exception {
        mvc.perform(multipart("/moderator/parse").param("subject", "physics"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(importer);
    }

    private MockMultipartFile payload() {
        String json =
                """
                {"updatedAt":"2026-10-29T00:00:00Z", "question":{"template":"<img_0>",
                "variables":{"math":{},"image":{"img_0":"Updated description"}}}, "options":[]}
                """;
        return new MockMultipartFile(
                "payload",
                "payload.json",
                "application/json",
                json.getBytes(StandardCharsets.UTF_8));
    }
}
