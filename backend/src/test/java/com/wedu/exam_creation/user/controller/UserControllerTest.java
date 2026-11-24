package com.wedu.exam_creation.user.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import com.wedu.exam_creation.user.usecase.UserUsecase;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {
    private static final String ID = "teacher-1";
    private static final String EMAIL = "teacher@example.com";
    private static final String TOKEN = "test-token";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String AVATAR = "avatars/new.png";
    private static final String NAME = "Giáo viên 1";
    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired private MockMvc mvc;
    @MockitoBean private UserUsecase usecase;
    @MockitoBean private JwtTokenProvider tokens;
    @MockitoBean private CustomUserDetailsService users;
    @MockitoBean private CustomOAuth2UserService oauthUsers;
    @MockitoBean private OAuth2AuthenticationSuccessHandler oauthSuccess;

    @Test
    @DisplayName("trả về HTTP 200 và hồ sơ công khai của chủ phiên khi gọi GET với JWT hợp lệ")
    void test_getMe_001() throws Exception {
        CommonUserResponseAllDTO principal = authenticate(true);
        String avatarUrl = "https://example.test/avatar";
        CommonUserResponseDTO profile =
                new CommonUserResponseDTO(
                        ID, EMAIL, NAME, "ROLE_TEACHER", avatarUrl, true, "FREE", TIME, TIME, null);
        when(usecase.getMe(principal)).thenReturn(profile);

        mvc.perform(get("/user/me").header("Authorization", BEARER).param("userId", "other-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.username").value(NAME))
                .andExpect(jsonPath("$.role").value("ROLE_TEACHER"))
                .andExpect(jsonPath("$.avatarUrl").value(avatarUrl))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.accountType").value("FREE"))
                .andExpect(jsonPath("$.hashedPassword").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.loginMethod").doesNotHaveJsonPath());
    }

    @Test
    @DisplayName("trả về HTTP 400 khi GET hồ sơ thiếu tiêu đề Authorization")
    void test_getMe_002() throws Exception {
        mvc.perform(get("/user/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header Authorization sai định dạng"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 401 khi JWT bị từ chối trước khi truy cập hồ sơ")
    void test_getMe_003() throws Exception {
        doThrow(new UnAuthorizedException("Token không hợp lệ"))
                .when(tokens)
                .validateAccessToken(TOKEN);

        mvc.perform(get("/user/me").header("Authorization", BEARER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token không hợp lệ"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 404 khi tài khoản trong JWT không còn tồn tại")
    void test_getMe_004() throws Exception {
        resolveToken();
        when(users.loadUserByUsername(EMAIL))
                .thenThrow(new UsernameNotFoundException("Không tìm thấy tài khoản"));

        mvc.perform(get("/user/me").header("Authorization", BEARER))
                .andExpect(status().isNotFound());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 403 khi tài khoản trong JWT đã bị khóa")
    void test_getMe_005() throws Exception {
        authenticate(false);

        mvc.perform(get("/user/me").header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 405 khi gửi POST đến đường dẫn hồ sơ")
    void test_getMe_006() throws Exception {
        authenticate(true);

        mvc.perform(post("/user/me").header("Authorization", BEARER))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(usecase);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về HTTP 200 với kết quả use case khi PATCH avatar cho người dùng trong JWT")
    void test_updateAvatar_007(boolean updated) throws Exception {
        authenticate(true);
        when(usecase.updateAvatar(ID, AVATAR)).thenReturn(updated);

        mvc.perform(
                        patch("/user/update-avatar")
                                .header("Authorization", BEARER)
                                .param("s3Key", AVATAR)
                                .param("userId", "other-user"))
                .andExpect(status().isOk())
                .andExpect(content().string(Boolean.toString(updated)));
        verify(usecase).updateAvatar(ID, AVATAR);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi PATCH avatar thiếu tham số s3Key")
    void test_updateAvatar_008() throws Exception {
        authenticate(true);

        mvc.perform(patch("/user/update-avatar").header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 400 và thông báo khi khóa avatar bị use case từ chối")
    void test_updateAvatar_009() throws Exception {
        authenticate(true);
        when(usecase.updateAvatar(ID, "")).thenThrow(new BadRequestException("s3Key rỗng"));

        mvc.perform(patch("/user/update-avatar").header("Authorization", BEARER).param("s3Key", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("s3Key rỗng"));
    }

    @Test
    @DisplayName("trả về HTTP 500 với thông báo khi use case báo lỗi lưu avatar")
    void test_updateAvatar_010() throws Exception {
        authenticate(true);
        String message = "Có lỗi trong quá trình cập nhật avatar";
        when(usecase.updateAvatar(ID, AVATAR)).thenThrow(new InternalServerException(message));

        mvc.perform(
                        patch("/user/update-avatar")
                                .header("Authorization", BEARER)
                                .param("s3Key", AVATAR))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(message));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về HTTP 200 với kết quả use case khi PATCH tên cho người dùng trong JWT")
    void test_updateUser_011(boolean updated) throws Exception {
        authenticate(true);
        when(usecase.updateUsername(ID, NAME)).thenReturn(updated);

        mvc.perform(
                        patch("/user/update-username")
                                .header("Authorization", BEARER)
                                .param("username", NAME)
                                .param("userId", "other-user"))
                .andExpect(status().isOk())
                .andExpect(content().string(Boolean.toString(updated)));
        verify(usecase).updateUsername(ID, NAME);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi tên người dùng chỉ được gửi trong JSON")
    void test_updateUser_012() throws Exception {
        authenticate(true);

        mvc.perform(
                        patch("/user/update-username")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"teacher\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 400 khi PATCH tên người dùng thiếu tham số username")
    void test_updateUser_013() throws Exception {
        authenticate(true);

        mvc.perform(patch("/user/update-username").header("Authorization", BEARER))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 400 với thông báo khi tên người dùng bị use case từ chối")
    void test_updateUser_014() throws Exception {
        authenticate(true);
        when(usecase.updateUsername(ID, ""))
                .thenThrow(new BadRequestException("Tên người dùng rỗng"));

        mvc.perform(
                        patch("/user/update-username")
                                .header("Authorization", BEARER)
                                .param("username", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tên người dùng rỗng"));
    }

    @Test
    @DisplayName("trả về HTTP 500 với thông báo khi use case báo lỗi lưu tên người dùng")
    void test_updateUser_015() throws Exception {
        authenticate(true);
        String message = "Có lỗi trong quá trình cập nhật tên người dùng";
        when(usecase.updateUsername(ID, NAME)).thenThrow(new InternalServerException(message));

        mvc.perform(
                        patch("/user/update-username")
                                .header("Authorization", BEARER)
                                .param("username", NAME))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @DisplayName("trả về HTTP 405 khi cập nhật tên người dùng bằng PUT")
    void test_updateUser_016() throws Exception {
        authenticate(true);

        mvc.perform(
                        put("/user/update-username")
                                .header("Authorization", BEARER)
                                .param("username", NAME))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về HTTP 400 và không đổi tên khi request PATCH thiếu tiêu đề Authorization")
    void test_updateUser_017() throws Exception {
        mvc.perform(patch("/user/update-username").param("username", NAME))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName(
            "trả về HTTP 400 và không đổi avatar khi request PATCH thiếu tiêu đề Authorization")
    void test_updateAvatar_018() throws Exception {
        mvc.perform(patch("/user/update-avatar").param("s3Key", AVATAR))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    private void resolveToken() {
        when(tokens.getPayloadFromAccessToken(TOKEN))
                .thenReturn(new ATPayload("test-session", ID, EMAIL, "ROLE_TEACHER"));
    }

    private CommonUserResponseAllDTO authenticate(boolean active) {
        resolveToken();
        CommonUserResponseAllDTO principal =
                new CommonUserResponseAllDTO(
                        ID,
                        EMAIL,
                        "unused-hash",
                        NAME,
                        "ROLE_TEACHER",
                        "LOCAL",
                        AVATAR,
                        active,
                        "FREE",
                        TIME,
                        TIME,
                        null);
        when(users.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(principal));
        return principal;
    }
}
