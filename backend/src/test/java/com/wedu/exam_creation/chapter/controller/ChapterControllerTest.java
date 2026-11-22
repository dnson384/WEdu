package com.wedu.exam_creation.chapter.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.chapter.usecase.ChapterUsecase;
import com.wedu.exam_creation.common.dto.chapter.response.*;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import java.time.Instant;
import java.util.List;
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
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ChapterControllerTest {
    private static final String PATH = "/chapter/chapters";
    private static final String TOKEN = "test-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String EMAIL = "teacher@example.com";
    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");

    @Autowired private MockMvc mvc;
    @MockitoBean private ChapterUsecase usecase;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private CustomOAuth2UserService oidc;
    @MockitoBean private OAuth2AuthenticationSuccessHandler success;
    @MockitoBean private ClientRegistrationRepository clients;

    @Test
    @DisplayName("trả về 200 và JSON chương bài bankStats đầy đủ khi đã xác thực")
    void test_getAll_001() throws Exception {
        authenticate("TEACHER", true);
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
        mvc.perform(get(PATH).header("Authorization", BEARER))
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
        authenticate(role, true);
        when(usecase.getAll()).thenReturn(List.of());

        mvc.perform(get(PATH).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("trả về HTTP 400 và không đọc chương khi thiếu tiêu đề Authorization")
    void test_getAll_003() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi usecase báo không tìm thấy")
    void test_getAll_004() throws Exception {
        authenticate("TEACHER", true);
        when(usecase.getAll()).thenThrow(new NotFoundException("Không tồn tại chương này"));

        mvc.perform(get(PATH).header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại chương này"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency không đọc được dữ liệu")
    void test_getAll_005() throws Exception {
        authenticate("TEACHER", true);
        when(usecase.getAll()).thenThrow(new InternalServerException("read failed"));

        mvc.perform(get(PATH).header("Authorization", BEARER))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 405 và không gọi usecase khi dùng POST cho API chỉ đọc")
    void test_getAll_006() throws Exception {
        authenticate("TEACHER", true);

        mvc.perform(post(PATH).header("Authorization", BEARER))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 401 và không đọc chương khi JWT bị bộ xác thực từ chối")
    void test_getAll_007() throws Exception {
        String message = "Token không hợp lệ";
        doThrow(new UnAuthorizedException(message)).when(tokens).validateAccessToken(TOKEN);

        mvc.perform(get(PATH).header("Authorization", BEARER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(message));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 403 và không đọc chương khi tài khoản đã bị khóa")
    void test_getAll_008() throws Exception {
        authenticate("TEACHER", false);

        mvc.perform(get(PATH).header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa"));
        verifyNoInteractions(usecase);
    }

    private void authenticate(String role, boolean active) {
        String authority = "ROLE_" + role;
        CommonUserResponseAllDTO account =
                new CommonUserResponseAllDTO(
                        "reader-1",
                        EMAIL,
                        "unused-hash",
                        "Giáo viên",
                        authority,
                        "LOCAL",
                        "avatars/default.png",
                        active,
                        "FREE",
                        NOW,
                        NOW,
                        null);
        when(tokens.getPayloadFromAccessToken(TOKEN))
                .thenReturn(new ATPayload("session-1", "reader-1", EMAIL, authority));
        when(users.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(account));
    }
}
