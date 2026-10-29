package com.wedu.exam_creation.moderator.controller;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.mail.service.MailService;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModeratorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ModeratorUsecase.class})
class ModeratorPendingQuestionsTest {
    @MockitoBean private com.wedu.exam_creation.moderator.usecase.EditPendingQuestionUsecase editor;
    @Autowired private MockMvc mvc;
    @MockitoBean private CustomOAuth2UserService oauth;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private ImporterService importer;
    @MockitoBean private MailService mail;
    @MockitoBean private S3Service storage;
    @MockitoBean private ChapterService chapters;
    @MockitoBean private QuestionService questions;

    @BeforeEach
    void setup() {
        when(tokens.getPayloadFromAccessToken("test-token")).thenReturn(new ATPayload("jti", "moderator", "moderator@example.com", "ROLE_MODERATOR"));
        when(users.loadUserByUsername("moderator@example.com")).thenReturn(User.withUsername("moderator").password("unused").roles("MODERATOR").build());
        when(chapters.getAll()).thenReturn(List.of());
    }

    @Test
    void keepsAiImageDescriptionAsTextWithoutSigningIt() throws Exception {
        var question = new PendingQuestionDTO();
        question.setQuestion(new ContentDTO("<img_0>", new VariablesDTO(Map.of(), Map.of("img_0", "Đồ thị dao động"))));
        when(questions.findPending(0, "", "", List.of(), List.of()))
                .thenReturn(new PendingQuestionPageDTO(List.of(question), 0, 10, 1, 1));
        mvc.perform(get("/moderator/questions/pending").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].question.variables.image.img_0").value("Đồ thị dao động"));
        verifyNoInteractions(storage);
    }

    @Test
    void returnsNinePendingQuestionsOnOnePageAndSignsImages() throws Exception {
        when(storage.generatePresignedUrl("media/image-key.png")).thenReturn("https://example.test/image");
        var items = java.util.stream.IntStream.range(0, 9).mapToObj(index -> {
            var entry = new PendingQuestionDTO();
            entry.setId("id-" + index);
            entry.setStatus("PENDING_REVIEW");
            entry.setQuestion(new ContentDTO("<img_1>", new VariablesDTO(Map.of(), Map.of("img_1", "media/image-key.png"))));
            return entry;
        }).toList();
        when(questions.findPending(0, "", "", List.of(), List.of())).thenReturn(new PendingQuestionPageDTO(items, 0, 10, 9, 1));
        mvc.perform(get("/moderator/questions/pending").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$.items.length()").value(9)).andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(9)).andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].chapter").value("Chương không còn tồn tại"))
                .andExpect(jsonPath("$.items[0].question.variables.image.img_1").value("https://example.test/image"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc", "1.5", "2147483648", ""})
    void rejectsInvalidPage(String page) throws Exception {
        mvc.perform(get("/moderator/questions/pending").param("page", page).header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(questions);
    }

    @Test
    void teacherCannotReadQueue() throws Exception {
        when(users.loadUserByUsername("moderator@example.com")).thenReturn(User.withUsername("teacher").password("unused").roles("TEACHER").build());
        mvc.perform(get("/moderator/questions/pending").header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(questions);
    }

    @Test
    void missingTokenCannotReadQueue() throws Exception {
        mvc.perform(get("/moderator/questions/pending")).andExpect(status().is4xxClientError());
        verifyNoInteractions(questions);
    }

    @Test
    void searchMatchesChapterAndLessonNamesBeforePagination() throws Exception {
        var lesson = new com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO("lesson", "Sóng cơ", List.of());
        var chapter = new com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO("chapter", "Vật lý", "Sóng", List.of(lesson));
        when(chapters.getAll()).thenReturn(List.of(chapter));
        when(questions.findPending(1, "SÓNG", "Đúng sai", List.of("chapter"), List.of("lesson")))
                .thenReturn(new PendingQuestionPageDTO(List.of(), 1, 10, 9, 1));
        mvc.perform(get("/moderator/questions/pending").header("Authorization", "Bearer test-token")
                        .param("page", "1").param("search", " SÓNG ").param("questionType", "Đúng sai"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.totalElements").value(9));
        verify(questions).findPending(1, "SÓNG", "Đúng sai", List.of("chapter"), List.of("lesson"));
    }
}
