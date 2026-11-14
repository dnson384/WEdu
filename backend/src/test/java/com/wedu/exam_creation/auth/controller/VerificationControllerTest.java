package com.wedu.exam_creation.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.wedu.exam_creation.auth.dto.response.*;
import com.wedu.exam_creation.auth.usecase.*;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
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
    void verifyReturnsUserAndHttpOnlyCookiesWithoutJwtInJson() throws Exception {
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
    void rejectsMalformedOtpBeforeUsecase() throws Exception {
        mvc.perform(
                        post("/auth/verification/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"otp\":\"12345x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Cache-Control", "private, no-store"));
        verifyNoInteractions(usecase);
    }

    @Test
    void rateLimitReturnsRetryAfterAndMachineCode() throws Exception {
        when(usecase.resend(any()))
                .thenThrow(new VerificationException("OTP_COOLDOWN", "wait", 429, 80));
        mvc.perform(post("/auth/verification/resend"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "80"))
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
    }

    @Test
    void expiredSessionDoesNotIssueCookies() throws Exception {
        when(usecase.metadata((String) null))
                .thenThrow(new VerificationException("VERIFICATION_EXPIRED", "expired", 401));
        mvc.perform(get("/auth/verification"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("accessToken"));
    }

    @Test
    void exchangeCanReturnPendingWithoutExposingVerificationToken() throws Exception {
        var now = Instant.now();
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
}
