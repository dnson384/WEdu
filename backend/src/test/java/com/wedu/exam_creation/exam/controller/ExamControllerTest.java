package com.wedu.exam_creation.exam.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.exam.response.*;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.exam.dto.response.*;
import com.wedu.exam_creation.exam.usecase.*;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ExamController.class, properties = "app.frontend.url=http://localhost:3000")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ExamControllerTest {
    private static final String TOKEN = "test-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String EMAIL = "teacher@example.com";
    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");

    @Autowired private MockMvc mvc;
    @MockitoBean private ExamUsecase usecase;
    @MockitoBean private ExamService service;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private CustomOAuth2UserService oidc;
    @MockitoBean private OAuth2AuthenticationSuccessHandler success;
    @MockitoBean private ClientRegistrationRepository clients;

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi generateExam nhận principal hợp lệ")
    void test_generateExam_001() throws Exception {
        authenticate(true);
        when(usecase.generateExam("draft", "owner", "PLUS"))
                .thenReturn(new ExamGeneratedResponseDTO("exam", List.of("Thiếu câu")));
        mvc.perform(post("/exam/generate/draft").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"examId\":\"exam\",\"errors\":[\"Thiếu câu\"]}"));
        verify(usecase).generateExam("draft", "owner", "PLUS");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và không gọi nghiệp vụ khi generateExam thiếu tiêu đề Authorization")
    void test_generateExam_002() throws Exception {
        mvc.perform(post("/exam/generate/draft"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi generateExam không tìm thấy dữ liệu thuộc owner")
    void test_generateExam_003() throws Exception {
        authenticate(true);
        when(usecase.generateExam("draft", "owner", "PLUS"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(post("/exam/generate/draft").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của generateExam gặp lỗi")
    void test_generateExam_004() throws Exception {
        authenticate(true);
        when(usecase.generateExam("draft", "owner", "PLUS"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(post("/exam/generate/draft").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi getExamById nhận principal hợp lệ")
    void test_getExamById_005() throws Exception {
        authenticate(true);
        when(service.getExamById("owner", "exam"))
                .thenReturn(new ExamDetailDTO("exam", "owner", "Đề", List.of()));
        mvc.perform(get("/exam/exam").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"id\":\"exam\",\"userId\":\"owner\",\"name\":\"Đề\",\"groups\":[]}"));
        verify(service).getExamById("owner", "exam");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và không gọi nghiệp vụ khi getExamById thiếu tiêu đề Authorization")
    void test_getExamById_006() throws Exception {
        mvc.perform(get("/exam/exam"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi getExamById không tìm thấy dữ liệu thuộc owner")
    void test_getExamById_007() throws Exception {
        authenticate(true);
        when(service.getExamById("owner", "exam"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(get("/exam/exam").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của getExamById gặp lỗi")
    void test_getExamById_008() throws Exception {
        authenticate(true);
        when(service.getExamById("owner", "exam"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(get("/exam/exam").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi getAllExams nhận principal hợp lệ")
    void test_getAllExams_009() throws Exception {
        authenticate(true);
        when(usecase.getAllUserExams("owner"))
                .thenReturn(
                        List.of(
                                new ExamDTO(
                                        "exam",
                                        "Đề",
                                        List.of(new ExamChapterDTO("c", List.of("l"))),
                                        2)));
        mvc.perform(get("/exam/user-exams").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "[{\"id\":\"exam\",\"name\":\"Đề\",\"chapters\":[{\"id\":\"c\",\"lessonIds\":[\"l\"]}],\"questionsCount\":2}]"));
        verify(usecase).getAllUserExams("owner");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và không gọi nghiệp vụ khi getAllExams thiếu tiêu đề Authorization")
    void test_getAllExams_010() throws Exception {
        mvc.perform(get("/exam/user-exams"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi getAllExams không tìm thấy dữ liệu thuộc owner")
    void test_getAllExams_011() throws Exception {
        authenticate(true);
        when(usecase.getAllUserExams("owner"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(get("/exam/user-exams").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của getAllExams gặp lỗi")
    void test_getAllExams_012() throws Exception {
        authenticate(true);
        when(usecase.getAllUserExams("owner"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(get("/exam/user-exams").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi getRecentExam nhận principal hợp lệ")
    void test_getRecentExam_013() throws Exception {
        authenticate(true);
        when(usecase.getRecentExams("owner"))
                .thenReturn(List.of(new ExamDTO("exam", "Đề", List.of(), 1)));
        mvc.perform(get("/exam/recent").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "[{\"id\":\"exam\",\"name\":\"Đề\",\"chapters\":[],\"questionsCount\":1}]"));
        verify(usecase).getRecentExams("owner");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và không gọi nghiệp vụ khi getRecentExam thiếu tiêu đề Authorization")
    void test_getRecentExam_014() throws Exception {
        mvc.perform(get("/exam/recent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi getRecentExam không tìm thấy dữ liệu thuộc owner")
    void test_getRecentExam_015() throws Exception {
        authenticate(true);
        when(usecase.getRecentExams("owner"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(get("/exam/recent").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của getRecentExam gặp lỗi")
    void test_getRecentExam_016() throws Exception {
        authenticate(true);
        when(usecase.getRecentExams("owner"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(get("/exam/recent").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi deleteExam nhận principal hợp lệ")
    void test_deleteExam_017() throws Exception {
        authenticate(true);
        when(usecase.deleteExam("owner", "exam")).thenReturn(true);
        mvc.perform(delete("/exam/exam").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).deleteExam("owner", "exam");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và không gọi nghiệp vụ khi deleteExam thiếu tiêu đề Authorization")
    void test_deleteExam_018() throws Exception {
        mvc.perform(delete("/exam/exam"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi deleteExam không tìm thấy dữ liệu thuộc owner")
    void test_deleteExam_019() throws Exception {
        authenticate(true);
        when(usecase.deleteExam("owner", "exam"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(delete("/exam/exam").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của deleteExam gặp lỗi")
    void test_deleteExam_020() throws Exception {
        authenticate(true);
        when(usecase.deleteExam("owner", "exam"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(delete("/exam/exam").header("Authorization", BEARER))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về HTTP 401 và không sinh đề khi JWT bị bộ xác thực từ chối")
    void test_generateExam_021() throws Exception {
        String message = "Token không hợp lệ";
        doThrow(new UnAuthorizedException(message)).when(tokens).validateAccessToken(TOKEN);

        mvc.perform(post("/exam/generate/draft").header("Authorization", BEARER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(message));
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về HTTP 403 và không xóa đề khi tài khoản đã bị khóa")
    void test_deleteExam_022() throws Exception {
        authenticate(false);

        mvc.perform(delete("/exam/exam").header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa"));
        verifyNoInteractions(usecase, service);
    }

    private void authenticate(boolean active) {
        CommonUserResponseAllDTO account =
                new CommonUserResponseAllDTO(
                        "owner",
                        EMAIL,
                        "unused-hash",
                        "Giáo viên",
                        "ROLE_TEACHER",
                        "LOCAL",
                        "avatars/default.png",
                        active,
                        "PLUS",
                        NOW,
                        NOW,
                        null);
        when(tokens.getPayloadFromAccessToken(TOKEN))
                .thenReturn(new ATPayload("session-1", "owner", EMAIL, "ROLE_TEACHER"));
        when(users.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(account));
    }
}
