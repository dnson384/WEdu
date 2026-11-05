package com.wedu.exam_creation.payment.infrastructure;

import com.wedu.exam_creation.payment.domain.entity.PaymentOrderEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class VnpayGateway {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final VnpayProperties properties;
    private final RestClient client;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VnpayGateway.class);

    @Autowired
    public VnpayGateway(VnpayProperties properties) {
        this(properties, buildClient());
    }

    VnpayGateway(VnpayProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    private static RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    public static String canonical(Map<String, String> values) {
        return new TreeMap<>(values).entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_") && !e.getKey().equals("vnp_SecureHash")
                        && !e.getKey().equals("vnp_SecureHashType") && e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String join(Map<String, String> values, String... keys) {
        return Arrays.stream(keys).map(k -> Objects.toString(values.get(k), "")).collect(Collectors.joining("|"));
    }

    public String paymentUrl(PaymentOrderEntity order) {
        Map<String, String> values = new TreeMap<>();
        values.put("vnp_Version", "2.1.0");
        values.put("vnp_Command", "pay");
        values.put("vnp_TmnCode", properties.getTmnCode());
        values.put("vnp_Amount", Long.toString(order.getAmount() * 100));
        values.put("vnp_CurrCode", "VND");
        values.put("vnp_TxnRef", order.getId());
        values.put("vnp_OrderInfo", "Nang cap WEdu PLUS " + order.getId());
        values.put("vnp_OrderType", "other");
        values.put("vnp_Locale", "vn");
        String bankCode = properties.getBankCode();
        if (bankCode != null && !bankCode.isBlank()) {
            values.put("vnp_BankCode", bankCode.trim());
        }
        values.put("vnp_ReturnUrl", properties.getReturnUrl());
        values.put("vnp_IpAddr", order.getClientIp());
        values.put("vnp_CreateDate", DATE.format(order.getCreatedAt()));
        values.put("vnp_ExpireDate", DATE.format(order.getExpiresAt()));
        String query = canonical(values);
        return properties.getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + sign(query);
    }

    public boolean validIpn(Map<String, String> values) {
        return properties.available() && validSignature(canonical(values), values.get("vnp_SecureHash"));
    }

    public boolean validQuery(Map<String, String> values) {
        return properties.available() && validSignature(join(values, "vnp_ResponseId", "vnp_Command", "vnp_ResponseCode",
                "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate",
                "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo",
                "vnp_PromotionCode", "vnp_PromotionAmount"), values.get("vnp_SecureHash"));
    }

    public Map<String, String> query(PaymentOrderEntity order) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
        values.put("vnp_Version", "2.1.0");
        values.put("vnp_Command", "querydr");
        values.put("vnp_TmnCode", properties.getTmnCode());
        values.put("vnp_TxnRef", order.getId());
        values.put("vnp_TransactionDate", DATE.format(order.getCreatedAt()));
        values.put("vnp_CreateDate", DATE.format(Instant.now()));
        values.put("vnp_IpAddr", properties.getServerIp());
        values.put("vnp_OrderInfo", "Kiem tra WEdu " + order.getId());
        values.put("vnp_SecureHash", sign(join(values, "vnp_RequestId", "vnp_Version", "vnp_Command",
                "vnp_TmnCode", "vnp_TxnRef", "vnp_TransactionDate", "vnp_CreateDate", "vnp_IpAddr", "vnp_OrderInfo")));
        Map<String, String> result;
        try {
            result = client.post().uri(properties.getQueryUrl()).contentType(MediaType.APPLICATION_JSON)
                    .body(values).retrieve().body(new ParameterizedTypeReference<Map<String, String>>() { });
        } catch (RestClientResponseException e) {
            throw queryFailure("HTTP_ERROR", "UNKNOWN", e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw queryFailure("TRANSPORT_ERROR", "UNKNOWN", null);
        } catch (RestClientException e) {
            throw queryFailure("RESPONSE_FORMAT", "UNKNOWN", null);
        }
        if (result == null) throw queryFailure("EMPTY_RESPONSE", "UNKNOWN", null);
        String providerCode = safeCode(result.get("vnp_ResponseCode"));
        // An unsigned error code is diagnostic metadata only, never a payment result.
        if (!validQuery(result)) throw queryFailure("UNVERIFIED_RESPONSE", providerCode, null);
        LOG.debug("VNPay query verified: providerCode={} transactionStatus={}",
                providerCode, safeCode(result.get("vnp_TransactionStatus")));
        return result;
    }

    private VnpayQueryException queryFailure(String stage, String providerCode, Integer httpStatus) {
        LOG.warn("VNPay query failed: stage={} providerCode={} httpStatus={}", stage, providerCode, httpStatus);
        return new VnpayQueryException(stage, providerCode, httpStatus);
    }

    private static String safeCode(String value) {
        return value != null && value.matches("[0-9]{2}") ? value : "UNKNOWN";
    }

    public String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign payment");
        }
    }

    private boolean validSignature(String data, String signature) {
        if (signature == null || !signature.matches("[a-fA-F0-9]{128}")) return false;
        return MessageDigest.isEqual(HexFormat.of().parseHex(sign(data)), HexFormat.of().parseHex(signature));
    }
}
