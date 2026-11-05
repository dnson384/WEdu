package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import com.wedu.exam_creation.payment.infrastructure.VnpayGateway;
import com.wedu.exam_creation.payment.infrastructure.VnpayProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnpayGatewayTest {
    VnpayProperties properties() {
        VnpayProperties p = new VnpayProperties();
        p.setEnabled(true);
        p.setTmnCode("TESTCODE");
        p.setHashSecret("test-only");
        p.setReturnUrl("https://example.com/me/bill");
        return p;
    }

    @Test
    void hmacMatchesRfc4231Vector() {
        VnpayProperties p = properties();
        p.setHashSecret("\u000b".repeat(20));
        assertThat(new VnpayGateway(p).sign("Hi There")).isEqualTo(
                "87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cde" +
                        "daa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854");
    }

    @Test
    void canonicalizationSortsEncodesAndExcludesSignature() {
        var values = Map.of("vnp_Z", "A B+C", "vnp_A", "100", "vnp_SecureHash", "ignore",
                "vnp_SecureHashType", "SHA512", "vnp_Empty", "", "untrusted", "x");
        assertThat(VnpayGateway.canonical(values)).isEqualTo("vnp_A=100&vnp_Z=A+B%2BC");
    }

    PaymentOrderEntity order() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setAmount(20000);
        order.setCreatedAt(Instant.parse("2026-11-03T00:00:00Z"));
        order.setExpiresAt(Instant.parse("2026-11-03T00:05:00Z"));
        order.setClientIp("127.0.0.1");
        return order;
    }

    Map<String, String> checkoutValues(String url) {
        Map<String, String> values = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    @Test
    void checkoutHasCorrectAmountDatesAndReturnUrlWithoutForcedBank() {
        VnpayGateway gateway = new VnpayGateway(properties());
        String url = gateway.paymentUrl(order());
        assertThat(url).contains(
                "vnp_Amount=2000000", "vnp_CreateDate=20261103070000", "vnp_ExpireDate=20261103070500",
                "vnp_ReturnUrl=https%3A%2F%2Fexample.com%2Fme%2Fbill").doesNotContain("vnp_BankCode=");
        assertThat(gateway.validIpn(checkoutValues(url))).isTrue();
    }

    @Test
    void configuredQrIsIncludedInSignedCheckoutAndCannotBeChanged() {
        VnpayProperties p = properties();
        p.setBankCode("VNPAYQR");
        VnpayGateway gateway = new VnpayGateway(p);
        String url = gateway.paymentUrl(order());
        assertThat(url).contains("vnp_BankCode=VNPAYQR");
        Map<String, String> values = checkoutValues(url);
        assertThat(gateway.validIpn(values)).isTrue();
        values.put("vnp_BankCode", "NCB");
        assertThat(gateway.validIpn(values)).isFalse();
    }

    @Test
    void emptyOrWhitespaceBankCodeIsOmittedWithoutBlockingAvailability() {
        VnpayProperties p = properties();
        for (String bankCode : new String[]{null, "", "   "}) {
            p.setBankCode(bankCode);
            VnpayGateway gateway = new VnpayGateway(p);
            String url = gateway.paymentUrl(order());
            assertThat(p.available()).isTrue();
            assertThat(url).doesNotContain("vnp_BankCode=");
            assertThat(gateway.validIpn(checkoutValues(url))).isTrue();
        }
    }

    @Test
    void querySignatureUsesPipeDelimitedResponseNotCheckoutEncoding() {
        VnpayGateway gateway = new VnpayGateway(properties());
        Map<String, String> response = new HashMap<>(Map.of(
                "vnp_ResponseId", "response1", "vnp_Command", "querydr", "vnp_ResponseCode", "00",
                "vnp_Message", "OK", "vnp_TmnCode", "TESTCODE", "vnp_TxnRef", "order1",
                "vnp_Amount", "2000000", "vnp_TransactionNo", "123", "vnp_TransactionType", "01",
                "vnp_TransactionStatus", "00"));
        response.put("vnp_SecureHash", gateway.sign("response1|querydr|00|OK|TESTCODE|order1|2000000|||123|01|00|||"));
        assertThat(gateway.validQuery(response)).isTrue();
        response.put("vnp_Amount", "1");
        assertThat(gateway.validQuery(response)).isFalse();
    }

    @Test
    void malformedHashAndDisabledMerchantAreRejected() {
        VnpayProperties p = properties();
        VnpayGateway gateway = new VnpayGateway(p);
        assertThat(gateway.validIpn(Map.of("vnp_SecureHash", "zz"))).isFalse();
        var response = new HashMap<>(Map.of("vnp_TmnCode", "TESTCODE"));
        response.put("vnp_SecureHash", gateway.sign(VnpayGateway.canonical(response)));
        assertThat(gateway.validIpn(response)).isTrue();
        p.setEnabled(false);
        assertThat(gateway.validIpn(response)).isFalse();
    }
}
