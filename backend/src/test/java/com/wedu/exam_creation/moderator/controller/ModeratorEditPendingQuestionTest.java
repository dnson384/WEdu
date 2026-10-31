package com.wedu.exam_creation.moderator.controller;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.moderator.usecase.EditPendingQuestionUsecase;
import com.wedu.exam_creation.moderator.usecase.ModeratorUsecase;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.storage.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModeratorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, EditPendingQuestionUsecase.class})
class ModeratorEditPendingQuestionTest {
    private static final String ID = "507f1f77bcf86cd799439011";
    @MockitoBean private com.wedu.exam_creation.moderator.usecase.ReviewPendingQuestionUsecase reviewer;
    @Autowired private MockMvc mvc;
    @MockitoBean private ModeratorUsecase moderator;
    @MockitoBean private QuestionService questions;
    @MockitoBean private S3Service storage;
    @MockitoBean private CustomOAuth2UserService oauth;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;

    @BeforeEach
    void setup() {
        when(tokens.getPayloadFromAccessToken("test-token")).thenReturn(new ATPayload("jti", "moderator", "moderator@example.com", "ROLE_MODERATOR"));
        when(users.loadUserByUsername("moderator@example.com")).thenReturn(User.withUsername("moderator").password("unused").roles("MODERATOR").build());
        var original = new PendingQuestionDTO();
        original.setUpdatedAt(Instant.parse("2026-10-29T00:00:00Z"));
        original.setQuestion(new ContentDTO("<img_0>", new VariablesDTO(Map.of(), Map.of("img_0", "AI description"))));
        original.setOptions(List.of());
        when(questions.getPendingForEdit(ID)).thenReturn(original);
    }

    @Test
    void patchesJsonAndFilePartsAndReturnsNoContent() throws Exception {
        when(storage.uploadImageFile(any(), eq("media/questions/" + ID), eq(5))).thenReturn("media/questions/new.png");
        mvc.perform(multipart(HttpMethod.PATCH, "/moderator/questions/" + ID).file(payload())
                        .file(new MockMultipartFile("question.img_0", "image.png", "image/png", new byte[]{1, 2}))
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent()).andExpect(header().string("Cache-Control", "private, no-store"));
        verify(questions).updatePendingContent(eq(ID), argThat(content -> "media/questions/new.png".equals(content.getVariables().getImage().get("img_0"))), eq(List.of()), eq(Instant.parse("2026-10-29T00:00:00Z")), eq(0L));
    }

    @Test
    void teacherCannotPatch() throws Exception {
        when(users.loadUserByUsername("moderator@example.com")).thenReturn(User.withUsername("teacher").password("unused").roles("TEACHER").build());
        mvc.perform(multipart(HttpMethod.PATCH, "/moderator/questions/" + ID).file(payload()).header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(storage);
    }

    @Test
    void unauthenticatedCannotPatch() throws Exception {
        mvc.perform(multipart(HttpMethod.PATCH, "/moderator/questions/" + ID).file(payload())).andExpect(status().is4xxClientError());
        verifyNoInteractions(storage);
    }

    @Test
    void missingImageIsRejectedBeforeUpdating() throws Exception {
        mvc.perform(multipart(HttpMethod.PATCH, "/moderator/questions/" + ID).file(payload()).header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        verify(questions, never()).updatePendingContent(any(), any(), any(), any(), anyLong());
    }

    @Test
    void noLongerPendingReturnsConflict() throws Exception {
        when(questions.getPendingForEdit(ID)).thenThrow(new ConflictException("no longer pending"));
        mvc.perform(multipart(HttpMethod.PATCH, "/moderator/questions/" + ID).file(payload()).header("Authorization", "Bearer test-token"))
                .andExpect(status().isConflict());
        verifyNoInteractions(storage);
    }

    @Test
    void malformedJsonAndInvalidIdAreRejected() throws Exception {
        mvc.perform(multipart(HttpMethod.PATCH, "/moderator/questions/" + ID)
                        .file(new MockMultipartFile("payload", "payload.json", "application/json", "{".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(HttpMethod.PATCH, "/moderator/questions/invalid").file(payload()).header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
    }

    private MockMultipartFile payload() {
        String json = """
                {"updatedAt":"2026-10-29T00:00:00Z", "question":{"template":"<img_0>",
                "variables":{"math":{},"image":{"img_0":"Updated description"}}}, "options":[]}
                """;
        return new MockMultipartFile("payload", "payload.json", "application/json", json.getBytes(StandardCharsets.UTF_8));
    }
}
