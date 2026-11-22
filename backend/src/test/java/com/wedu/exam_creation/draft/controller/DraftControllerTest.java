package com.wedu.exam_creation.draft.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.draft.dto.request.*;
import com.wedu.exam_creation.draft.usecase.DraftUsecase;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import java.time.Instant;
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
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DraftControllerTest {
    private static final String TOKEN = "test-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String EMAIL = "teacher@example.com";
    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");

    @Autowired private MockMvc mvc;
    @MockitoBean private DraftUsecase usecase;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private CustomOAuth2UserService oidc;
    @MockitoBean private OAuth2AuthenticationSuccessHandler success;
    @MockitoBean private ClientRegistrationRepository clients;

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        authenticate(true);
        return request.header("Authorization", BEARER);
    }

    @Test
    @DisplayName(
            "trả về HTTP 200 với ID của bản nháp mới khi người dùng đã xác thực gửi yêu cầu tạo nháp hợp lệ")
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
    @DisplayName(
            "trả về HTTP 400 và chặn tạo nháp khi yêu cầu tạo nháp thiếu tiêu đề Authorization")
    void test_createDraft_002() throws Exception {

        mvc.perform(
                        post("/draft/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"examName\":\"Đề\",\"questionsCount\":2,\"questionTypes\":[\"Đúng sai\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi use case không tìm thấy dữ liệu cần để tạo nháp")
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
    @DisplayName("trả về HTTP 200 với bản nháp của chủ phiên khi chủ phiên đọc bản nháp theo ID")
    void test_getDraft_004() throws Exception {

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
    @DisplayName(
            "trả về HTTP 400 và chặn đọc nháp khi yêu cầu đọc nháp thiếu tiêu đề Authorization")
    void test_getDraft_005() throws Exception {

        mvc.perform(get("/draft/draft"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi ID nháp không thuộc chủ phiên hoặc không tồn tại")
    void test_getDraft_006() throws Exception {

        when(usecase.getDraft("draft", "owner")).thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(get("/draft/draft"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về HTTP 200 với true khi use case cập nhật chương thành công")
    void test_updateChapters_007() throws Exception {

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
    @DisplayName(
            "trả về HTTP 400 và chặn cập nhật chương khi yêu cầu cập nhật chương thiếu tiêu đề Authorization")
    void test_updateChapters_008() throws Exception {

        mvc.perform(
                        put("/draft/draft/chapter")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi không tìm thấy bản nháp để cập nhật chương")
    void test_updateChapters_009() throws Exception {

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
    @DisplayName("trả về HTTP 200 với true khi use case cập nhật bài học thành công")
    void test_updateLessons_010() throws Exception {

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
    @DisplayName(
            "trả về HTTP 400 và chặn cập nhật bài học khi yêu cầu cập nhật bài học thiếu tiêu đề Authorization")
    void test_updateLessons_011() throws Exception {

        mvc.perform(
                        put("/draft/draft/chapter/lesson")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"add\":[{\"id\":\"added\",\"name\":\"Tên\"}],\"del\":[\"removed\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi không tìm thấy chương hoặc bản nháp để cập nhật bài")
    void test_updateLessons_012() throws Exception {

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
    @DisplayName("trả về HTTP 200 với true khi use case sinh ma trận thành công")
    void test_generateMatrix_013() throws Exception {

        when(usecase.generateMatrix("draft", "owner")).thenReturn(true);
        mvc.perform(authenticated(put("/draft/draft/generate-matrix")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).generateMatrix("draft", "owner");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và chặn sinh ma trận khi yêu cầu sinh ma trận thiếu tiêu đề Authorization")
    void test_generateMatrix_014() throws Exception {

        mvc.perform(put("/draft/draft/generate-matrix"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi không tìm thấy bản nháp để sinh ma trận")
    void test_generateMatrix_015() throws Exception {

        when(usecase.generateMatrix("draft", "owner")).thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(put("/draft/draft/generate-matrix")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về HTTP 200 với true khi use case sinh bản đặc tả thành công")
    void test_generateMatrixDetails_016() throws Exception {

        when(usecase.generateMatrixDetails("draft", "owner")).thenReturn(true);
        mvc.perform(authenticated(put("/draft/draft/generate-matrix-details")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).generateMatrixDetails("draft", "owner");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và chặn sinh bản đặc tả khi yêu cầu sinh bản đặc tả thiếu tiêu đề Authorization")
    void test_generateMatrixDetails_017() throws Exception {

        mvc.perform(put("/draft/draft/generate-matrix-details"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi không tìm thấy dữ liệu để sinh bản đặc tả")
    void test_generateMatrixDetails_018() throws Exception {

        when(usecase.generateMatrixDetails("draft", "owner"))
                .thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(put("/draft/draft/generate-matrix-details")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về HTTP 200 với danh sách bản nháp gần đây khi chủ phiên có bản nháp gần đây")
    void test_getRecentDraft_019() throws Exception {

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
    @DisplayName(
            "trả về HTTP 400 và chặn đọc nháp gần đây khi yêu cầu đọc nháp gần đây thiếu tiêu đề Authorization")
    void test_getRecentDraft_020() throws Exception {

        mvc.perform(get("/draft/recent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 500 khi truy vấn bản nháp gần đây thất bại")
    void test_getRecentDraft_021() throws Exception {

        when(usecase.getRecentDraft("owner")).thenThrow(new InternalServerException("missing"));
        mvc.perform(authenticated(get("/draft/recent")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName(
            "trả về HTTP 200 với danh sách bản nháp của chủ phiên khi chủ phiên yêu cầu liệt kê các bản nháp của mình")
    void test_getAllUserDrafts_022() throws Exception {

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
    @DisplayName(
            "trả về HTTP 400 và chặn liệt kê bản nháp khi yêu cầu liệt kê bản nháp thiếu tiêu đề Authorization")
    void test_getAllUserDrafts_023() throws Exception {

        mvc.perform(get("/draft/user-drafts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 500 khi truy vấn danh sách bản nháp của chủ phiên thất bại")
    void test_getAllUserDrafts_024() throws Exception {

        when(usecase.getAllUserDrafts("owner")).thenThrow(new InternalServerException("missing"));
        mvc.perform(authenticated(get("/draft/user-drafts")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("trả về HTTP 200 với true khi use case xóa thành công bản nháp của chủ phiên")
    void test_deleteDraft_025() throws Exception {

        when(usecase.deleteDraft("draft", "owner")).thenReturn(true);
        mvc.perform(authenticated(delete("/draft/draft")))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).deleteDraft("draft", "owner");
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và chặn xóa nháp khi yêu cầu xóa nháp thiếu tiêu đề Authorization")
    void test_deleteDraft_026() throws Exception {

        mvc.perform(delete("/draft/draft"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi không tìm thấy bản nháp thuộc chủ phiên để xóa")
    void test_deleteDraft_027() throws Exception {

        when(usecase.deleteDraft("draft", "owner")).thenThrow(new NotFoundException("missing"));
        mvc.perform(authenticated(delete("/draft/draft"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("trả về HTTP 400 và không tạo nháp khi JSON yêu cầu không đọc được")
    void test_createDraft_028() throws Exception {

        mvc.perform(
                        authenticated(
                                post("/draft/create")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về ID đã tạo và chuyển đủ tên đề, số câu, loại câu khi JSON tạo nháp hợp lệ")
    void test_createDraft_029() throws Exception {

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
    @DisplayName(
            "trả về true và chuyển đủ danh sách thêm xóa chương khi JSON cập nhật chương hợp lệ")
    void test_updateChapters_030() throws Exception {

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
    @DisplayName("trả về HTTP 400 và không cập nhật chương khi thiếu body yêu cầu")
    void test_updateChapters_031() throws Exception {

        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter")
                                        .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về true và chuyển đủ danh sách thêm xóa bài khi JSON cập nhật bài học hợp lệ")
    void test_updateLessons_032() throws Exception {

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
    @DisplayName("trả về HTTP 400 và không cập nhật bài học khi thiếu body yêu cầu")
    void test_updateLessons_033() throws Exception {

        mvc.perform(
                        authenticated(
                                put("/draft/draft/chapter/lesson")
                                        .contentType(MediaType.APPLICATION_JSON)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 401 và không sinh ma trận khi JWT bị bộ xác thực từ chối")
    void test_generateMatrix_034() throws Exception {
        String message = "Token không hợp lệ";
        doThrow(new UnAuthorizedException(message)).when(tokens).validateAccessToken(TOKEN);

        mvc.perform(put("/draft/draft/generate-matrix").header("Authorization", BEARER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(message));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 403 và không xóa nháp khi tài khoản đã bị khóa")
    void test_deleteDraft_035() throws Exception {
        authenticate(false);

        mvc.perform(delete("/draft/draft").header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa"));
        verifyNoInteractions(usecase);
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
                        "FREE",
                        NOW,
                        NOW,
                        null);
        when(tokens.getPayloadFromAccessToken(TOKEN))
                .thenReturn(new ATPayload("session-1", "owner", EMAIL, "ROLE_TEACHER"));
        when(users.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(account));
    }
}
