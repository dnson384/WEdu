package com.wedu.exam_creation.admin.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.admin.usecase.AdminUsecase;
import com.wedu.exam_creation.common.dto.user.response.*;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.config.SecurityConfig;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.principal.CustomUserDetails;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AdminController.class, properties = "app.frontend.url=http://localhost:3000")
@Import(SecurityConfig.class)
class AdminControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AdminUsecase usecase;
    @MockitoBean private JwtAuthenticationFilter jwt;
    @MockitoBean private CustomOAuth2UserService oidc;
    @MockitoBean private OAuth2AuthenticationSuccessHandler success;
    @MockitoBean private ClientRegistrationRepository clients;
    private final CommonUserResponseAllDTO admin = admin();
    private final CustomUserDetails principal = new CustomUserDetails(admin);

    private CommonUserResponseAllDTO admin() {
        CommonUserResponseAllDTO result = new CommonUserResponseAllDTO();
        result.setId("admin");
        result.setEmail("admin@example.com");
        result.setRole("ROLE_ADMIN");
        result.setIsActive(true);
        result.setAccountType("FREE");
        return result;
    }

    private CommonUserResponseDTO response() {
        return new CommonUserResponseDTO(
                "target",
                "teacher@example.com",
                "Giáo viên",
                "ROLE_TEACHER",
                "avatars/teacher.png",
                true,
                "FREE",
                Instant.parse("2026-11-19T00:00:00Z"),
                Instant.parse("2026-11-20T00:00:00Z"),
                null);
    }

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
    @DisplayName("trả về 200 đúng body và truyền principal khi ADMIN gọi getAllUsers hợp lệ")
    void test_getAllUsers_001() throws Exception {
        when(usecase.getAllUsers(admin)).thenReturn(List.of(response()));
        mvc.perform(get("/admin/users").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "[{\"id\":\"target\",\"email\":\"teacher@example.com\",\"username\":\"Giáo viên\",\"role\":\"ROLE_TEACHER\",\"avatarUrl\":\"avatars/teacher.png\",\"isActive\":true,\"accountType\":\"FREE\",\"createdAt\":\"2026-11-19T00:00:00Z\",\"updatedAt\":\"2026-11-20T00:00:00Z\",\"plusExpiresAt\":null}]"))
                .andExpect(jsonPath("$[0].hashedPassword").doesNotExist())
                .andExpect(jsonPath("$[0].loginMethod").doesNotExist());
        verify(usecase).getAllUsers(admin);
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi getAllUsers chưa xác thực")
    void test_getAllUsers_002() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR", "USER"})
    @DisplayName(
            "trả về 403 và không gọi usecase khi getAllUsers được gọi bởi role không phải ADMIN")
    void test_getAllUsers_003(String role) throws Exception {
        mvc.perform(get("/admin/users").with(user("other").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi getAllUsers không tìm thấy dữ liệu")
    void test_getAllUsers_004() throws Exception {
        when(usecase.getAllUsers(admin)).thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(get("/admin/users").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 cùng thông báo khi dependency của getAllUsers thất bại")
    void test_getAllUsers_005() throws Exception {
        when(usecase.getAllUsers(admin)).thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(get("/admin/users").with(user(principal)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không xử lý được"));
    }

    @Test
    @DisplayName("trả về 200 đúng body và truyền principal khi ADMIN gọi findUsers hợp lệ")
    void test_findUsers_006() throws Exception {
        when(usecase.findUsers(admin, " Nguyễn + & ")).thenReturn(List.of(response()));
        mvc.perform(
                        get("/admin/users/search")
                                .param("keyword", " Nguyễn + & ")
                                .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "[{\"id\":\"target\",\"email\":\"teacher@example.com\",\"username\":\"Giáo viên\",\"role\":\"ROLE_TEACHER\",\"avatarUrl\":\"avatars/teacher.png\",\"isActive\":true,\"accountType\":\"FREE\",\"createdAt\":\"2026-11-19T00:00:00Z\",\"updatedAt\":\"2026-11-20T00:00:00Z\",\"plusExpiresAt\":null}]"))
                .andExpect(jsonPath("$[0].hashedPassword").doesNotExist())
                .andExpect(jsonPath("$[0].loginMethod").doesNotExist());
        verify(usecase).findUsers(admin, " Nguyễn + & ");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi findUsers chưa xác thực")
    void test_findUsers_007() throws Exception {
        mvc.perform(get("/admin/users/search").param("keyword", " Nguyễn + & "))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR", "USER"})
    @DisplayName("trả về 403 và không gọi usecase khi findUsers được gọi bởi role không phải ADMIN")
    void test_findUsers_008(String role) throws Exception {
        mvc.perform(
                        get("/admin/users/search")
                                .param("keyword", " Nguyễn + & ")
                                .with(user("other").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi findUsers không tìm thấy dữ liệu")
    void test_findUsers_009() throws Exception {
        when(usecase.findUsers(admin, " Nguyễn + & "))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(
                        get("/admin/users/search")
                                .param("keyword", " Nguyễn + & ")
                                .with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 cùng thông báo khi dependency của findUsers thất bại")
    void test_findUsers_010() throws Exception {
        when(usecase.findUsers(admin, " Nguyễn + & "))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(
                        get("/admin/users/search")
                                .param("keyword", " Nguyễn + & ")
                                .with(user(principal)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không xử lý được"));
    }

    @Test
    @DisplayName("trả về 200 đúng body và truyền principal khi ADMIN gọi updateUserRole hợp lệ")
    void test_updateUserRole_011() throws Exception {
        when(usecase.updateUserRole(admin, "target", "ROLE_TEACHER")).thenReturn(response());
        mvc.perform(put("/admin/target/role").param("role", "ROLE_TEACHER").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"id\":\"target\",\"email\":\"teacher@example.com\",\"username\":\"Giáo viên\",\"role\":\"ROLE_TEACHER\",\"avatarUrl\":\"avatars/teacher.png\",\"isActive\":true,\"accountType\":\"FREE\",\"createdAt\":\"2026-11-19T00:00:00Z\",\"updatedAt\":\"2026-11-20T00:00:00Z\",\"plusExpiresAt\":null}"))
                .andExpect(jsonPath("$.hashedPassword").doesNotExist())
                .andExpect(jsonPath("$.loginMethod").doesNotExist());
        verify(usecase).updateUserRole(admin, "target", "ROLE_TEACHER");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi updateUserRole chưa xác thực")
    void test_updateUserRole_012() throws Exception {
        mvc.perform(put("/admin/target/role").param("role", "ROLE_TEACHER"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR", "USER"})
    @DisplayName(
            "trả về 403 và không gọi usecase khi updateUserRole được gọi bởi role không phải ADMIN")
    void test_updateUserRole_013(String role) throws Exception {
        mvc.perform(
                        put("/admin/target/role")
                                .param("role", "ROLE_TEACHER")
                                .with(user("other").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi updateUserRole không tìm thấy dữ liệu")
    void test_updateUserRole_014() throws Exception {
        when(usecase.updateUserRole(admin, "target", "ROLE_TEACHER"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(put("/admin/target/role").param("role", "ROLE_TEACHER").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 cùng thông báo khi dependency của updateUserRole thất bại")
    void test_updateUserRole_015() throws Exception {
        when(usecase.updateUserRole(admin, "target", "ROLE_TEACHER"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(put("/admin/target/role").param("role", "ROLE_TEACHER").with(user(principal)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không xử lý được"));
    }

    @Test
    @DisplayName("trả về 200 đúng body và truyền principal khi ADMIN gọi lockUser hợp lệ")
    void test_lockUser_016() throws Exception {
        CommonUserResponseDTO locked = response();
        locked.setIsActive(false);
        when(usecase.lockUser(admin, "target")).thenReturn(locked);
        mvc.perform(put("/admin/target/lock").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"id\":\"target\",\"email\":\"teacher@example.com\",\"username\":\"Giáo viên\",\"role\":\"ROLE_TEACHER\",\"avatarUrl\":\"avatars/teacher.png\",\"isActive\":false,\"accountType\":\"FREE\",\"createdAt\":\"2026-11-19T00:00:00Z\",\"updatedAt\":\"2026-11-20T00:00:00Z\",\"plusExpiresAt\":null}"))
                .andExpect(jsonPath("$.hashedPassword").doesNotExist())
                .andExpect(jsonPath("$.loginMethod").doesNotExist());
        verify(usecase).lockUser(admin, "target");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi lockUser chưa xác thực")
    void test_lockUser_017() throws Exception {
        mvc.perform(put("/admin/target/lock")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR", "USER"})
    @DisplayName("trả về 403 và không gọi usecase khi lockUser được gọi bởi role không phải ADMIN")
    void test_lockUser_018(String role) throws Exception {
        mvc.perform(put("/admin/target/lock").with(user("other").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi lockUser không tìm thấy dữ liệu")
    void test_lockUser_019() throws Exception {
        when(usecase.lockUser(admin, "target"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(put("/admin/target/lock").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 cùng thông báo khi dependency của lockUser thất bại")
    void test_lockUser_020() throws Exception {
        when(usecase.lockUser(admin, "target"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(put("/admin/target/lock").with(user(principal)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không xử lý được"));
    }

    @Test
    @DisplayName("trả về 200 đúng body và truyền principal khi ADMIN gọi unLockUser hợp lệ")
    void test_unLockUser_021() throws Exception {
        when(usecase.unlockUser(admin, "target")).thenReturn(response());
        mvc.perform(put("/admin/target/unlock").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .json(
                                        "{\"id\":\"target\",\"email\":\"teacher@example.com\",\"username\":\"Giáo viên\",\"role\":\"ROLE_TEACHER\",\"avatarUrl\":\"avatars/teacher.png\",\"isActive\":true,\"accountType\":\"FREE\",\"createdAt\":\"2026-11-19T00:00:00Z\",\"updatedAt\":\"2026-11-20T00:00:00Z\",\"plusExpiresAt\":null}"))
                .andExpect(jsonPath("$.hashedPassword").doesNotExist())
                .andExpect(jsonPath("$.loginMethod").doesNotExist());
        verify(usecase).unlockUser(admin, "target");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi unLockUser chưa xác thực")
    void test_unLockUser_022() throws Exception {
        mvc.perform(put("/admin/target/unlock")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR", "USER"})
    @DisplayName(
            "trả về 403 và không gọi usecase khi unLockUser được gọi bởi role không phải ADMIN")
    void test_unLockUser_023(String role) throws Exception {
        mvc.perform(put("/admin/target/unlock").with(user("other").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi unLockUser không tìm thấy dữ liệu")
    void test_unLockUser_024() throws Exception {
        when(usecase.unlockUser(admin, "target"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(put("/admin/target/unlock").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 cùng thông báo khi dependency của unLockUser thất bại")
    void test_unLockUser_025() throws Exception {
        when(usecase.unlockUser(admin, "target"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(put("/admin/target/unlock").with(user(principal)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không xử lý được"));
    }

    @Test
    @DisplayName("trả về 200 đúng body và truyền principal khi ADMIN gọi deleteUser hợp lệ")
    void test_deleteUser_026() throws Exception {
        when(usecase.deleteUser(admin, "target")).thenReturn(true);
        mvc.perform(delete("/admin/target").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(usecase).deleteUser(admin, "target");
    }

    @Test
    @DisplayName("trả về 401 và không gọi usecase khi deleteUser chưa xác thực")
    void test_deleteUser_027() throws Exception {
        mvc.perform(delete("/admin/target")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usecase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "MODERATOR", "USER"})
    @DisplayName(
            "trả về 403 và không gọi usecase khi deleteUser được gọi bởi role không phải ADMIN")
    void test_deleteUser_028(String role) throws Exception {
        mvc.perform(delete("/admin/target").with(user("other").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 404 cùng thông báo khi deleteUser không tìm thấy dữ liệu")
    void test_deleteUser_029() throws Exception {
        when(usecase.deleteUser(admin, "target"))
                .thenThrow(new NotFoundException("Không tồn tại dữ liệu"));
        mvc.perform(delete("/admin/target").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tồn tại dữ liệu"));
    }

    @Test
    @DisplayName("trả về 500 cùng thông báo khi dependency của deleteUser thất bại")
    void test_deleteUser_030() throws Exception {
        when(usecase.deleteUser(admin, "target"))
                .thenThrow(new InternalServerException("Không xử lý được"));
        mvc.perform(delete("/admin/target").with(user(principal)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Không xử lý được"));
    }

    @Test
    @DisplayName("trả về 200 và danh sách rỗng khi usecase trả danh sách người dùng rỗng")
    void test_getAllUsers_031() throws Exception {
        when(usecase.getAllUsers(admin)).thenReturn(List.of());
        mvc.perform(get("/admin/users").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("trả về 400 và không gọi usecase khi tìm người dùng thiếu keyword")
    void test_findUsers_032() throws Exception {
        mvc.perform(get("/admin/users/search").with(user(principal)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 400 cùng thông báo khi usecase từ chối keyword rỗng")
    void test_findUsers_033() throws Exception {
        when(usecase.findUsers(admin, " "))
                .thenThrow(new BadRequestException("Từ khóa không được rỗng"));
        mvc.perform(get("/admin/users/search").param("keyword", " ").with(user(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Từ khóa không được rỗng"));
    }

    @Test
    @DisplayName("trả về 200 và danh sách rỗng khi usecase tìm kiếm trả rỗng")
    void test_findUsers_034() throws Exception {
        when(usecase.findUsers(admin, "missing")).thenReturn(List.of());
        mvc.perform(get("/admin/users/search").param("keyword", "missing").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("trả về 400 và không gọi usecase khi đổi role thiếu query parameter role")
    void test_updateUserRole_035() throws Exception {
        mvc.perform(put("/admin/target/role").with(user(principal)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 400 cùng thông báo khi usecase từ chối role không hợp lệ")
    void test_updateUserRole_036() throws Exception {
        when(usecase.updateUserRole(admin, "target", "ROLE_USER"))
                .thenThrow(new BadRequestException("Quyền không hợp lệ"));
        mvc.perform(put("/admin/target/role").param("role", "ROLE_USER").with(user(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Quyền không hợp lệ"));
    }

    @Test
    @DisplayName("trả về 403 cùng thông báo khi đổi quyền ADMIN khác bị usecase từ chối")
    void test_updateUserRole_037() throws Exception {
        when(usecase.updateUserRole(admin, "target", "ROLE_TEACHER"))
                .thenThrow(new ForbiddenException("Không được phép thay đổi quyền của admin khác"));
        mvc.perform(put("/admin/target/role").param("role", "ROLE_TEACHER").with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.message")
                                .value("Không được phép thay đổi quyền của admin khác"));
    }

    @Test
    @DisplayName("trả về 400 cùng thông báo khi usecase chặn khóa tài khoản bản thân")
    void test_lockUser_038() throws Exception {
        when(usecase.lockUser(admin, "admin"))
                .thenThrow(new BadRequestException("Không được phép khóa tài khoản bản thân"));
        mvc.perform(put("/admin/admin/lock").with(user(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không được phép khóa tài khoản bản thân"));
    }

    @Test
    @DisplayName("trả về 403 cùng thông báo khi usecase chặn khóa tài khoản ADMIN khác")
    void test_lockUser_039() throws Exception {
        when(usecase.lockUser(admin, "target"))
                .thenThrow(new ForbiddenException("Không được phép khóa của admin khác"));
        mvc.perform(put("/admin/target/lock").with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Không được phép khóa của admin khác"));
    }

    @Test
    @DisplayName("trả về 400 cùng thông báo khi usecase chặn mở khóa tài khoản bản thân")
    void test_unLockUser_040() throws Exception {
        when(usecase.unlockUser(admin, "admin"))
                .thenThrow(new BadRequestException("Không được phép mở khóa tài khoản bản thân"));
        mvc.perform(put("/admin/admin/unlock").with(user(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message").value("Không được phép mở khóa tài khoản bản thân"));
    }

    @Test
    @DisplayName("trả về 403 cùng thông báo khi usecase chặn mở khóa tài khoản ADMIN khác")
    void test_unLockUser_041() throws Exception {
        when(usecase.unlockUser(admin, "target"))
                .thenThrow(new ForbiddenException("Không được phép mở khóa của admin khác"));
        mvc.perform(put("/admin/target/unlock").with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Không được phép mở khóa của admin khác"));
    }

    @Test
    @DisplayName("trả về 400 cùng thông báo khi usecase chặn xóa tài khoản bản thân")
    void test_deleteUser_042() throws Exception {
        when(usecase.deleteUser(admin, "admin"))
                .thenThrow(new BadRequestException("Không được phép xóa tài khoản bản thân"));
        mvc.perform(delete("/admin/admin").with(user(principal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không được phép xóa tài khoản bản thân"));
    }

    @Test
    @DisplayName("trả về 403 cùng thông báo khi usecase chặn xóa tài khoản ADMIN khác")
    void test_deleteUser_043() throws Exception {
        when(usecase.deleteUser(admin, "target"))
                .thenThrow(new ForbiddenException("Không được phép xóa tài khoản của admin khác"));
        mvc.perform(delete("/admin/target").with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.message")
                                .value("Không được phép xóa tài khoản của admin khác"));
    }

    @Test
    @DisplayName("trả về 200 với body false khi dịch vụ không xóa được tài khoản")
    void test_deleteUser_044() throws Exception {
        when(usecase.deleteUser(admin, "target")).thenReturn(false);
        mvc.perform(delete("/admin/target").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
