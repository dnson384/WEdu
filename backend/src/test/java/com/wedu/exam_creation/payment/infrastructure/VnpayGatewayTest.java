package com.wedu.exam_creation.payment.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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
    @DisplayName("trả về chữ ký HMAC đúng chuẩn khi dùng vector RFC 4231")
    void test_sign_001() {
        VnpayProperties p = properties();
        p.setHashSecret("\u000b".repeat(20));
        assertThat(new VnpayGateway(p).sign("Hi There"))
                .isEqualTo(
                        "87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cde"
                                + "daa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854");
    }

    @Test
    @DisplayName(
            "trả về chuỗi tham số đã sắp xếp và mã hóa khi dữ liệu có chữ ký và trường không dùng")
    void test_canonical_002() {
        Map<String, String> values =
                Map.of(
                        "vnp_Z",
                        "A B+C",
                        "vnp_A",
                        "100",
                        "vnp_SecureHash",
                        "ignore",
                        "vnp_SecureHashType",
                        "SHA512",
                        "vnp_Empty",
                        "",
                        "untrusted",
                        "x");
        assertThat(VnpayGateway.canonical(values)).isEqualTo("vnp_A=100&vnp_Z=A+B%2BC");
    }

    PaymentOrderEntity order() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setAmount(20000);
        order.setCreatedAt(Instant.parse("2026-11-03T00:00:00Z"));
        order.setExpiresAt(Instant.parse("2026-11-03T00:05:00Z"));
        return order;
    }

    Map<String, String> checkoutValues(String url) {
        Map<String, String> values = new HashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    @Test
    @DisplayName("trả về URL đúng số tiền, ngày và URL quay về khi không cấu hình ngân hàng")
    void test_paymentUrl_003() {
        VnpayGateway gateway = new VnpayGateway(properties());
        String url = gateway.paymentUrl(order(), "127.0.0.1");
        assertThat(url)
                .contains(
                        "vnp_Amount=2000000",
                        "vnp_CreateDate=20261103070000",
                        "vnp_ExpireDate=20261103070500",
                        "vnp_ReturnUrl=https%3A%2F%2Fexample.com%2Fme%2Fbill")
                .doesNotContain("vnp_BankCode=");
        assertThat(gateway.validIpn(checkoutValues(url))).isTrue();
    }

    @Test
    @DisplayName("trả về URL có mã ngân hàng trong phần ký khi cấu hình kênh QR")
    void test_paymentUrl_004() {
        VnpayProperties p = properties();
        p.setBankCode("VNPAYQR");
        VnpayGateway gateway = new VnpayGateway(p);
        String url = gateway.paymentUrl(order(), "127.0.0.1");
        assertThat(url).contains("vnp_BankCode=VNPAYQR");
        Map<String, String> values = checkoutValues(url);
        assertThat(gateway.validIpn(values)).isTrue();
        values.put("vnp_BankCode", "NCB");
        assertThat(gateway.validIpn(values)).isFalse();
    }

    @Test
    @DisplayName("trả về URL bỏ mã ngân hàng khi cấu hình giá trị rỗng hoặc chỉ có khoảng trắng")
    void test_paymentUrl_005() {
        VnpayProperties p = properties();
        for (String bankCode : new String[] {null, "", "   "}) {
            p.setBankCode(bankCode);
            VnpayGateway gateway = new VnpayGateway(p);
            String url = gateway.paymentUrl(order(), "127.0.0.1");
            assertThat(p.available()).isTrue();
            assertThat(url).doesNotContain("vnp_BankCode=");
            assertThat(gateway.validIpn(checkoutValues(url))).isTrue();
        }
    }

    @Test
    @DisplayName(
            "trả về true khi chữ ký phản hồi đúng chuỗi các trường phân cách bằng dấu gạch đứng")
    void test_validQuery_006() {
        VnpayGateway gateway = new VnpayGateway(properties());
        Map<String, String> response =
                new HashMap<>(
                        Map.of(
                                "vnp_ResponseId",
                                "response1",
                                "vnp_Command",
                                "querydr",
                                "vnp_ResponseCode",
                                "00",
                                "vnp_Message",
                                "OK",
                                "vnp_TmnCode",
                                "TESTCODE",
                                "vnp_TxnRef",
                                "order1",
                                "vnp_Amount",
                                "2000000",
                                "vnp_TransactionNo",
                                "123",
                                "vnp_TransactionType",
                                "01",
                                "vnp_TransactionStatus",
                                "00"));
        response.put(
                "vnp_SecureHash",
                gateway.sign("response1|querydr|00|OK|TESTCODE|order1|2000000|||123|01|00|||"));
        assertThat(gateway.validQuery(response)).isTrue();
        response.put("vnp_Amount", "1");
        assertThat(gateway.validQuery(response)).isFalse();
    }

    @Test
    @DisplayName("trả về false khi chữ ký sai định dạng hoặc merchant bị tắt")
    void test_validIpn_007() {
        VnpayProperties p = properties();
        VnpayGateway gateway = new VnpayGateway(p);
        assertThat(gateway.validIpn(Map.of("vnp_SecureHash", "zz"))).isFalse();
        Map<String, String> response = new HashMap<>(Map.of("vnp_TmnCode", "TESTCODE"));
        response.put("vnp_SecureHash", gateway.sign(VnpayGateway.canonical(response)));
        assertThat(gateway.validIpn(response)).isTrue();
        p.setEnabled(false);
        assertThat(gateway.validIpn(response)).isFalse();
    }

    MockRestServiceServer server;
    VnpayGateway gateway;
    PaymentOrderEntity order;
    final ObjectMapper json = new ObjectMapper();
    final Logger logger = (Logger) LoggerFactory.getLogger(VnpayGateway.class);
    ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setup() {
        VnpayProperties p = new VnpayProperties();
        p.setEnabled(true);
        p.setTmnCode("TESTCODE");
        p.setHashSecret("dummy-secret-not-for-logs");
        p.setReturnUrl("https://example.com/me/bill");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new VnpayGateway(p, builder.build());
        order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setCreatedAt(Instant.parse("2026-11-05T00:00:00Z"));
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void cleanup() {
        logger.detachAppender(logs);
        logs.stop();
    }

    Map<String, String> signedResponse() {
        Map<String, String> values =
                new HashMap<>(
                        Map.of(
                                "vnp_ResponseId",
                                "response1",
                                "vnp_Command",
                                "querydr",
                                "vnp_ResponseCode",
                                "00",
                                "vnp_Message",
                                "OK",
                                "vnp_TmnCode",
                                "TESTCODE",
                                "vnp_TxnRef",
                                order.getId(),
                                "vnp_Amount",
                                "2000000",
                                "vnp_TransactionNo",
                                "123",
                                "vnp_TransactionType",
                                "01",
                                "vnp_TransactionStatus",
                                "00"));
        String[] keys = {
            "vnp_ResponseId",
            "vnp_Command",
            "vnp_ResponseCode",
            "vnp_Message",
            "vnp_TmnCode",
            "vnp_TxnRef",
            "vnp_Amount",
            "vnp_BankCode",
            "vnp_PayDate",
            "vnp_TransactionNo",
            "vnp_TransactionType",
            "vnp_TransactionStatus",
            "vnp_OrderInfo",
            "vnp_PromotionCode",
            "vnp_PromotionAmount"
        };
        values.put(
                "vnp_SecureHash",
                gateway.sign(
                        String.join(
                                "|",
                                Arrays.stream(keys)
                                        .map(k -> values.getOrDefault(k, ""))
                                        .toList())));
        return values;
    }

    @Test
    @DisplayName(
            "trả về kết quả đã xác minh khi query dùng ngày checkout gốc và phản hồi có chữ ký hợp lệ")
    void test_query_008() throws Exception {
        server.expect(requestTo("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        request -> {
                            Map<String, String> body =
                                    json.readValue(
                                            ((MockClientHttpRequest) request).getBodyAsString(),
                                            new TypeReference<Map<String, String>>() {});
                            assertThat(body)
                                    .containsEntry("vnp_Command", "querydr")
                                    .containsEntry("vnp_TransactionDate", "20261105070000")
                                    .containsEntry("vnp_TxnRef", order.getId());
                            assertThat(body.get("vnp_RequestId")).matches("[a-f0-9]{32}");
                            String input =
                                    String.join(
                                            "|",
                                            Arrays.stream(
                                                            new String[] {
                                                                "vnp_RequestId",
                                                                "vnp_Version",
                                                                "vnp_Command",
                                                                "vnp_TmnCode",
                                                                "vnp_TxnRef",
                                                                "vnp_TransactionDate",
                                                                "vnp_CreateDate",
                                                                "vnp_IpAddr",
                                                                "vnp_OrderInfo"
                                                            })
                                                    .map(body::get)
                                                    .toList());
                            assertThat(body.get("vnp_SecureHash")).isEqualTo(gateway.sign(input));
                        })
                .andRespond(
                        withSuccess(
                                json.writeValueAsString(signedResponse()),
                                MediaType.APPLICATION_JSON));
        assertThat(gateway.query(order)).containsEntry("vnp_TransactionStatus", "00");
        server.verify();
    }

    @Test
    @DisplayName("trả về request ký đúng checksum khi dùng vector cố định độc lập")
    void test_query_009() throws Exception {
        UUID requestId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        Instant requestTime = Instant.parse("2026-11-18T14:45:24Z");
        // Expected HMAC-SHA512 calculated independently with .NET using the dummy test key.
        String expectedHash =
                "5da705d4df44cf27b66eb9eee6b4d736de6375dbc2373d1f7562416ee0de98973"
                        + "2e2c77afc5f34b00fbd04e6c58be2317878196092a13661433491707eea4a58";
        String response = json.writeValueAsString(signedResponse());
        server.expect(requestTo("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        request -> {
                            Map<String, String> body =
                                    json.readValue(
                                            ((MockClientHttpRequest) request).getBodyAsString(),
                                            new TypeReference<Map<String, String>>() {});
                            assertThat(body)
                                    .containsEntry(
                                            "vnp_RequestId", "0123456789abcdef0123456789abcdef")
                                    .containsEntry("vnp_Version", "2.1.0")
                                    .containsEntry("vnp_Command", "querydr")
                                    .containsEntry("vnp_TmnCode", "TESTCODE")
                                    .containsEntry("vnp_TxnRef", "a".repeat(64))
                                    .containsEntry("vnp_TransactionDate", "20261105070000")
                                    .containsEntry("vnp_CreateDate", "20261118214524")
                                    .containsEntry("vnp_IpAddr", "127.0.0.1")
                                    .containsEntry(
                                            "vnp_OrderInfo", "Kiem tra WEdu " + "a".repeat(64))
                                    .containsEntry("vnp_SecureHash", expectedHash);
                            assertThat(body).doesNotContainKey("vnp_TransactionNo");
                        })
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        try (MockedStatic<UUID> uuids = mockStatic(UUID.class);
                MockedStatic<Instant> instants = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            uuids.when(UUID::randomUUID).thenReturn(requestId);
            instants.when(Instant::now).thenReturn(requestTime);
            assertThat(gateway.query(order)).containsEntry("vnp_TransactionStatus", "00");
        }
        server.verify();
    }

    @Test
    @DisplayName("trả về chữ ký đúng vector kiểm tra khi dữ liệu đầu vào là UTF-8")
    void test_sign_010() {
        // Includes Vietnamese text, spaces and URL-sensitive characters without encoding them.
        assertThat(gateway.sign("Yêu cầu tra cứu WEdu + & %"))
                .isEqualTo(
                        "deb1d3210e67a79dc5791cfc891e516d02596fca8eddc3c8a4e9966cb0856eaf"
                                + "a21486c81189d5406434bf7359d8f05c1b6f6fd6607e8e4e81c82321bc51d4ad");
    }

    @Test
    @DisplayName("trả về lỗi chẩn đoán đã rút gọn khi VNPay trả mã lỗi thiếu chữ ký")
    void test_query_011() {
        server.expect(anything())
                .andRespond(
                        withSuccess(
                                "{\"vnp_ResponseCode\":\"99\",\"vnp_Message\":\"dummy-secret-not-for-logs\",\"vnp_SecureHash\":\"dummy-signature-not-for-logs\"}",
                                MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order))
                .isInstanceOfSatisfying(
                        VnpayQueryException.class,
                        e -> {
                            assertThat(e.getStage()).isEqualTo("UNVERIFIED_RESPONSE");
                            assertThat(e.getProviderCode()).isEqualTo("99");
                            assertThat(e.getCause()).isNull();
                            assertThat(e.getMessage())
                                    .doesNotContain("dummy-secret", "dummy-signature");
                        });
        assertThat(logs.list)
                .anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("providerCode=99"));
        assertThat(logs.list)
                .allSatisfy(
                        e ->
                                assertThat(e.getFormattedMessage())
                                        .doesNotContain(
                                                "dummy-secret", "dummy-signature", order.getId()));
        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi xác minh khi dữ liệu phản hồi thành công không khớp chữ ký")
    void test_query_012() throws Exception {
        Map<String, String> result = signedResponse();
        result.put("vnp_Amount", "1");
        server.expect(anything())
                .andRespond(
                        withSuccess(json.writeValueAsString(result), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order))
                .isInstanceOfSatisfying(
                        VnpayQueryException.class,
                        e -> assertThat(e.getStage()).isEqualTo("UNVERIFIED_RESPONSE"));
        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi HTTP không lộ response body khi máy chủ trả lỗi")
    void test_query_013() {
        server.expect(anything()).andRespond(withServerError().body("dummy-secret-not-for-logs"));
        assertThatThrownBy(() -> gateway.query(order))
                .isInstanceOfSatisfying(
                        VnpayQueryException.class,
                        e -> {
                            assertThat(e.getStage()).isEqualTo("HTTP_ERROR");
                            assertThat(e.getHttpStatus()).isEqualTo(500);
                            assertThat(e.getMessage()).doesNotContain("dummy-secret");
                        });
        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi kết nối không lộ chi tiết riêng khi HTTP client timeout")
    void test_query_014() {
        server.expect(anything())
                .andRespond(withException(new SocketTimeoutException("dummy-secret-not-for-logs")));
        assertThatThrownBy(() -> gateway.query(order))
                .isInstanceOfSatisfying(
                        VnpayQueryException.class,
                        e -> {
                            assertThat(e.getStage()).isEqualTo("TRANSPORT_ERROR");
                            assertThat(e.getMessage()).doesNotContain("dummy-secret");
                        });
        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi định dạng khi phản hồi không phải JSON hợp lệ")
    void test_query_015() {
        server.expect(anything()).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order))
                .isInstanceOfSatisfying(
                        VnpayQueryException.class,
                        e -> assertThat(e.getStage()).isEqualTo("RESPONSE_FORMAT"));
        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi phản hồi rỗng khi máy chủ không trả body")
    void test_query_016() {
        server.expect(anything()).andRespond(withNoContent());
        assertThatThrownBy(() -> gateway.query(order))
                .isInstanceOfSatisfying(
                        VnpayQueryException.class,
                        e -> assertThat(e.getStage()).isEqualTo("EMPTY_RESPONSE"));
        server.verify();
    }

    @Test
    @DisplayName("trả về mã chẩn đoán an toàn khi trường báo lỗi của provider không hợp lệ")
    void test_query_017() {
        server.expect(anything())
                .andRespond(
                        withSuccess(
                                "{\"vnp_ResponseCode\":\"dummy-secret-not-for-logs\"}",
                                MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order))
                .isInstanceOfSatisfying(
                        VnpayQueryException.class,
                        e -> assertThat(e.getProviderCode()).isEqualTo("UNKNOWN"));
        assertThat(logs.list)
                .allSatisfy(
                        e -> assertThat(e.getFormattedMessage()).doesNotContain("dummy-secret"));
        server.verify();
    }
}
