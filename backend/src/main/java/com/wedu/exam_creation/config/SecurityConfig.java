package com.wedu.exam_creation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.security.constant.SecurityConstants;
import com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilter;
import com.wedu.exam_creation.security.infrastructure.handler.OAuth2AuthenticationSuccessHandler;
import com.wedu.exam_creation.security.infrastructure.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    public final String[] PUBLIC_PATHS = SecurityConstants.PUBLIC_PATHS;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2AuthenticationSuccessHandler oauth2SuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain googleSecurityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clients,
            @Value("${app.frontend.url}") String frontendUrl)
            throws Exception {
        var delegate =
                new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
        OAuth2AuthorizationRequestResolver resolver =
                new OAuth2AuthorizationRequestResolver() {
                    private OAuth2AuthorizationRequest bind(
                            HttpServletRequest request, OAuth2AuthorizationRequest result) {
                        if (result != null) {
                            String binding = request.getParameter("binding");
                            if (binding == null || !binding.matches("[a-f0-9]{64}"))
                                throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_request"));
                            var session = request.getSession(true);
                            session.setMaxInactiveInterval(600);
                            session.setAttribute("googleBinding", binding);
                            session.setAttribute("googleStarted", System.currentTimeMillis());
                        }
                        return result;
                    }

                    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                        return bind(request, delegate.resolve(request));
                    }

                    public OAuth2AuthorizationRequest resolve(
                            HttpServletRequest request, String id) {
                        return bind(request, delegate.resolve(request, id));
                    }
                };
        http.securityMatcher("/oauth2/authorization/google", "/auth/login/oauth2/code/google")
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(
                        context ->
                                context.securityContextRepository(
                                        new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .oauth2Login(
                        oauth ->
                                oauth.authorizationEndpoint(
                                                endpoint ->
                                                        endpoint.authorizationRequestResolver(
                                                                resolver))
                                        .redirectionEndpoint(
                                                endpoint ->
                                                        endpoint.baseUri(
                                                                "/auth/login/oauth2/code/*"))
                                        .userInfoEndpoint(
                                                info ->
                                                        info.oidcUserService(
                                                                customOAuth2UserService))
                                        .successHandler(oauth2SuccessHandler)
                                        .failureHandler(
                                                (request, response, exception) -> {
                                                    if (request.getSession(false) != null)
                                                        request.getSession(false).invalidate();
                                                    response.setHeader("Cache-Control", "no-store");
                                                    response.setHeader(
                                                            "Referrer-Policy", "no-referrer");
                                                    response.sendRedirect(
                                                            frontendUrl.replaceAll("/+$", "")
                                                                    + "/api/auth/google/callback?error=google_failed");
                                                }));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                // 2. Bộ lọc JWT cho các request thông thường từ ứng dụng
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                    response.setStatus(
                                                            HttpServletResponse.SC_UNAUTHORIZED);
                                                    response.setContentType("application/json");
                                                    response.setCharacterEncoding("UTF-8");
                                                    response.getWriter()
                                                            .write(
                                                                    new ObjectMapper()
                                                                            .writeValueAsString(
                                                                                    Map.of(
                                                                                            "message",
                                                                                            "Chưa xác thực")));
                                                })
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) -> {
                                                    response.setStatus(
                                                            HttpServletResponse.SC_FORBIDDEN);
                                                    response.setContentType("application/json");
                                                    response.setCharacterEncoding("UTF-8");
                                                    response.getWriter()
                                                            .write(
                                                                    new ObjectMapper()
                                                                            .writeValueAsString(
                                                                                    Map.of(
                                                                                            "message",
                                                                                            "Không có quyền truy cập")));
                                                }));

        return http.build();
    }
}
