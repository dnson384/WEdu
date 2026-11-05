package com.wedu.exam_creation.payment.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VnpayQueryGatewayTest {
    MockRestServiceServer server;
    VnpayGateway gateway;
    PaymentOrderEntity order;
    final ObjectMapper json = new ObjectMapper();
    final Logger logger = (Logger) LoggerFactory.getLogger(VnpayGateway.class);
    ListAppender<ILoggingEvent> logs;

    @BeforeEach void setup() {
        VnpayProperties p = new VnpayProperties();
        p.setEnabled(true);
        p.setTmnCode("TESTCODE");
        p.setHashSecret("dummy-secret-not-for-logs");
        p.setReturnUrl("https://example.com/me/bill");
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new VnpayGateway(p, builder.build());
        order = new PaymentOrderEntity();
        order.setId("a".repeat(64));
        order.setCreatedAt(Instant.parse("2026-11-05T00:00:00Z"));
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach void cleanup() {
        logger.detachAppender(logs);
        logs.stop();
    }

    Map<String, String> signedResponse() {
        Map<String, String> values = new HashMap<>(Map.of(
                "vnp_ResponseId", "response1", "vnp_Command", "querydr", "vnp_ResponseCode", "00",
                "vnp_Message", "OK", "vnp_TmnCode", "TESTCODE", "vnp_TxnRef", order.getId(),
                "vnp_Amount", "2000000", "vnp_TransactionNo", "123", "vnp_TransactionType", "01",
                "vnp_TransactionStatus", "00"));
        String[] keys = {"vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message",
                "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate",
                "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo",
                "vnp_PromotionCode", "vnp_PromotionAmount"};
        values.put("vnp_SecureHash", gateway.sign(String.join("|",
                Arrays.stream(keys).map(k -> values.getOrDefault(k, "")).toList())));
        return values;
    }

    @Test void sendsSignedQueryUsingOriginalCheckoutDateAndAcceptsSignedResult() throws Exception {
        server.expect(requestTo("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    Map<String, String> body = json.readValue(((MockClientHttpRequest) request).getBodyAsString(),
                            new TypeReference<Map<String, String>>() { });
                    assertThat(body).containsEntry("vnp_Command", "querydr")
                            .containsEntry("vnp_TransactionDate", "20261105070000")
                            .containsEntry("vnp_TxnRef", order.getId());
                    assertThat(body.get("vnp_RequestId")).matches("[a-f0-9]{32}");
                    String input = String.join("|", Arrays.stream(new String[]{"vnp_RequestId", "vnp_Version",
                            "vnp_Command", "vnp_TmnCode", "vnp_TxnRef", "vnp_TransactionDate",
                            "vnp_CreateDate", "vnp_IpAddr", "vnp_OrderInfo"})
                            .map(body::get).toList());
                    assertThat(body.get("vnp_SecureHash")).isEqualTo(gateway.sign(input));
                }).andRespond(withSuccess(json.writeValueAsString(signedResponse()), MediaType.APPLICATION_JSON));
        assertThat(gateway.query(order)).containsEntry("vnp_TransactionStatus", "00");
        server.verify();
    }

    @Test void unsignedProviderErrorIsDiagnosticOnlyAndDoesNotLeakResponse() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"vnp_ResponseCode\":\"99\",\"vnp_Message\":\"dummy-secret-not-for-logs\",\"vnp_SecureHash\":\"dummy-signature-not-for-logs\"}",
                MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order)).isInstanceOfSatisfying(VnpayQueryException.class, e -> {
            assertThat(e.getStage()).isEqualTo("UNVERIFIED_RESPONSE");
            assertThat(e.getProviderCode()).isEqualTo("99");
            assertThat(e.getCause()).isNull();
            assertThat(e.getMessage()).doesNotContain("dummy-secret", "dummy-signature");
        });
        assertThat(logs.list).anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("providerCode=99"));
        assertThat(logs.list).allSatisfy(e -> assertThat(e.getFormattedMessage())
                .doesNotContain("dummy-secret", "dummy-signature", order.getId()));
        server.verify();
    }

    @Test void tamperedSuccessIsRejected() throws Exception {
        Map<String, String> result = signedResponse();
        result.put("vnp_Amount", "1");
        server.expect(anything()).andRespond(withSuccess(json.writeValueAsString(result), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order)).isInstanceOfSatisfying(VnpayQueryException.class,
                e -> assertThat(e.getStage()).isEqualTo("UNVERIFIED_RESPONSE"));
        server.verify();
    }

    @Test void httpFailureIsDistinguishedWithoutLoggingBody() {
        server.expect(anything()).andRespond(withServerError().body("dummy-secret-not-for-logs"));
        assertThatThrownBy(() -> gateway.query(order)).isInstanceOfSatisfying(VnpayQueryException.class, e -> {
            assertThat(e.getStage()).isEqualTo("HTTP_ERROR");
            assertThat(e.getHttpStatus()).isEqualTo(500);
            assertThat(e.getMessage()).doesNotContain("dummy-secret");
        });
        server.verify();
    }

    @Test void networkFailureIsDistinguishedWithoutLoggingExceptionMessage() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("dummy-secret-not-for-logs")));
        assertThatThrownBy(() -> gateway.query(order)).isInstanceOfSatisfying(VnpayQueryException.class, e -> {
            assertThat(e.getStage()).isEqualTo("TRANSPORT_ERROR");
            assertThat(e.getMessage()).doesNotContain("dummy-secret");
        });
        server.verify();
    }

    @Test void malformedResponseIsDistinguished() {
        server.expect(anything()).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order)).isInstanceOfSatisfying(VnpayQueryException.class,
                e -> assertThat(e.getStage()).isEqualTo("RESPONSE_FORMAT"));
        server.verify();
    }

    @Test void emptyResponseIsDistinguished() {
        server.expect(anything()).andRespond(withNoContent());
        assertThatThrownBy(() -> gateway.query(order)).isInstanceOfSatisfying(VnpayQueryException.class,
                e -> assertThat(e.getStage()).isEqualTo("EMPTY_RESPONSE"));
        server.verify();
    }

    @Test void untrustedDiagnosticCodeIsSanitized() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"vnp_ResponseCode\":\"dummy-secret-not-for-logs\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.query(order)).isInstanceOfSatisfying(VnpayQueryException.class,
                e -> assertThat(e.getProviderCode()).isEqualTo("UNKNOWN"));
        assertThat(logs.list).allSatisfy(e -> assertThat(e.getFormattedMessage()).doesNotContain("dummy-secret"));
        server.verify();
    }
}
