package com.wedu.exam_creation.auth.infrastructure.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationDocumentTest {
    @Test
    @DisplayName(
            "trả về chuỗi không chứa định danh hay các giá trị băm khi biểu diễn document xác thực")
    void test_toString_001() {
        VerificationDocument session = new VerificationDocument();
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
