package com.fckedu.exam_creation.security.infrastructure.handler;

import com.fckedu.exam_creation.common.dto.token.ATPayload;
import com.fckedu.exam_creation.common.dto.token.RTPayload;
import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.fckedu.exam_creation.user.domain.entity.UserEntity;
import com.fckedu.exam_creation.user.domain.repository.IUserRepository;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final IUserRepository userRepository;

    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider tokenProvider, IUserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException, java.io.IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        assert oAuth2User != null;
        String email = oAuth2User.getAttribute("email");

        Optional<UserEntity> userEntity = userRepository.findByEmail(email);
        if (userEntity.isEmpty()) {
            throw new NotFoundException("User chưa tồn tại");
        }

        UserEntity curUser = userEntity.get();
        String accessToken = tokenProvider.generateAccessToken(
                new ATPayload(curUser.getId(), curUser.getEmail(), curUser.getRole()));

        String jti = UUID.randomUUID().toString();
        String refreshToken = tokenProvider.generateRefreshToken(
                new RTPayload(jti, curUser.getId(), curUser.getEmail(), curUser.getRole()));

        ResponseCookie accessTokenCookie = ResponseCookie
                .from("access_token", accessToken)
                .httpOnly(true)
                // .secure(true)
                .path("/")
                .maxAge(15 * 60)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie
                .from("refresh_token", refreshToken)
                .httpOnly(true)
                // .secure(true)
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        // 3. Đẩy cookies vào Response Header
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // 4. Chuyển hướng người dùng về trang chủ của Frontend (Ví dụ: React/Next.js chạy cổng 3000)
        String targetUrl = "http://localhost:3000/dashboard";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
