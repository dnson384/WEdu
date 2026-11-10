package com.wedu.exam_creation.payment.usecase;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payment.refund")
public class RefundSettings {
    private boolean enabled;
    public static final String POLICY = "plus-refund-v1";
}
