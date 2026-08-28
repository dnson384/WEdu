package com.wedu.exam_creation.security.constant;

public final class SecurityConstants {
    public static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/register",
            "/auth/logout",
            "/auth/regenerate-access-token",
            "/static/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    };

    private SecurityConstants() {
    }
}
