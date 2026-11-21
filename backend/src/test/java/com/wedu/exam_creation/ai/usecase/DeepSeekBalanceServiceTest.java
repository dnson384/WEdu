package com.wedu.exam_creation.ai.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BalanceDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DeepSeekBalanceServiceTest {
    @Test
    @DisplayName("trả về số dư gần nhất với cờ stale khi hết cache 60 giây và lần làm mới thất bại")
    void test_getBalance_001() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.invalid");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.invalid/user/balance"))
                .andRespond(
                        withSuccess(
                                """
                {"is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"12.34","granted_balance":"2","topped_up_balance":"10.34"}]}
                """,
                                MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://provider.invalid/user/balance"))
                .andRespond(withServerError());
        MutableClock clock = new MutableClock();
        DeepSeekBalanceService service =
                new DeepSeekBalanceService(builder.build(), new ObjectMapper(), clock);
        BalanceDTO first = service.getBalance();
        assertThat(first.isStale()).isFalse();
        assertThat(first.getBalances().get(0).getCurrency()).isEqualTo("CNY");
        clock.now = clock.now.plusSeconds(59);
        assertThat(service.getBalance().getUpdatedAt()).isEqualTo(first.getUpdatedAt());
        clock.now = clock.now.plusSeconds(1);
        BalanceDTO stale = service.getBalance();
        assertThat(stale.isStale()).isTrue();
        assertThat(stale.getUpdatedAt()).isEqualTo(first.getUpdatedAt());
        assertThat(stale.getBalances().get(0).getTotalBalance()).isEqualTo("12.34");
        service.getBalance(); // Failed polling is throttled too.
        server.verify();
    }

    @Test
    @DisplayName("trả về snapshot stale chưa biết số dư khi lần gọi đầu tiên thất bại")
    void test_getBalance_002() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.invalid");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.invalid/user/balance"))
                .andRespond(withServerError());
        DeepSeekBalanceService service =
                new DeepSeekBalanceService(builder.build(), new ObjectMapper(), new MutableClock());
        BalanceDTO snapshot = service.getBalance();
        assertThat(snapshot.isStale()).isTrue();
        assertThat(snapshot.getAvailable()).isNull();
        assertThat(snapshot.getUpdatedAt()).isNull();
        assertThat(snapshot.getBalances()).isEmpty();
        server.verify();
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
