package com.wedu.exam_creation.exporter.controller;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExporterController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ExporterMatrixControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ExporterUsecase exporter;
    @MockitoBean private CustomOAuth2UserService oauthService;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthHandler;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;

    private void role(String role) {
        var user = new CommonUserResponseAllDTO();
        user.setId("owner");
        user.setRole(role);
        user.setIsActive(true);
        when(users.loadUserByUsername("teacher@example.com")).thenReturn(new CustomUserDetails(user));
    }

    @BeforeEach
    void authenticate() {
        when(tokens.getPayloadFromAccessToken("test-token"))
                .thenReturn(new ATPayload("jti", "owner", "teacher@example.com", "ROLE_TEACHER"));
        role("ROLE_TEACHER");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request() {
        return post("/exporter/matrix").header("Authorization", "Bearer test-token")
                .contentType("application/json").content("{\"examId\":\"exam\",\"examName\":\"Đề 1\"}");
    }

    @Test
    void teacherDownloadsDocumentWithVietnameseFilename() throws Exception {
        when(exporter.exportMatrix("owner", "exam")).thenReturn(new byte[]{80, 75});
        mvc.perform(request()).andExpect(status().isOk()).andExpect(content().bytes(new byte[]{80, 75}))
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''%C4%90%E1%BB%81%201_ma%20tr%E1%BA%ADn%20%C4%91%E1%BB%81.docx"));
        verify(exporter).exportMatrix("owner", "exam");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_MODERATOR"})
    void otherRolesCannotExport(String role) throws Exception {
        role(role);
        mvc.perform(request()).andExpect(status().isForbidden());
        verifyNoInteractions(exporter);
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        mvc.perform(request().content("{\"examId\":\" \",\"examName\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(exporter);
    }

    @Test
    void preservesNotFound() throws Exception {
        when(exporter.exportMatrix("owner", "exam")).thenThrow(new NotFoundException("Không tồn tại bài kiểm tra"));
        mvc.perform(request()).andExpect(status().isNotFound());
    }

    @Test
    void preservesMissingSnapshotError() throws Exception {
        when(exporter.exportMatrix("owner", "exam")).thenThrow(new UnprocessableEntityException("Thiếu ma trận"));
        mvc.perform(request()).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("Thiếu ma trận"));
    }
}
