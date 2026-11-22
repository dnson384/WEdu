package com.wedu.exam_creation.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthMigrationUserEntityTest {
    @Test
    @DisplayName("trả về chuỗi không chứa định danh và mật khẩu băm khi biểu diễn tài khoản cũ")
    void test_toString_001() {
        AuthMigrationUserEntity user =
                new AuthMigrationUserEntity(
                        "legacy-id",
                        "teacher@example.com",
                        "google-subject",
                        "LOCAL",
                        "password-hash-fixture");

        assertThat(user.toString())
                .doesNotContain(
                        "legacy-id",
                        "teacher@example.com",
                        "google-subject",
                        "password-hash-fixture");
    }
}
