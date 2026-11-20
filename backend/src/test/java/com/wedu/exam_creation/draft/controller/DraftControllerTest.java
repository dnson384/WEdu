package com.wedu.exam_creation.draft.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.draft.dto.request.*;
import com.wedu.exam_creation.draft.usecase.DraftUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(value = DraftController.class, properties = "app.frontend.url=http://localhost:3000")
@Import(SecurityConfig.class)
class DraftControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private DraftUsecase usecase;
    @MockitoBean private JwtAuthenticationFilter jwt;
    @MockitoBean private CustomOAuth2UserService oidc;
    @MockitoBean private OAuth2AuthenticationSuccessHandler success;
    @MockitoBean private ClientRegistrationRepository clients;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() throws Exception {
        CommonUserResponseAllDTO owner = mock(CommonUserResponseAllDTO.class);
        when(owner.getId()).thenReturn("owner");
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

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(user(principal));
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_createDraft_001() throws Exception {

        when(usecase.createDraft(any(CreateDraftDTO.class), eq("owner"))).thenReturn("created");
        mvc.perform(
                        authenticated(
                                post("/draft/create")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"examName\":\"Đề\",\"questionsCount\":2,\"questionTypes\":[\"Đúng sai\"]}")))
                .andExpect(status().isOk())
                .andExpect(content().string("created"));
        verify(usecase).createDraft(any(CreateDraftDTO.class), eq("owner"));
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_createDraft_002() throws Exception {

        mvc.perform(
                        post("/draft/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"examName\":\"Đề\",\"questionsCount\":2,\"questionTypes\":[\"Đúng sai\"]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 khi usecase báo không tìm thấy dữ liệu")
    void test_createDraft_003() throws Exception {

        when(usecase.createDraft(any(CreateDraftDTO.class), eq("owner")))
                .thenThrow(new NotFoundException("missing"));
        mvc.perform(
                        authenticated(
                                post("/draft/create")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"examName\":\"Đề\",\"questionsCount\":2,\"questionTypes\":[\"Đúng sai\"]}")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_getDraft_001() throws Exception {

        when(usecase.getDraft("draft", "owner"))
                .thenReturn(
                        new DraftDTO(
                                "draft",
                                "owner",
                                "Đề",
                                2,
                                List.of("Đúng sai"),
                                List.of(),
                                null,
                                null));
        mvc.perform(authenticated(get("/draft/draft")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("draft"));
        verify(usecase).getDraft("draft", "owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_getDraft_002() throws Exception {

        mvc.perform(get("/draft/draft")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 khi usecase báo không tìm thấy dữ liệu")
    void test_getDraft_003() throws Exception {

        when(usecase.getDraft("draft", "owner")).thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(get("/draft/draft"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_updateChapters_001() throws Exception {

        when(usecase.updateChapters(eq("draft"), any(UpdateDraftDTO.class), eq("owner")))
                .thenReturn(true);
        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).updateChapters(eq("draft"), any(UpdateDraftDTO.class), eq("owner"));
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_updateChapters_002() throws Exception {

        mvc.perform(
                        put("/draft/draft/chapter")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 khi usecase báo không tìm thấy dữ liệu")
    void test_updateChapters_003() throws Exception {

        when(usecase.updateChapters(eq("draft"), any(UpdateDraftDTO.class), eq("owner")))
                .thenThrow(new NotFoundException("missing"));
        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_updateLessons_001() throws Exception {

        when(usecase.updateLessons(
                        eq("draft"), eq("chapter"), any(UpdateDraftDTO.class), eq("owner")))
                .thenReturn(true);
        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter/lesson")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase)
                .updateLessons(eq("draft"), eq("chapter"), any(UpdateDraftDTO.class), eq("owner"));
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_updateLessons_002() throws Exception {

        mvc.perform(
                        put("/draft/draft/chapter/lesson")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 khi usecase báo không tìm thấy dữ liệu")
    void test_updateLessons_003() throws Exception {

        when(usecase.updateLessons(
                        eq("draft"), eq("chapter"), any(UpdateDraftDTO.class), eq("owner")))
                .thenThrow(new NotFoundException("missing"));
        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter/lesson")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_generateMatrix_001() throws Exception {

        when(usecase.generateMatrix("draft", "owner")).thenReturn(true);
        mvc.perform(authenticated(put("/draft/draft/generate-matrix")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).generateMatrix("draft", "owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_generateMatrix_002() throws Exception {

        mvc.perform(put("/draft/draft/generate-matrix")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 khi usecase báo không tìm thấy dữ liệu")
    void test_generateMatrix_003() throws Exception {

        when(usecase.generateMatrix("draft", "owner")).thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(put("/draft/draft/generate-matrix")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_generateMatrixDetails_001() throws Exception {

        when(usecase.generateMatrixDetails("draft", "owner")).thenReturn(true);
        mvc.perform(authenticated(put("/draft/draft/generate-matrix-details")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).generateMatrixDetails("draft", "owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_generateMatrixDetails_002() throws Exception {

        mvc.perform(put("/draft/draft/generate-matrix-details"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 khi usecase báo không tìm thấy dữ liệu")
    void test_generateMatrixDetails_003() throws Exception {

        when(usecase.generateMatrixDetails("draft", "owner"))
                .thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(put("/draft/draft/generate-matrix-details")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_getRecentDraft_001() throws Exception {

        when(usecase.getRecentDraft("owner"))
                .thenReturn(
                        List.of(
                                new DraftDTO(
                                        "draft", "owner", "Đề", 2, List.of(), List.of(), null,
                                        null)));
        mvc.perform(authenticated(get("/draft/recent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("draft"));
        verify(usecase).getRecentDraft("owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_getRecentDraft_002() throws Exception {

        mvc.perform(get("/draft/recent")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 500 khi truy vấn danh sách thất bại")
    void test_getRecentDraft_003() throws Exception {

        when(usecase.getRecentDraft("owner")).thenThrow(new InternalServerException("missing"));
        mvc.perform(authenticated(get("/draft/recent")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_getAllUserDrafts_001() throws Exception {

        when(usecase.getAllUserDrafts("owner"))
                .thenReturn(
                        List.of(
                                new DraftDTO(
                                        "draft", "owner", "Đề", 2, List.of(), List.of(), null,
                                        null)));
        mvc.perform(authenticated(get("/draft/user-drafts")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("draft"));
        verify(usecase).getAllUserDrafts("owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_getAllUserDrafts_002() throws Exception {

        mvc.perform(get("/draft/user-drafts")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 500 khi truy vấn danh sách thất bại")
    void test_getAllUserDrafts_003() throws Exception {

        when(usecase.getAllUserDrafts("owner")).thenThrow(new InternalServerException("missing"));
        mvc.perform(authenticated(get("/draft/user-drafts")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về 200 cùng body đúng contract khi yêu cầu hợp lệ và đã xác thực")
    void test_deleteDraft_001() throws Exception {

        when(usecase.deleteDraft("draft", "owner")).thenReturn(true);
        mvc.perform(authenticated(delete("/draft/draft")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).deleteDraft("draft", "owner");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi chưa xác thực")
    void test_deleteDraft_002() throws Exception {

        mvc.perform(delete("/draft/draft")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 khi usecase báo không tìm thấy dữ liệu")
    void test_deleteDraft_003() throws Exception {

        when(usecase.deleteDraft("draft", "owner")).thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(delete("/draft/draft"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về 400 và không gọi usecase khi JSON không đọc được")
    void test_createDraft_004() throws Exception {

        mvc.perform(
                        authenticated(
                                post("/draft/create")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về payload đầy đủ từ request khi tạo draft")
    void test_createDraft_005() throws Exception {

        mvc.perform(
                        authenticated(
                                post("/draft/create")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"examName\":\"Đề\",\"questionsCount\":2,\"questionTypes\":[\"Đúng sai\"]}")))
                .andExpect(status().isOk());
        ArgumentCaptor<CreateDraftDTO> payload = ArgumentCaptor.forClass(CreateDraftDTO.class);
        verify(usecase).createDraft(payload.capture(), eq("owner"));
        assertThat(payload.getValue()).isEqualTo(new CreateDraftDTO(2, "Đề", List.of("Đúng sai")));
    }

    @Test
    @DisplayName("trả về payload thêm xóa đầy đủ khi nhận request cập nhật")
    void test_updateChapters_004() throws Exception {

        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}")))
                .andExpect(status().isOk());
        ArgumentCaptor<UpdateDraftDTO> payload = ArgumentCaptor.forClass(UpdateDraftDTO.class);
        verify(usecase).updateChapters(eq("draft"), payload.capture(), eq("owner"));
        assertThat(payload.getValue())
                .isEqualTo(
                        new UpdateDraftDTO(
                                List.of(new UpdateParamDTO("added", "Tên")), List.of("removed")));
    }

    @Test
    @DisplayName("trả về 400 và không gọi usecase khi thiếu request body")
    void test_updateChapters_005() throws Exception {

        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter")
                                        .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về payload thêm xóa đầy đủ khi nhận request cập nhật")
    void test_updateLessons_004() throws Exception {

        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter/lesson")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}")))
                .andExpect(status().isOk());
        ArgumentCaptor<UpdateDraftDTO> payload = ArgumentCaptor.forClass(UpdateDraftDTO.class);
        verify(usecase).updateLessons(eq("draft"), eq("chapter"), payload.capture(), eq("owner"));
        assertThat(payload.getValue())
                .isEqualTo(
                        new UpdateDraftDTO(
                                List.of(new UpdateParamDTO("added", "Tên")), List.of("removed")));
    }

    @Test
    @DisplayName("trả về 400 và không gọi usecase khi thiếu request body")
    void test_updateLessons_005() throws Exception {

        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter/lesson")
                                        .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }
}
