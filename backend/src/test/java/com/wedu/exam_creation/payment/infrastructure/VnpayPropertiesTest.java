package com.wedu.exam_creation.payment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VnpayPropertiesTest {
    @Test
    @DisplayName("trả về true khi merchant đã bật và đủ cấu hình HTTPS")
    void test_available_001() {
        assertThat(configured().available()).isTrue();
    }

    @Test
    @DisplayName("trả về false khi merchant bị tắt hoặc thiếu thông tin bắt buộc")
    void test_available_002() {
        VnpayProperties disabled = configured();
        disabled.setEnabled(false);
        VnpayProperties incomplete = configured();
        incomplete.setTmnCode("");
        VnpayProperties missingSecret = configured();
        missingSecret.setHashSecret("");

        assertThat(disabled.available()).isFalse();
        assertThat(incomplete.available()).isFalse();
        assertThat(missingSecret.available()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://checkout.example.test",
                "https:///no-host",
                "https://wedu.example.test/return?param=1",
                "https://wedu.example.test/return#done",
                "https://user@wedu.example.test/return",
                "not a URI"
            })
    @DisplayName("trả về false khi URL cấu hình vi phạm yêu cầu HTTPS hoặc có thành phần bị cấm")
    void test_available_003(String url) {
        VnpayProperties checkout = configured();
        checkout.setPaymentUrl(url);
        VnpayProperties query = configured();
        query.setQueryUrl(url);
        VnpayProperties callback = configured();
        callback.setReturnUrl(url);

        assertThat(checkout.available()).isFalse();
        assertThat(query.available()).isFalse();
        assertThat(callback.available()).isFalse();
    }

    private VnpayProperties configured() {
        VnpayProperties properties = new VnpayProperties();
        properties.setEnabled(true);
        properties.setTmnCode("TEST-MERCHANT");
        properties.setHashSecret("test-signing-value");
        properties.setPaymentUrl("https://checkout.example.test/pay");
        properties.setQueryUrl("https://checkout.example.test/query");
        properties.setReturnUrl("https://wedu.example.test/return");
        return properties;
    }
}
