package com.wedu.exam_creation.security.infrastructure.provider;

import com.wedu.exam_creation.common.dto.refreshToken.request.NewRTRequestDTO;
import com.wedu.exam_creation.common.dto.token.ATPayload;
import com.wedu.exam_creation.common.dto.token.RTPayload;
import com.wedu.exam_creation.common.exception.UnAuthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    private final SecretKey accessSecret;
    private final SecretKey refreshSecret;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.access-secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.accessSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        this.refreshSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // AT
    public String generateAccessToken(ATPayload payload) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(payload.getUserId())
                .claim("parentJti", payload.getParentJti())
                .claim("email", payload.getEmail())
                .claim("role", payload.getRole())
                .issuedAt(now).expiration(expiryDate)
                .signWith(accessSecret)
                .compact();
    }

    public ATPayload getPayloadFromAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(accessSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new ATPayload(
                claims.get("parentJti", String.class),
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("role", String.class)
        );
    }

    // RT
    public String generateRefreshToken(RTPayload payload) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .id(payload.getJti())
                .subject(payload.getUserId())
                .claim("email", payload.getEmail())
                .claim("role", payload.getRole())
                .issuedAt(now).expiration(expiryDate)
                .signWith(refreshSecret)
                .compact();
    }

    public RTPayload getPayloadFromRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(refreshSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        RTPayload payload = new RTPayload();
        payload.setJti(claims.getId());
        payload.setUserId(claims.getSubject());
        payload.setEmail(claims.get("email", String.class));
        payload.setRole(claims.get("role", String.class));

        return payload;
    }

    public NewRTRequestDTO parseNewRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(refreshSecret)
                .build()
                .parseSignedClaims(token).getPayload();

        return new NewRTRequestDTO(
                claims.getId(),
                claims.getSubject(),
                claims.getExpiration().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
                claims.getIssuedAt().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
        );
    }

    public void validateAccessToken(String token) {
        validateToken(token, accessSecret, "Access Token");
    }

    public void validateRefreshToken(String token) {
        validateToken(token, refreshSecret, "Refresh Token");
    }

    private void validateToken(String token, SecretKey secretKey, String tokenType) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
        } catch (MalformedJwtException ex) {
            throw new UnAuthorizedException("Cấu trúc " + tokenType + " không hợp lệ");
        } catch (ExpiredJwtException ex) {
            throw new UnAuthorizedException(tokenType + " đã hết hạn");
        } catch (UnsupportedJwtException ex) {
            throw new UnAuthorizedException(tokenType + " không được hỗ trợ");
        } catch (IllegalArgumentException ex) {
            throw new UnAuthorizedException("Chuỗi Claims của " + tokenType + " đang để trống");
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            throw new UnAuthorizedException("Chữ ký " + tokenType + " không chính xác");
        } catch (io.jsonwebtoken.security.WeakKeyException ex) {
            throw new UnAuthorizedException("Chữ ký " + tokenType + " Secret Key quá yếu");
        }
    }
}
