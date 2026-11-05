package com.wedu.exam_creation.payment.infrastructure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Data
@ConfigurationProperties(prefix = "payment.vnpay")
public class VnpayProperties {
    private boolean enabled;
    private String tmnCode = "";
    private String hashSecret = "";
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String queryUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private String returnUrl = "";
    private String bankCode = "";
    private String serverIp = "127.0.0.1";

    public boolean available() {
        return enabled &&
                !tmnCode.isBlank() &&
                !hashSecret.isBlank() &&
                validUrl(paymentUrl) &&
                validUrl(queryUrl) &&
                validUrl(returnUrl);
    }

    private boolean validUrl(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equals(uri.getScheme()) &&
                    uri.getHost() != null &&
                    uri.getRawQuery() == null &&
                    uri.getFragment() == null &&
                    uri.getUserInfo() == null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
