package com.wedu.exam_creation.storage.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.storage.service.S3Service;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StorageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class StorageControllerTest {
    private static final String AVATAR_PATH = "/storage/avatar";
    private static final String DOCUMENT_PATH = "/storage/question-file";
    private static final String EMAIL = "teacher@example.com";
    private static final String TOKEN = "test-access-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String SUBJECT = "math";
    private static final String DOCX_FOLDER = "question-files/" + SUBJECT;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @MockitoBean private S3Service s3Service;

    @Test
    @DisplayName("trả về HTTP 200 và khóa avatar khi người đã xác thực tải ảnh hợp lệ")
    void test_uploadAvatar_001() throws Exception {
        authenticateTeacher();
        MockMultipartFile file = avatar();
        String key = "avatars/sample.png";
        when(s3Service.uploadImageFile(file, "avatars", 5)).thenReturn(key);

        mockMvc.perform(multipart(AVATAR_PATH).file(file).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(content().string(key));
    }

    @Test
    @DisplayName("trả về HTTP 400 khi request tải avatar thiếu tiêu đề Authorization")
    void test_uploadAvatar_002() throws Exception {
        mockMvc.perform(multipart(AVATAR_PATH).file(avatar()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("trả về HTTP 405 khi gọi đường dẫn tải avatar bằng GET")
    void test_uploadAvatar_003() throws Exception {
        authenticateTeacher();

        mockMvc.perform(get(AVATAR_PATH).header("Authorization", BEARER))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("trả về HTTP 500 với thông báo tải avatar thất bại khi service ném IOException")
    void test_uploadAvatar_004() throws Exception {
        authenticateTeacher();
        MockMultipartFile file = avatar();
        when(s3Service.uploadImageFile(file, "avatars", 5))
                .thenThrow(new IOException("read failed"));

        mockMvc.perform(multipart(AVATAR_PATH).file(file).header("Authorization", BEARER))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Upload avatar thất bại"));
    }

    @Test
    @DisplayName("trả về HTTP 400 khi request tải avatar thiếu phần multipart file")
    void test_uploadAvatar_005() throws Exception {
        authenticateTeacher();

        mockMvc.perform(multipart(AVATAR_PATH).header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("trả về HTTP 400 và thông báo lỗi định dạng khi service từ chối ảnh")
    void test_uploadAvatar_006() throws Exception {
        authenticateTeacher();
        MockMultipartFile file = avatar();
        String message = "Lỗi: File không đúng định dạng";
        when(s3Service.uploadImageFile(file, "avatars", 5))
                .thenThrow(new BadRequestException(message));

        mockMvc.perform(multipart(AVATAR_PATH).file(file).header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @DisplayName(
            "trả về HTTP 200 và khóa theo email tài khoản khi tải DOCX kèm subject và tham số email khác")
    void test_uploadDocumentToQuestionBank_007() throws Exception {
        authenticateTeacher();
        MockMultipartFile file = document();
        String key = DOCX_FOLDER + "/" + EMAIL + "-Test(2).docx";
        when(s3Service.updateDocumentFile(file, DOCX_FOLDER, 50, EMAIL)).thenReturn(key);

        mockMvc.perform(
                        multipart(DOCUMENT_PATH)
                                .file(file)
                                .param("subject", SUBJECT)
                                .param("email", "other@example.com")
                                .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(content().string(key));
    }

    @Test
    @DisplayName("trả về HTTP 400 khi request tải DOCX thiếu subject")
    void test_uploadDocumentToQuestionBank_008() throws Exception {
        authenticateTeacher();

        mockMvc.perform(multipart(DOCUMENT_PATH).file(document()).header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi request tải DOCX thiếu phần multipart file")
    void test_uploadDocumentToQuestionBank_009() throws Exception {
        authenticateTeacher();

        mockMvc.perform(
                        multipart(DOCUMENT_PATH)
                                .param("subject", SUBJECT)
                                .header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi request tải DOCX thiếu tiêu đề Authorization")
    void test_uploadDocumentToQuestionBank_010() throws Exception {
        mockMvc.perform(multipart(DOCUMENT_PATH).file(document()).param("subject", SUBJECT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("trả về HTTP 500 với thông báo tải tài liệu thất bại khi service ném IOException")
    void test_uploadDocumentToQuestionBank_011() throws Exception {
        authenticateTeacher();
        MockMultipartFile file = document();
        when(s3Service.updateDocumentFile(file, DOCX_FOLDER, 50, EMAIL))
                .thenThrow(new IOException("read failed"));

        mockMvc.perform(
                        multipart(DOCUMENT_PATH)
                                .file(file)
                                .param("subject", SUBJECT)
                                .header("Authorization", BEARER))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Upload document thất bại"));
    }

    @Test
    @DisplayName("trả về HTTP 400 và thông báo lỗi định dạng khi service từ chối DOCX")
    void test_uploadDocumentToQuestionBank_012() throws Exception {
        authenticateTeacher();
        MockMultipartFile file = document();
        String message = "Lỗi: Chỉ chấp nhận tài liệu Word (.docx)";
        when(s3Service.updateDocumentFile(file, DOCX_FOLDER, 50, EMAIL))
                .thenThrow(new BadRequestException(message));

        mockMvc.perform(
                        multipart(DOCUMENT_PATH)
                                .file(file)
                                .param("subject", SUBJECT)
                                .header("Authorization", BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    private void authenticateTeacher() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        CommonUserResponseAllDTO user =
                new CommonUserResponseAllDTO(
                        "teacher-1",
                        EMAIL,
                        "unused",
                        "Giáo viên",
                        "ROLE_TEACHER",
                        "LOCAL",
                        "avatars/default.png",
                        true,
                        "FREE",
                        timestamp,
                        timestamp,
                        null);
        when(jwtTokenProvider.getPayloadFromAccessToken(TOKEN))
                .thenReturn(new ATPayload("test-session", "teacher-1", EMAIL, "ROLE_TEACHER"));
        when(customUserDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(new CustomUserDetails(user));
    }

    private MockMultipartFile avatar() {
        return new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[] {1});
    }

    private MockMultipartFile document() {
        return new MockMultipartFile(
                "file", "test.docx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[] {1});
    }
}
