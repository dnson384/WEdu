package com.wedu.exam_creation.ai.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.dto.AiManagementDTO;
import com.wedu.exam_creation.ai.properties.AiPricingProperties;
import com.wedu.exam_creation.common.exception.AiAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiUsageService {
    private final IAiUsageRepository aiUsageRepository;
    private final AiManagementUsecase management;
    private final AiPricingProperties pricing;

    public AiUsageEntity begin(String model) {
        try {
            AiManagementDTO.StatusDTO status = management.status();
            if (!status.getReasons().isEmpty()) {
                String code = status.getReasons().get(0);

                String message = switch (code) {
                    case "AI_BUDGET_EXCEEDED" -> "Đã hết budget AI tháng này. Vui lòng liên hệ quản trị viên.";
                    case "AI_TOKEN_QUOTA_EXCEEDED" ->
                            "Đã hết hạn mức token AI tháng này. Vui lòng liên hệ quản trị viên.";
                    default -> "Tài khoản DeepSeek không đủ số dư. Vui lòng liên hệ quản trị viên.";
                };

                throw new AiAccessException(code, 429, message);
            }

            AiPricingProperties.ModelPrice price = pricing.getModels().get(model);
            if (price == null ||
                    pricing.getVersion() == null ||
                    !valid(price.getInputCacheHit()) ||
                    !valid(price.getInputCacheMiss()) ||
                    !valid(price.getOutput())
            ) {
                throw new AiAccessException("AI_PRICING_UNAVAILABLE", 503, "Chưa cấu hình bảng giá cho model AI.");
            }

            AiUsageEntity usage = new AiUsageEntity();
            usage.setId(UUID.randomUUID().toString());
            usage.setCreatedAt(Instant.now());
            usage.setModel(model);
            usage.setOutcome("STARTED");
            usage.setPricingVersion(pricing.getVersion());
            usage.setInputCacheHitPrice(price.getInputCacheHit());
            usage.setInputCacheMissPrice(price.getInputCacheMiss());
            usage.setOutputPrice(price.getOutput());

            aiUsageRepository.saveUsage(usage);
            return usage;
        } catch (AiAccessException known) {
            throw known;
        } catch (Exception unavailable) {
            throw new AiAccessException("AI_TRACKING_UNAVAILABLE", 503, "Chưa thể kiểm tra hạn mức AI. Vui lòng thử lại sau.");
        }
    }

    public void complete(AiUsageEntity usage, JsonNode root) {
        usage.setOutcome(root == null ? "FAILED" : "RECEIVED");

        JsonNode tokens = root == null ? null : root.get("usage");
        if (tokens != null &&
                count(tokens.get("prompt_tokens")) &&
                count(tokens.get("completion_tokens")) &&
                count(tokens.get("total_tokens"))
        ) {
            long input = tokens.get("prompt_tokens").longValue();
            long output = tokens.get("completion_tokens").longValue();
            JsonNode hitNode = tokens.get("prompt_cache_hit_tokens");
            long hit = hitNode == null ? 0 : count(hitNode) ? hitNode.longValue() : -1;
            long total = tokens.get("total_tokens").longValue();

            if (hit >= 0 && hit <= input && input <= Long.MAX_VALUE - output && total == input + output) {
                usage.setInputTokens(input);
                usage.setOutputTokens(output);
                usage.setTotalTokens(total);
                usage.setCacheHitTokens(hit);
                usage.setUsageKnown(true);

                BigDecimal cost = BigDecimal.valueOf(hit).multiply(usage.getInputCacheHitPrice())
                        .add(BigDecimal.valueOf(input - hit)
                                .multiply(usage.getInputCacheMissPrice()))
                        .add(BigDecimal.valueOf(output)
                                .multiply(usage.getOutputPrice()))
                        .movePointLeft(6);
                usage.setEstimatedCostUsd(cost);
            }
        }

        try {
            aiUsageRepository.saveUsage(usage);
        } catch (Exception unavailable) {
            log.warn("Không thể hoàn tất ghi nhận usage AI; cần đối soát bản ghi STARTED");
        }
    }

    private boolean count(JsonNode value) {
        return value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0;
    }

    private boolean valid(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }
}
