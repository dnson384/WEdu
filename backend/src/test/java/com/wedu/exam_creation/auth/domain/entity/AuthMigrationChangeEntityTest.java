package com.wedu.exam_creation.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthMigrationChangeEntityTest {
    @Test
    @DisplayName("trả về chuỗi không chứa ID hay email khi biểu diễn thay đổi của migration")
    void test_toString_001() {
        AuthMigrationChangeEntity change =
                new AuthMigrationChangeEntity("legacy-id", "teacher@example.com", "MIXED");

        assertThat(change.toString()).doesNotContain("legacy-id", "teacher@example.com");
    }
}
