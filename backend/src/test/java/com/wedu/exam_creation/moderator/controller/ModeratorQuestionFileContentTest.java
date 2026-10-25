package com.wedu.exam_creation.moderator.controller;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.moderator.usecase.ModeratorUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModeratorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ModeratorUsecase.class, S3Service.class})
@TestPropertySource(properties = "aws.bucketName=test-question-bucket")
class ModeratorQuestionFileContentTest {
    @Autowired private MockMvc mvc;
    @Autowired private WebApplicationContext context;
    @MockitoBean private CustomOAuth2UserService oauthService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private ImporterService importer;
    @MockitoBean private S3Client s3;
    @MockitoBean private S3Presigner presigner;

    @BeforeEach
    void authenticate() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").header("Authorization", "Bearer test-token"))
                .build();
        lenient().when(tokens.getPayloadFromAccessToken("test-token"))
                .thenReturn(new ATPayload("jti", "moderator", "moderator@example.com", "ROLE_MODERATOR"));
        lenient().when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(User.withUsername("moderator").password("unused").roles("MODERATOR").build());
    }

    @Test
    void returnsExactDocumentWithoutImporting() throws Exception {
        String key = "question-files/vật lý/user+1-Đề #1%.docx";
        byte[] bytes = {80, 75, 3, 4};
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes));
        mvc.perform(get("/moderator/question-files/content").queryParam("key", key))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        verify(s3).getObjectAsBytes(GetObjectRequest.builder().bucket("test-question-bucket").key(key).build());
        verifyNoInteractions(importer, presigner);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "avatars/test.docx", "question-files/", "question-files/../x.docx",
            "question-files/./x.docx", "question-files/x.pdf", "question-files-other/x.docx"})
    void rejectsInvalidKeys(String key) throws Exception {
        var request = get("/moderator/question-files/content");
        if (key != null) request.queryParam("key", key);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(s3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void rejectsOtherRoles(String role) throws Exception {
        when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(User.withUsername("user").password("unused").roles(role).build());
        mvc.perform(get("/moderator/question-files/content").queryParam("key", "question-files/a.docx"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(s3);
    }

    @Test
    void rejectsMissingToken() throws Exception {
        mvc.perform(get("/moderator/question-files/content").header("Authorization", "Bearer ")
                        .queryParam("key", "question-files/a.docx"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(s3);
    }

    @Test
    void reportsMissingObject() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());
        mvc.perform(get("/moderator/question-files/content").queryParam("key", "question-files/a.docx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tệp tin không còn tồn tại"));
    }

    @Test
    void hidesStorageErrors() throws Exception {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("private AWS details").build());
        mvc.perform(get("/moderator/question-files/content").queryParam("key", "question-files/a.docx"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không thể tải nội dung tệp tin câu hỏi"));
    }
}
