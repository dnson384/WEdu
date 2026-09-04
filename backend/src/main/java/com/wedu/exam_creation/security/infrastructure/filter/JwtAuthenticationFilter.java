package com.wedu.exam_creation.security.infrastructure.filter;

import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.constant.SecurityConstants;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import com.wedu.exam_creation.security.infrastructure.service.CustomUserDetailsService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@NullMarked
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String[] PUBLIC_PATHS = SecurityConstants.PUBLIC_PATHS;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String currentPath = request.getServletPath();
        return Arrays.stream(PUBLIC_PATHS)
                .anyMatch(path -> pathMatcher.match(path, currentPath));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException, java.io.IOException {
        try {
            String accessToken = getAccessTokenFromRequest(request);

            if (accessToken.trim().isEmpty()) {
                throw new UnAuthorizedException("Không có AT");
            }

            tokenProvider.validateAccessToken(accessToken);

            ATPayload payload = tokenProvider.getPayloadFromAccessToken(accessToken);

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(payload.getEmail());

            if (!userDetails.isEnabled()) {
                throw new ForbiddenException("Tài khoản đã bị khóa");
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadRequestException ex) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        } catch (UnAuthorizedException ex) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            return;
        } catch (ForbiddenException ex) {
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            return;
        } catch (UsernameNotFoundException | NotFoundException ex) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            log.error("Lỗi không xác định khi xác thực JWT", ex);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi xác thực");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getAccessTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        throw new BadRequestException("Header Authorization sai định dạng");
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException, java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                new ObjectMapper().writeValueAsString(Map.of("message", message))
        );
    }
}