package com.wedu.exam_creation.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.auth.dto.response.*;
import com.wedu.exam_creation.auth.dto.response.VerificationResponseDTO;
import com.wedu.exam_creation.auth.usecase.*;
import com.wedu.exam_creation.auth.usecase.VerificationUsecase;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = VerificationController.class,
        properties = "app.frontend.url=https://wedu.example")
@AutoConfigureMockMvc(addFilters = false)
class VerificationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean VerificationUsecase usecase;
    @MockitoBean JwtAuthenticationFilter jwt;

    @Test
    @DisplayName("trả về 200 và cookie HttpOnly không lộ JWT trong JSON khi OTP hợp lệ")
    void test_verify_001() throws Exception {
        when(usecase.verify("token", "012345"))
                .thenReturn(
                        new AuthorizedResponseDTO(
                                new UserResponseDTO(
                                        "user",
                                        "teacher@example.com",
                                        "Teacher",
                                        "ROLE_TEACHER",
                                        "default"),
                                "access",
                                "refresh"));
        mvc.perform(
                        post("/auth/verification/verify")
                                .cookie(new Cookie("verificationToken", "token"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"otp\":\"012345\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("accessToken", "access"))
                .andExpect(cookie().secure("accessToken", true))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().maxAge("accessToken", 300))
                .andExpect(cookie().maxAge("verificationToken", 0))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.id").value("user"));
    }

    @Test
    @DisplayName("trả về 400 trước usecase khi OTP sai định dạng")
    void test_verify_002() throws Exception {
        mvc.perform(
                        post("/auth/verification/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"otp\":\"12345x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", "private, no-store"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName("trả về 429 kèm Retry-After và mã lỗi khi chưa hết cooldown")
    void test_resend_003() throws Exception {
        when(usecase.resend(any()))
                .thenThrow(new VerificationException("OTP_COOLDOWN", "wait", 429, 80));
        mvc.perform(post("/auth/verification/resend"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "80"))
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
    }

    @Test
    @DisplayName("trả về 401 không cấp cookie JWT khi phiên hết hạn")
    void test_metadata_004() throws Exception {
        when(usecase.metadata((String) null))
                .thenThrow(new VerificationException("VERIFICATION_EXPIRED", "expired", 401));
        mvc.perform(get("/auth/verification"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("accessToken"));
    }

    @Test
    @DisplayName("trả về 202 không lộ verification token trong JSON khi Google cần OTP")
    void test_exchange_005() throws Exception {
        java.time.Instant now = java.time.Instant.parse("2026-11-13T00:00:00Z");
        when(usecase.exchange(anyString(), anyString()))
                .thenReturn(
                        new VerificationUsecase.Exchange(
                                new VerificationUsecase.Pending(
                                        "c".repeat(64),
                                        new VerificationResponseDTO(
                                                "t***@example.com",
                                                now.plusSeconds(1800),
                                                now.plusSeconds(300),
                                                now.plusSeconds(120),
                                                5,
                                                now)),
                                null));
        mvc.perform(
                        post("/auth/google/exchange")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"code\":\""
                                                + "a".repeat(64)
                                                + "\",\"verifier\":\""
                                                + "b".repeat(64)
                                                + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(cookie().exists("verificationToken"))
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VerificationUsecase.Pending mockAuthorizedResponse;

    @BeforeEach
    void setUp() {
        java.time.Instant now = java.time.Instant.parse("2026-11-13T00:00:00Z");
        mockAuthorizedResponse =
                new VerificationUsecase.Pending(
                        "a".repeat(64),
                        new VerificationResponseDTO(
                                "a***@gmail.com",
                                now.plusSeconds(1800),
                                now.plusSeconds(300),
                                now.plusSeconds(120),
                                5,
                                now));
    }

    private NewUserRequestDTO validRequest() {
        return new NewUserRequestDTO(
                "anv@gmail.com",
                "Password123@", // 12 ký tự, hợp lệ (8-32)
                "Password123@",
                "Nguyen Van A",
                "LOCAL");
    }

    @Test
    @DisplayName("trả về 202 khi Email chưa tồn tại, Email đúng định dạng")
    void test_register_006() throws Exception {
        when(usecase.register(any(NewUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 202 khi Email có ký tự đặc biệt")
    void test_register_007() throws Exception {
        NewUserRequestDTO req =
                new NewUserRequestDTO(
                        "anv_@gmail.com", "Password123@", "Password123@", "Nguyen Van A", "LOCAL");
        when(usecase.register(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 400 khi Email có ký tự đặc biệt không hợp lệ")
    void test_register_008() throws Exception {
        NewUserRequestDTO req =
                new NewUserRequestDTO(
                        "anv@@gmail.com", "Password123@", "Password123@", "Nguyen Van A", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 202 khi Email có \".\"  hợp lệ")
    void test_register_009() throws Exception {
        NewUserRequestDTO req =
                new NewUserRequestDTO(
                        "anv.tlu@gmail.com",
                        "Password123@",
                        "Password123@",
                        "Nguyen Van A",
                        "LOCAL");

        when(usecase.register(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 400 khi Email có \".\"  không hợp lệ")
    void test_register_010() throws Exception {
        NewUserRequestDTO req =
                new NewUserRequestDTO(
                        ".anv@gmail.com", "Password123@", "Password123@", "Nguyen Van A", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 202 khi Email viết hoa / thường")
    void test_register_011() throws Exception {
        NewUserRequestDTO req =
                new NewUserRequestDTO(
                        "ANV@gmail.com", "Password123@", "Password123@", "Nguyen Van A", "LOCAL");

        when(usecase.register(any(req.getClass()))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 400 khi Email chứa ký tự Unicode")
    void test_register_012() throws Exception {
        NewUserRequestDTO unicodeRequest =
                new NewUserRequestDTO(
                        "anguyễnvăn@gmail.com",
                        "Password123@",
                        "Password123@",
                        "Nguyen Van A",
                        "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(unicodeRequest)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email chứa khoảng trắng")
    void test_register_013() throws Exception {
        NewUserRequestDTO spaceRequest =
                new NewUserRequestDTO(
                        "anv tlu@gmail.com",
                        "Password123@",
                        "Password123@",
                        "Nguyen Van A",
                        "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(spaceRequest)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email dài hơn 254 ký tự")
    void test_register_014() throws Exception {
        NewUserRequestDTO more254Request =
                new NewUserRequestDTO(
                        "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmd@a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.a123456789b123456789c123456789d123456789e123456789f123456789g12.com",
                        "Password123@",
                        "Password123@",
                        "Nguyen Van A",
                        "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(more254Request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email có local part dài hơn 64 ký tự")
    void test_register_015() throws Exception {
        NewUserRequestDTO localPartRequest =
                new NewUserRequestDTO(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@gmail.com",
                        "Password123@",
                        "Password123@",
                        "Nguyen Van A",
                        "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(localPartRequest)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Email rỗng")
    void test_register_016() throws Exception {
        NewUserRequestDTO empty =
                new NewUserRequestDTO("", "Password123@", "Password123@", "Nguyen Van A", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(empty)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 409 khi email đã tồn tại")
    void test_register_017() throws Exception {
        when(usecase.register(any(NewUserRequestDTO.class)))
                .thenThrow(new ConflictException("Tài khoản đã tồn tại"));

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 202 khi Mật khẩu có đúng 8 ký tự")
    void test_register_018() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO(
                        "boundary@gmail.com", "Passwo1@", "Passwo1@", "Nguyen Van A", "LOCAL");
        when(usecase.register(any(NewUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 202 khi Mật khẩu có đúng 32 ký tự")
    void test_register_019() throws Exception {
        String pw32 = "A1@" + "a".repeat(29);
        NewUserRequestDTO request =
                new NewUserRequestDTO("boundary2@gmail.com", pw32, pw32, "Nguyen Van A", "LOCAL");
        when(usecase.register(any(NewUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 202 khi Mật khẩu có chữ hoa, ký tự đặc biệt, số")
    void test_register_020() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO(
                        "boundary2@gmail.com",
                        "Password123@",
                        "Password123@",
                        "Nguyen Van A",
                        "LOCAL");
        when(usecase.register(any(NewUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 202 khi mật khẩu không có chữ hoa ký tự đặc biệt hoặc số")
    void test_register_021() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO(
                        "boundary2@gmail.com",
                        "passwordabcd",
                        "passwordabcd",
                        "Nguyen Van A",
                        "LOCAL");
        when(usecase.register(any(NewUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu có khoảng trắng")
    void test_register_022() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO(
                        "boundary2@gmail.com",
                        "password abcd",
                        "password abcd",
                        "Nguyen Van A",
                        "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu có ít hơn 8 ký tự")
    void test_register_023() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO("anv@gmail.com", "Pas1@", "Pas1@", "Nguyen Van A", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu có nhiều hơn 32 ký tự")
    void test_register_024() throws Exception {
        String pw33 = "A1@" + "a".repeat(30); // tổng 33 ký tự
        NewUserRequestDTO request =
                new NewUserRequestDTO("anv@gmail.com", pw33, pw33, "Nguyen Van A", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Mật khẩu rỗng")
    void test_register_025() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO("anv@gmail.com", "", "Password123@", "Nguyen Van A", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Xác nhận mật khẩu rỗng")
    void test_register_026() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO("anv@gmail.com", "password123@", "", "Nguyen Van A", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 400 khi Xác nhận mật khẩu không trùng khớp")
    void test_register_027() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO(
                        "anv@gmail.com", "password123@", "password123", "Nguyen Van A", "LOCAL");

        when(usecase.register(any(request.getClass())))
                .thenThrow(new BadRequestException("Mật khẩu xác nhận không trùng khớp"));

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 400 khi Tên người dùng rỗng")
    void test_register_028() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO("anv@gmail.com", "Password123@", "Password123@", "", "LOCAL");

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(usecase, times(0)).register(any());
    }

    @Test
    @DisplayName("trả về 202 khi Tên người dùng là tiếng Việt")
    void test_register_029() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO(
                        "user.vn@gmail.com",
                        "Password123@",
                        "Password123@",
                        "Nguyễn Văn A",
                        "LOCAL");
        when(usecase.register(any(NewUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding("UTF-8")
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về 202 khi Tên người dùng có ký tự đặc biệt")
    void test_register_030() throws Exception {
        NewUserRequestDTO request =
                new NewUserRequestDTO(
                        "anv@gmail.com", "Password123@", "Password123@", "anv@123", "LOCAL");
        when(usecase.register(any(NewUserRequestDTO.class))).thenReturn(mockAuthorizedResponse);

        mvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding("UTF-8")
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("accessToken"))
                .andExpect(cookie().doesNotExist("refreshToken"))
                .andExpect(cookie().httpOnly("verificationToken", true));

        verify(usecase, times(1)).register(any(NewUserRequestDTO.class));
    }

    @Test
    @DisplayName("trả về metadata đã che email và cấm lưu cache khi cookie xác thực còn hiệu lực")
    void test_metadata_031() throws Exception {
        when(usecase.metadata("pending-token")).thenReturn(mockAuthorizedResponse.verification());

        mvc.perform(
                        get("/auth/verification")
                                .cookie(new Cookie("verificationToken", "pending-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$.email").value("a***@gmail.com"))
                .andExpect(jsonPath("$.remainingAttempts").value(5))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("trả về metadata mới và không cấp JWT khi gửi lại OTP thành công")
    void test_resend_032() throws Exception {
        when(usecase.resend("pending-token")).thenReturn(mockAuthorizedResponse.verification());

        mvc.perform(
                        post("/auth/verification/resend")
                                .cookie(new Cookie("verificationToken", "pending-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.remainingAttempts").value(5));
    }

    @Test
    @DisplayName("trả về hồ sơ và JWT qua cookie HttpOnly khi Google exchange xác thực thành công")
    void test_exchange_033() throws Exception {
        String code = "a".repeat(64);
        String verifier = "b".repeat(64);
        UserResponseDTO profile =
                new UserResponseDTO(
                        "teacher-1",
                        "teacher@example.com",
                        "Teacher",
                        "ROLE_TEACHER",
                        "avatars/default.png");
        AuthorizedResponseDTO authorized =
                new AuthorizedResponseDTO(profile, "access-fixture", "refresh-fixture");
        when(usecase.exchange(code, verifier))
                .thenReturn(new VerificationUsecase.Exchange(null, authorized));

        mvc.perform(
                        post("/auth/google/exchange")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("code", code, "verifier", verifier))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(cookie().value("accessToken", "access-fixture"))
                .andExpect(cookie().httpOnly("accessToken", true))
                .andExpect(cookie().secure("accessToken", true))
                .andExpect(cookie().value("refreshToken", "refresh-fixture"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().maxAge("refreshToken", 604800))
                .andExpect(cookie().maxAge("verificationToken", 0))
                .andExpect(jsonPath("$.id").value("teacher-1"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName(
            "trả về HTTP 400 trước use case khi mã exchange hoặc verifier Google sai định dạng")
    void test_exchange_034(boolean invalidCode) throws Exception {
        String code = invalidCode ? "invalid" : "a".repeat(64);
        String verifier = invalidCode ? "b".repeat(64) : "invalid";

        mvc.perform(
                        post("/auth/google/exchange")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("code", code, "verifier", verifier))))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(usecase);
    }

    @Test
    @DisplayName(
            "trả về HTTP 401 cùng mã hết hạn và không cấp cookie khi xác minh OTP của phiên đã hết hạn")
    void test_verify_035() throws Exception {
        when(usecase.verify("expired-token", "012345"))
                .thenThrow(
                        new VerificationException("VERIFICATION_EXPIRED", "Phiên đã hết hạn", 401));

        mvc.perform(
                        post("/auth/verification/verify")
                                .cookie(new Cookie("verificationToken", "expired-token"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"otp\":\"012345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("VERIFICATION_EXPIRED"))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
