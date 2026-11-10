package com.wedu.exam_creation.payment.infrastructure;

import com.wedu.exam_creation.payment.domain.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VnpayRefundGatewayTest {
    VnpayGateway gateway;
    MockRestServiceServer server;
    PaymentOrderEntity order = new PaymentOrderEntity();
    RefundEntity refund = new RefundEntity();
    @BeforeEach void setup() {
        VnpayProperties properties = new VnpayProperties(); properties.setEnabled(true);
        properties.setTmnCode("TESTCODE"); properties.setHashSecret("test-only"); properties.setReturnUrl("https://example.com/me/bill");
        RestClient.Builder builder = RestClient.builder(); server = MockRestServiceServer.bindTo(builder).build();
        gateway = new VnpayGateway(properties, builder.build());
        order.setId("a".repeat(64)); order.setTransactionNo("123"); order.setAmount(20000);
        order.setCreatedAt(Instant.parse("2026-11-09T00:00:00Z"));
        refund.setId("b".repeat(32)); refund.setRequestId("c".repeat(32)); refund.setAmount(20000);
        refund.setAdminId("admin"); refund.setSubmittedAt(Instant.parse("2026-11-09T01:00:00Z"));
    }
    Map<String, String> signed() {
        Map<String, String> result = new LinkedHashMap<>();
        String[] keys = {"vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo"};
        String[] values = {"response", "refund", "00", "OK", "TESTCODE", order.getId(), "2000000", "NCB", "20261109080000", "456", "02", "05", "Refund"};
        for (int i = 0; i < keys.length; i++) result.put(keys[i], values[i]);
        result.put("vnp_SecureHash", gateway.sign(String.join("|", values))); return result;
    }
    @Test void signsFullRefundWithPersistedAttemptAndOriginalCheckoutTime() throws Exception {
        var result = signed();
        server.expect(requestTo("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction")).andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = request.getBody().toString();
                    // Mock request output stream exposes the exact JSON bytes.
                    var json = new ObjectMapper().readTree(body);
                    assertThat(json.get("vnp_Amount").asText()).isEqualTo("2000000");
                    assertThat(json.get("vnp_TransactionType").asText()).isEqualTo("02");
                    assertThat(json.get("vnp_TransactionDate").asText()).isEqualTo("20261109070000");
                    assertThat(json.get("vnp_RequestId").asText()).isEqualTo(refund.getRequestId());
                    String[] keys = {"vnp_RequestId", "vnp_Version", "vnp_Command", "vnp_TmnCode", "vnp_TransactionType", "vnp_TxnRef", "vnp_Amount", "vnp_TransactionNo", "vnp_TransactionDate", "vnp_CreateBy", "vnp_CreateDate", "vnp_IpAddr", "vnp_OrderInfo"};
                    assertThat(json.get("vnp_SecureHash").asText()).isEqualTo(gateway.sign(String.join("|", Arrays.stream(keys).map(k -> json.get(k).asText()).toList())));
                }).andRespond(withSuccess(new ObjectMapper().writeValueAsString(result), MediaType.APPLICATION_JSON));
        assertThat(gateway.refund(order, refund)).containsEntry("vnp_TransactionStatus", "05"); server.verify();
    }
    @Test void rejectsTamperedSignatureAndDoesNotRetry() throws Exception {
        var result = signed(); result.put("vnp_Amount", "1");
        server.expect(requestTo("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"))
                .andRespond(withSuccess(new ObjectMapper().writeValueAsString(result), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.refund(order, refund)).isInstanceOf(VnpayQueryException.class); server.verify();
    }
    @Test void emptyProviderResponseCannotConfirmRefund() {
        server.expect(requestTo("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> gateway.refund(order, refund)).isInstanceOf(VnpayQueryException.class);
    }
}
