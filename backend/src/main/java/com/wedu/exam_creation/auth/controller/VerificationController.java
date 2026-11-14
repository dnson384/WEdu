package com.wedu.exam_creation.auth.controller;

import com.wedu.exam_creation.auth.dto.request.GoogleExchangeRequestDTO;
import com.wedu.exam_creation.auth.dto.request.VerifyOtpRequestDTO;
import com.wedu.exam_creation.auth.dto.response.AuthorizedResponseDTO;
import com.wedu.exam_creation.auth.dto.response.VerificationResponseDTO;
import com.wedu.exam_creation.auth.usecase.VerificationUsecase;
import com.wedu.exam_creation.common.dto.user.request.NewUserRequestDTO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class VerificationController {
    private final VerificationUsecase usecase;
    private final boolean secure;

    public VerificationController(
            VerificationUsecase usecase, @Value("${app.frontend.url}") String frontendUrl) {
        this.usecase = usecase;
        this.secure = frontendUrl.startsWith("https://");
    }

    @ModelAttribute
    public void noStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
    }

    @PostMapping("/register")
    public ResponseEntity<VerificationResponseDTO> register(
            @Valid @RequestBody NewUserRequestDTO input) {
        return pending(usecase.register(input));
    }

    @GetMapping("/verification")
    public ResponseEntity<VerificationResponseDTO> metadata(
            @CookieValue(value = "verificationToken", required = false) String token) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(usecase.metadata(token));
    }

    @PostMapping("/verification/resend")
    public ResponseEntity<VerificationResponseDTO> resend(
            @CookieValue(value = "verificationToken", required = false) String token) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(usecase.resend(token));
    }

    @PostMapping("/verification/verify")
    public ResponseEntity<?> verify(
            @CookieValue(value = "verificationToken", required = false) String token,
            @Valid @RequestBody VerifyOtpRequestDTO input) {
        return authorized(usecase.verify(token, input.getOtp()));
    }

    @PostMapping("/google/exchange")
    public ResponseEntity<?> exchange(@Valid @RequestBody GoogleExchangeRequestDTO input) {
        var result = usecase.exchange(input.getCode(), input.getVerifier());
        return result.pending() == null
                ? authorized(result.authorized())
                : pending(result.pending());
    }

    private ResponseEntity<VerificationResponseDTO> pending(VerificationUsecase.Pending result) {
        return ResponseEntity.accepted()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.SET_COOKIE, cookie("verificationToken", result.token(), 1800))
                .body(result.verification());
    }

    private ResponseEntity<?> authorized(AuthorizedResponseDTO result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie("accessToken", result.getAccessToken(), 300),
                        cookie("refreshToken", result.getRefreshToken(), 604800),
                        cookie("verificationToken", "", 0))
                .body(result.getUser());
    }

    private String cookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
