package com.wedu.exam_creation.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailNormalizerTest {
    @Test
    @DisplayName("trả về email chữ thường khi đầu vào có chữ hoa và khoảng trắng bao quanh")
    void test_normalize_001() {
        assertThat(EmailNormalizer.normalize(" \tMINH+TAG@EXAMPLE.COM\n"))
                .isEqualTo("minh+tag@example.com");
    }

    @Test
    @DisplayName("trả về email giữ nguyên khi đầu vào đã được chuẩn hóa")
    void test_normalize_002() {
        String email = "minh+tag@example.com";

        assertThat(EmailNormalizer.normalize(email)).isEqualTo(email);
    }

    @Test
    @DisplayName("trả về true khi email đã viết thường và không có khoảng trắng bao quanh")
    void test_isCanonical_003() {
        assertThat(EmailNormalizer.isCanonical("minh+tag@example.com")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t", "Minh@example.com", "minh@example.com "})
    @DisplayName("trả về false khi email bị thiếu, chỉ có khoảng trắng hoặc cần chuẩn hóa")
    void test_isCanonical_004(String email) {
        assertThat(EmailNormalizer.isCanonical(email)).isFalse();
    }
}
