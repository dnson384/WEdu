package com.wedu.exam_creation.chapter.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.chapter.usecase.ChapterUsecase;
import com.wedu.exam_creation.common.dto.chapter.response.*;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ChapterController.class, properties = "app.frontend.url=http://localhost:3000")
@Import(SecurityConfig.class)
class ChapterControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ChapterUsecase usecase;
    @MockitoBean private JwtAuthenticationFilter jwt;
    @MockitoBean private CustomOAuth2UserService oidc;
    @MockitoBean private OAuth2AuthenticationSuccessHandler success;
    @MockitoBean private ClientRegistrationRepository clients;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(
                        call -> {
                            FilterChain chain = call.getArgument(2);
                            chain.doFilter(call.getArgument(0), call.getArgument(1));
                            return null;
                        })
                .when(jwt)
                .doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("trả về 200 và JSON chương bài bankStats đầy đủ khi đã xác thực")
    void test_getAll_001() throws Exception {
        when(usecase.getAll())
                .thenReturn(
                        List.of(
                                new ChapterResponseDTO(
                                        "c1",
                                        "Vật lý",
                                        "Chương 1",
                                        List.of(
                                                new LessonDataResponseDTO(
                                                        "l1",
                                                        "Bài 1",
                                                        List.of(
                                                                new BankStatResponseDTO(
                                                                        "Dạng 1",
                                                                        List.of("Nhận biết"),
                                                                        List.of("Yêu cầu 1"),
                                                                        "Đúng sai",
                                                                        3)))))));
        mvc.perform(get("/chapter/chapters").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        """
                        [{"id":"c1","subject":"Vật lý","name":"Chương 1","lessons":[{"id":"l1","name":"Bài 1","bankStats":[{"exerciseType":"Dạng 1","difficultyLevels":["Nhận biết"],"learningOutcomes":["Yêu cầu 1"],"questionType":"Đúng sai","count":3}]}]}]
                        """));
        verify(usecase).getAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR", "ADMIN"})
    @DisplayName("trả về 200 và danh sách rỗng khi user đã xác thực và chưa có chương")
    void test_getAll_002(String role) throws Exception {
        when(usecase.getAll()).thenReturn(List.of());
        mvc.perform(get("/chapter/chapters").with(user("reader").roles(role)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_getAll_003() throws Exception {
        mvc.perform(get("/chapter/chapters")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi usecase báo không tìm thấy")
    void test_getAll_004() throws Exception {
        when(usecase.getAll()).thenThrow(new NotFoundException("Không tồn tại chương này"));
        mvc.perform(get("/chapter/chapters").with(user("reader")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại chương này"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency không đọc được dữ liệu")
    void test_getAll_005() throws Exception {
        when(usecase.getAll()).thenThrow(new InternalServerException("read failed"));
        mvc.perform(get("/chapter/chapters").with(user("reader")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 405 và không gọi usecase khi dùng POST cho API chỉ đọc")
    void test_getAll_006() throws Exception {
        mvc.perform(post("/chapter/chapters").with(user("reader")))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(usecase);
    }
}
