package com.wedu.exam_creation.security.constant;

public final class SecurityConstants {
    public static final String[] PUBLIC_PATHS = {
        "/payment/vnpay/ipn",
        "/auth/login",
        "/auth/register",
        "/auth/verification",
        "/auth/verification/verify",
        "/auth/verification/resend",
        "/auth/google/exchange",
        "/oauth2/authorization/google",
        "/auth/login/oauth2/code/google",
        "/auth/logout",
        "/auth/regenerate-access-token",
        "/auth/forgot-password",
        "/auth/reset-password",
        "/static/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml"
    };

    private SecurityConstants() {}
}
