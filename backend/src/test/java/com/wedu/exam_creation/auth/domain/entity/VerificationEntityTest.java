package com.wedu.exam_creation.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationEntityTest {
    @Test
    @DisplayName("trả về chuỗi không chứa email và các giá trị băm khi biểu diễn phiên xác thực")
    void test_toString_001() {
        VerificationEntity session = new VerificationEntity();
        session.setId("session-digest");
        session.setEmail("teacher@example.com");
        session.setHashedPassword("password-hash-fixture");
        session.setOtpHash("otp-digest");
        session.setBindingHash("binding-digest");

        assertThat(session.toString())
                .doesNotContain(
                        "session-digest",
                        "teacher@example.com",
                        "password-hash-fixture",
                        "otp-digest",
                        "binding-digest");
    }
}
