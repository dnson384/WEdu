package com.wedu.exam_creation.ai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.pricing")
public class AiPricingProperties {
    private String version;
    private Map<String, ModelPrice> models = new HashMap<>();

    @Data
    public static class ModelPrice {
        private BigDecimal inputCacheHit;
        private BigDecimal inputCacheMiss;
        private BigDecimal output;
    }
}
