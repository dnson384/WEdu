package com.wedu.exam_creation.ai.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BalanceDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BalanceInfoDTO;
import com.wedu.exam_creation.ai.properties.DeepSeekProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekBalanceService {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private BalanceDTO cached = new BalanceDTO();
    private Instant lastAttempt;

    @Autowired
    public DeepSeekBalanceService(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this(buildClient(properties), objectMapper, Clock.systemUTC());
    }

    DeepSeekBalanceService(RestClient client, ObjectMapper objectMapper, Clock clock) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.clock = clock;
        cached.setStale(true);
    }

    private static RestClient buildClient(DeepSeekProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        return RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey()).build();
    }

    public synchronized BalanceDTO getBalance() {
        Instant now = clock.instant();

        if (lastAttempt != null && now.isBefore(lastAttempt.plusSeconds(60))) {
            return copy(cached);
        }
        lastAttempt = now;

        try {
            JsonNode root = objectMapper.readTree(client.get().uri("/user/balance").retrieve().body(String.class));
            if (!root.path("is_available").isBoolean() ||
                    !root.path("balance_infos").isArray()
            ) {
                throw new IllegalArgumentException("Invalid balance response");
            }

            List<BalanceInfoDTO> balances = new ArrayList<>();
            for (JsonNode item : root.path("balance_infos")) {
                String currency = item.path("currency").asText();
                if (!currency.equals("USD") && !currency.equals("CNY")) {
                    throw new IllegalArgumentException("Invalid currency");
                }

                BalanceInfoDTO info = new BalanceInfoDTO();
                info.setCurrency(currency);
                info.setTotalBalance(new BigDecimal(item.path("total_balance").asText()).toPlainString());
                info.setGrantedBalance(new BigDecimal(item.path("granted_balance").asText()).toPlainString());
                info.setToppedUpBalance(new BigDecimal(item.path("topped_up_balance").asText()).toPlainString());

                balances.add(info);
            }
            BalanceDTO value = new BalanceDTO();
            value.setAvailable(root.path("is_available").asBoolean());
            value.setBalances(balances);
            value.setUpdatedAt(now);

            cached = value;
        } catch (Exception failure) {
            cached.setStale(true);
        }

        return copy(cached);
    }

    private BalanceDTO copy(BalanceDTO source) {
        BalanceDTO value = new BalanceDTO();
        value.setAvailable(source.getAvailable());
        value.setUpdatedAt(source.getUpdatedAt());
        value.setStale(source.isStale());
        value.setBalances(source.getBalances());

        return value;
    }
}
