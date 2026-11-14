package com.wedu.exam_creation.security.infrastructure.handler;

import com.wedu.exam_creation.auth.usecase.VerificationUsecase;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final VerificationUsecase verification;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            VerificationUsecase verification, @Value("${app.frontend.url}") String frontendUrl) {
        this.verification = verification;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        String target = frontendUrl + "/api/auth/google/callback?error=google_failed";
        try {
            var session = request.getSession(false);
            String binding =
                    session == null ? null : (String) session.getAttribute("googleBinding");
            Long started = session == null ? null : (Long) session.getAttribute("googleStarted");
            if (started == null || System.currentTimeMillis() - started > 600_000)
                throw new IllegalStateException("OAuth expired");
            OidcUser user = (OidcUser) authentication.getPrincipal();
            String email = user.getEmail();
            String hd = user.getClaimAsString("hd");
            boolean authoritative =
                    email != null
                            && (email.toLowerCase(Locale.ROOT).endsWith("@gmail.com")
                                    || (hd != null && !hd.isBlank()));
            String code =
                    verification.googleHandoff(
                            email,
                            user.getFullName(),
                            user.getSubject(),
                            Boolean.TRUE.equals(user.getEmailVerified()),
                            authoritative,
                            binding);
            target = frontendUrl + "/api/auth/google/callback?code=" + code;
        } catch (RuntimeException ignored) {
            // Do not expose provider details, authorization codes or tokens in logs or error URLs.
        } finally {
            if (request.getSession(false) != null) request.getSession(false).invalidate();
        }
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
