package com.wedu.exam_creation.exporter.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnprocessableEntityException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.exporter.usecase.ExporterUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ExporterController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ExporterControllerTest {
    private static final String DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String EMAIL = "teacher@example.com";
    private static final String TOKEN = "test-token";
    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");

    @Autowired private MockMvc mvc;
    @MockitoBean private ExporterUsecase exporter;
    @MockitoBean private CustomOAuth2UserService oauthService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;

    @Test
    @DisplayName(
            "trả về DOCX đính kèm với tên tiếng Việt và cấm lưu cache khi giáo viên xuất ma trận")
    void test_exportMatrix_001() throws Exception {
        authenticate("ROLE_TEACHER");
        byte[] bytes = {80, 75};
        when(exporter.exportMatrix("owner", "exam")).thenReturn(bytes);

        mvc.perform(request("/exporter/matrix"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType(DOCX))
                .andExpect(header().longValue("Content-Length", bytes.length))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename*=UTF-8''%C4%90%E1%BB%81%201_ma%20tr%E1%BA%ADn%20%C4%91%E1%BB%81.docx"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_MODERATOR"})
    @DisplayName(
            "trả về HTTP 403 khi tài khoản đã xác thực không có quyền giáo viên để xuất ma trận")
    void test_exportMatrix_002(String role) throws Exception {
        authenticate(role);

        mvc.perform(request("/exporter/matrix")).andExpect(status().isForbidden());
        verifyNoInteractions(exporter);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi yêu cầu xuất ma trận có ID và tên đề để trống")
    void test_exportMatrix_003() throws Exception {
        authenticate("ROLE_TEACHER");

        mvc.perform(request("/exporter/matrix").content("{\"examId\":\" \",\"examName\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(exporter);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi không tìm thấy đề của giáo viên để xuất ma trận")
    void test_exportMatrix_004() throws Exception {
        authenticate("ROLE_TEACHER");
        String message = "Không tồn tại bài kiểm tra";
        when(exporter.exportMatrix("owner", "exam")).thenThrow(new NotFoundException(message));

        mvc.perform(request("/exporter/matrix"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @DisplayName("trả về HTTP 422 cùng thông báo khi đề thiếu snapshot ma trận")
    void test_exportMatrix_005() throws Exception {
        authenticate("ROLE_TEACHER");
        when(exporter.exportMatrix("owner", "exam"))
                .thenThrow(new UnprocessableEntityException("Thiếu ma trận"));

        mvc.perform(request("/exporter/matrix"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Thiếu ma trận"));
    }

    @Test
    @DisplayName("trả về HTTP 400 và chặn xuất ma trận khi yêu cầu thiếu tiêu đề Authorization")
    void test_exportMatrix_006() throws Exception {
        mvc.perform(
                        post("/exporter/matrix")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"examId\":\"exam\",\"examName\":\"Đề 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(exporter);
    }

    @Test
    @DisplayName("trả về DOCX đính kèm đúng tên khi chủ sở hữu đã xác thực yêu cầu xuất đề")
    void test_exportExam_007() throws Exception {
        authenticate("ROLE_TEACHER");
        byte[] bytes = {80, 75, 3, 4};
        when(exporter.exportExam("owner", "exam")).thenReturn(bytes);

        mvc.perform(request("/exporter/exam"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType(DOCX))
                .andExpect(header().longValue("Content-Length", bytes.length))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename*=UTF-8''%C4%90%E1%BB%81+1.docx"));
        verify(exporter).exportExam("owner", "exam");
    }

    @Test
    @DisplayName("trả về HTTP 404 khi mã đề cần xuất không khớp đề thuộc chủ phiên")
    void test_exportExam_008() throws Exception {
        authenticate("ROLE_TEACHER");
        when(exporter.exportExam("owner", "exam"))
                .thenThrow(new NotFoundException("Không tồn tại bài kiểm tra"));

        mvc.perform(request("/exporter/exam")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về HTTP 500 khi dịch vụ xuất đề không tạo được tài liệu")
    void test_exportExam_009() throws Exception {
        authenticate("ROLE_TEACHER");
        when(exporter.exportExam("owner", "exam"))
                .thenThrow(new InternalServerException("Không thể xuất đề"));

        mvc.perform(request("/exporter/exam")).andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về HTTP 400 và chặn xuất đề khi yêu cầu thiếu tiêu đề Authorization")
    void test_exportExam_010() throws Exception {
        mvc.perform(
                        post("/exporter/exam")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"examId\":\"exam\",\"examName\":\"Đề 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(exporter);
    }

    private MockHttpServletRequestBuilder request(String path) {
        return post(path)
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"examId\":\"exam\",\"examName\":\"Đề 1\"}");
    }

    private void authenticate(String role) {
        CommonUserResponseAllDTO user =
                new CommonUserResponseAllDTO(
                        "owner",
                        EMAIL,
                        "unused-hash",
                        "Giáo viên",
                        role,
                        "LOCAL",
                        "avatars/default.png",
                        true,
                        "FREE",
                        NOW,
                        NOW,
                        null);
        when(tokens.getPayloadFromAccessToken(TOKEN))
                .thenReturn(new ATPayload("session", "owner", EMAIL, role));
        when(users.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(user));
    }
}
