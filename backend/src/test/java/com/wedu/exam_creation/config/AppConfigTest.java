package com.wedu.exam_creation.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;

class AppConfigTest {
    private final AppConfig config = new AppConfig();
    private final String storedHash = BCrypt.hashpw("config-test", "$2a$10$abcdefghijklmnopqrstuu");

    @Test
    @DisplayName("trả về clock có múi giờ UTC khi ứng dụng khởi tạo bean thời gian")
    void test_clock_001() {
        Clock clock = config.clock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("trả về khớp khi encoder kiểm tra đúng mật khẩu với hash BCrypt đã lưu")
    void test_passwordEncoder_002() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder.matches("config-test", storedHash)).isTrue();
    }

    @Test
    @DisplayName("trả về không khớp khi encoder kiểm tra mật khẩu khác với hash BCrypt đã lưu")
    void test_passwordEncoder_003() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder.matches("another-password", storedHash)).isFalse();
    }
}
