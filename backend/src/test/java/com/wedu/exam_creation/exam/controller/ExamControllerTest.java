package com.wedu.exam_creation.exam.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.exam.response.*;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.exam.dto.response.*;
import com.wedu.exam_creation.exam.usecase.*;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ExamController.class, properties = "app.frontend.url=http://localhost:3000")
@Import(SecurityConfig.class)
class ExamControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ExamUsecase usecase;
    @MockitoBean private ExamService service;
    @MockitoBean private JwtAuthenticationFilter jwt;
    @MockitoBean private CustomOAuth2UserService oidc;
    @MockitoBean private OAuth2AuthenticationSuccessHandler success;
    @MockitoBean private ClientRegistrationRepository clients;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() throws Exception {
        CommonUserResponseAllDTO owner = mock(CommonUserResponseAllDTO.class);
        when(owner.getId()).thenReturn("owner");
        when(owner.getAccountType()).thenReturn("PLUS");
        principal = mock(CustomUserDetails.class);
        when(principal.getUser()).thenReturn(owner);
        when(principal.getUsername()).thenReturn("teacher@example.com");
        when(principal.getAuthorities()).thenReturn(List.of());
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
    @DisplayName("trả về 200 đúng JSON contract khi generateExam nhận principal hợp lệ")
    void test_generateExam_001() throws Exception {
        when(usecase.generateExam("draft", "owner", "PLUS"))
                .thenReturn(new ExamGeneratedResponseDTO("exam", List.of("Thiếu câu")));
        mvc.perform(post("/exam/generate/draft").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"examId\":\"exam\",\"errors\":[\"Thiếu câu\"]}"));
        verify(usecase).generateExam("draft", "owner", "PLUS");
    }

    @Test
    @DisplayName("trả về 401 và không gọi nghiệp vụ khi generateExam chưa xác thực")
    void test_generateExam_002() throws Exception {
        mvc.perform(post("/exam/generate/draft")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi generateExam không tìm thấy dữ liệu thuộc owner")
    void test_generateExam_003() throws Exception {
        when(usecase.generateExam("draft", "owner", "PLUS"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(post("/exam/generate/draft").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của generateExam gặp lỗi")
    void test_generateExam_004() throws Exception {
        when(usecase.generateExam("draft", "owner", "PLUS"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(post("/exam/generate/draft").with(user(principal)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi getExamById nhận principal hợp lệ")
    void test_getExamById_001() throws Exception {
        when(service.getExamById("owner", "exam"))
                .thenReturn(new ExamDetailDTO("exam", "owner", "Đề", List.of()));
        mvc.perform(get("/exam/exam").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"id\":\"exam\",\"userId\":\"owner\",\"name\":\"Đề\",\"groups\":[]}"));
        verify(service).getExamById("owner", "exam");
    }

    @Test
    @DisplayName("trả về 401 và không gọi nghiệp vụ khi getExamById chưa xác thực")
    void test_getExamById_002() throws Exception {
        mvc.perform(get("/exam/exam")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi getExamById không tìm thấy dữ liệu thuộc owner")
    void test_getExamById_003() throws Exception {
        when(service.getExamById("owner", "exam"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(get("/exam/exam").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của getExamById gặp lỗi")
    void test_getExamById_004() throws Exception {
        when(service.getExamById("owner", "exam"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(get("/exam/exam").with(user(principal)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi getAllExams nhận principal hợp lệ")
    void test_getAllExams_001() throws Exception {
        when(usecase.getAllUserExams("owner"))
                .thenReturn(
                        List.of(
                                new ExamDTO(
                                        "exam",
                                        "Đề",
                                        List.of(new ExamChapterDTO("c", List.of("l"))),
                                        2)));
        mvc.perform(get("/exam/user-exams").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "[{\"id\":\"exam\",\"name\":\"Đề\",\"chapters\":[{\"id\":\"c\",\"lessonIds\":[\"l\"]}],\"questionsCount\":2}]"));
        verify(usecase).getAllUserExams("owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi nghiệp vụ khi getAllExams chưa xác thực")
    void test_getAllExams_002() throws Exception {
        mvc.perform(get("/exam/user-exams")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi getAllExams không tìm thấy dữ liệu thuộc owner")
    void test_getAllExams_003() throws Exception {
        when(usecase.getAllUserExams("owner"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(get("/exam/user-exams").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của getAllExams gặp lỗi")
    void test_getAllExams_004() throws Exception {
        when(usecase.getAllUserExams("owner"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(get("/exam/user-exams").with(user(principal)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi getRecentExam nhận principal hợp lệ")
    void test_getRecentExam_001() throws Exception {
        when(usecase.getRecentExams("owner"))
                .thenReturn(List.of(new ExamDTO("exam", "Đề", List.of(), 1)));
        mvc.perform(get("/exam/recent").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "[{\"id\":\"exam\",\"name\":\"Đề\",\"chapters\":[],\"questionsCount\":1}]"));
        verify(usecase).getRecentExams("owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi nghiệp vụ khi getRecentExam chưa xác thực")
    void test_getRecentExam_002() throws Exception {
        mvc.perform(get("/exam/recent")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi getRecentExam không tìm thấy dữ liệu thuộc owner")
    void test_getRecentExam_003() throws Exception {
        when(usecase.getRecentExams("owner"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(get("/exam/recent").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của getRecentExam gặp lỗi")
    void test_getRecentExam_004() throws Exception {
        when(usecase.getRecentExams("owner"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(get("/exam/recent").with(user(principal)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 đúng JSON contract khi deleteExam nhận principal hợp lệ")
    void test_deleteExam_001() throws Exception {
        when(usecase.deleteExam("owner", "exam")).thenReturn(true);
        mvc.perform(delete("/exam/exam").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).deleteExam("owner", "exam");
    }

    @Test
    @DisplayName("trả về 401 và không gọi nghiệp vụ khi deleteExam chưa xác thực")
    void test_deleteExam_002() throws Exception {
        mvc.perform(delete("/exam/exam")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase, service);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi deleteExam không tìm thấy dữ liệu thuộc owner")
    void test_deleteExam_003() throws Exception {
        when(usecase.deleteExam("owner", "exam"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(delete("/exam/exam").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 khi dependency của deleteExam gặp lỗi")
    void test_deleteExam_004() throws Exception {
        when(usecase.deleteExam("owner", "exam"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(delete("/exam/exam").with(user(principal)))
                .andExpect(status().isInternalServerError());
    }
}
