package com.wedu.exam_creation.auth.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.wedu.exam_creation.auth.domain.entity.VerificationEntity;
import com.wedu.exam_creation.auth.infrastructure.document.VerificationDocument;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class VerificationMapperTest {
    private static final Instant NOW = Instant.parse("2026-11-13T00:00:00Z");
    private final VerificationMapper mapper = Mappers.getMapper(VerificationMapper.class);

    @Test
    @DisplayName("trả về entity giữ hash OTP, lượt thử và hạn phiên khi đọc phiên đăng ký LOCAL")
    void test_toEntity_001() {
        VerificationDocument document = new VerificationDocument();
        document.setId("session-digest");
        document.setPurpose("LOCAL_REGISTER");
        document.setEmail("teacher@example.com");
        document.setUsername("Teacher");
        document.setHashedPassword("password-hash-fixture");
        document.setOtpHash("otp-digest");
        document.setAttempts(3);
        document.setReady(true);
        document.setConsumed(true);
        document.setRevision(7);
        document.setExpiresAt(NOW.plusSeconds(1800));
        document.setOtpExpiresAt(NOW.plusSeconds(300));
        document.setResendAt(NOW.plusSeconds(120));

        VerificationEntity actual = mapper.toEntity(document);

        assertThat(actual).usingRecursiveComparison().isEqualTo(document);
    }

    @Test
    @DisplayName("trả về null khi thiếu document của phiên xác thực")
    void test_toEntity_002() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("trả về document giữ ràng buộc trình duyệt và định danh Google khi ghi handoff")
    void test_toDocument_003() {
        VerificationEntity identity = new VerificationEntity();
        identity.setId("session-digest");
        identity.setPurpose("HANDOFF");
        identity.setEmail("teacher@example.com");
        identity.setUsername("Teacher");
        identity.setUserId("teacher-1");
        identity.setGoogleSubject("google-subject");
        identity.setBindingHash("binding-digest");
        identity.setAuthoritativeEmail(true);
        identity.setExpiresAt(NOW.plusSeconds(60));

        VerificationDocument actual = mapper.toDocument(identity);

        assertThat(actual).usingRecursiveComparison().isEqualTo(identity);
    }

    @Test
    @DisplayName("trả về null khi thiếu entity của phiên xác thực")
    void test_toDocument_004() {
        assertThat(mapper.toDocument(null)).isNull();
    }
}
