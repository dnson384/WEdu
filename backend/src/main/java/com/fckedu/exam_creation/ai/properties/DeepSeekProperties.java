package com.fckedu.exam_creation.ai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
@Data
public class DeepSeekProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private int timeoutSeconds;
}
