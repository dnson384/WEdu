package com.wedu.exam_creation.security.service;

import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import com.wedu.exam_creation.security.infrastructure.provider.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider provider;

    public SecurityService(PasswordEncoder passwordEncoder, JwtTokenProvider provider) {
        this.passwordEncoder = passwordEncoder;
        this.provider = provider;
    }

    // PasswordEncoder
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    public boolean validatePassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    // Generate Token
    public String generateAccessToken(ATPayload payload) {
        return provider.generateAccessToken(payload);
    }

    public String generateRefreshToken(RTPayload payload) {
        return provider.generateRefreshToken(payload);
    }

    // Parse Token
    public ATPayload getPayloadFromAccessToken(String token) {
        try {
            return provider.getPayloadFromAccessToken(token);
        } catch (Exception ex) {
            throw new UnAuthorizedException("Token không hợp lệ");
        }
    }

    public RTPayload getPayloadFromRefreshToken(String token) {
        try {
            return provider.getPayloadFromRefreshToken(token);
        } catch (Exception ex) {
            throw new UnAuthorizedException("Token không hợp lệ");
        }
    }

    public NewRTRequestDTO parseNewRefreshToken(String token) {
        return provider.parseNewRefreshToken(token);
    }

    // Validate Token
    public void validateAccessToken(String token) {
        provider.validateAccessToken(token);
    }

    public void validateRefreshToken(String token) {
        provider.validateRefreshToken(token);
    }
}
