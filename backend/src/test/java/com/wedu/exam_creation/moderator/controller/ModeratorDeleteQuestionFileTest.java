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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModeratorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ModeratorUsecase.class, S3Service.class})
@TestPropertySource(properties = "aws.bucketName=test-question-bucket")
class ModeratorDeleteQuestionFileTest {
    @MockitoBean private com.wedu.exam_creation.moderator.usecase.EditPendingQuestionUsecase editor;
    @MockitoBean private com.wedu.exam_creation.chapter.usecase.ChapterService chapters;
    @MockitoBean private com.wedu.exam_creation.question.usecase.QuestionService questions;
    @Autowired private MockMvc mvc;
    @Autowired private WebApplicationContext context;
    @MockitoBean private CustomOAuth2UserService oauthService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private ImporterService importer;
    @MockitoBean private com.wedu.exam_creation.mail.service.MailService mail;
    @MockitoBean private S3Client s3;
    @MockitoBean private S3Presigner presigner;

    @BeforeEach
    void authenticate() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(delete("/").header("Authorization", "Bearer test-token"))
                .build();
        lenient().when(tokens.getPayloadFromAccessToken("test-token"))
                .thenReturn(new ATPayload("jti", "moderator", "moderator@example.com", "ROLE_MODERATOR"));
        lenient().when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(User.withUsername("moderator").password("unused").roles("MODERATOR").build());
    }

    @Test
    void deletesExactKeyAndReturnsNoContent() throws Exception {
        String key = "question-files/vật lý/user+1@example.com-Đề thi #1%.docx";
        mvc.perform(delete("/moderator/question-files").queryParam("key", key))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("test-question-bucket").key(key).build());
        verifyNoInteractions(presigner, importer);
    }

    @Test
    void repeatedDeletionRemainsSuccessful() throws Exception {
        String key = "question-files/math/test.docx";
        for (int i = 0; i < 2; i++) {
            mvc.perform(delete("/moderator/question-files").queryParam("key", key))
                    .andExpect(status().isNoContent());
        }
        verify(s3, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "avatars/user.png", "media/test.png", "question-files",
            "question-files/", "question-files/math/", "question-files-other/test.docx",
            "question-files/../avatars/test.png", "question-files/./test.docx"})
    void rejectsInvalidKeysWithoutCallingS3(String key) throws Exception {
        var request = delete("/moderator/question-files");
        if (key != null) request.queryParam("key", key);
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(s3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN"})
    void rejectsOtherRoles(String role) throws Exception {
        when(users.loadUserByUsername("moderator@example.com"))
                .thenReturn(User.withUsername("user").password("unused").roles(role).build());
        mvc.perform(delete("/moderator/question-files").queryParam("key", "question-files/test.docx"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(s3);
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mvc.perform(delete("/moderator/question-files").header("Authorization", "Bearer ")
                        .queryParam("key", "question-files/test.docx"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(s3);
    }

    @Test
    void hidesAwsFailureDetails() throws Exception {
        when(s3.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("private AWS details").build());
        mvc.perform(delete("/moderator/question-files").queryParam("key", "question-files/test.docx"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không thể xóa tệp tin câu hỏi. Vui lòng thử lại"));
    }
}
