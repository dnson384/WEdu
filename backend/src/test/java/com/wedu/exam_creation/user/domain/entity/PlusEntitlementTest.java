package com.wedu.exam_creation.user.domain.entity;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlusEntitlementTest {
    final Instant now = Instant.parse("2026-11-03T00:00:00Z");

    @Test
    @DisplayName("trả về FREE tại thời điểm hết hạn và PLUS khi vẫn còn hiệu lực")
    void test_effectiveType_001() {
        assertThat(PlusEntitlement.effectiveType("PLUS", now, now)).isEqualTo("FREE");
        assertThat(PlusEntitlement.effectiveType("PLUS", now.plusSeconds(1), now))
                .isEqualTo("PLUS");
        assertThat(PlusEntitlement.effectiveType("PLUS", null, now)).isEqualTo("PLUS");
        assertThat(PlusEntitlement.effectiveType("FREE", now.plusSeconds(1), now))
                .isEqualTo("FREE");
    }

    @Test
    @DisplayName(
            "trả về hạn mới thêm 30 ngày khi tính từ thời điểm hiện tại hoặc hạn cũ còn hiệu lực")
    void test_extend_002() {
        assertThat(PlusEntitlement.extend(null, now, 30)).isEqualTo(now.plusSeconds(2592000));
        assertThat(PlusEntitlement.extend(now.minusSeconds(1), now, 30))
                .isEqualTo(now.plusSeconds(2592000));
        assertThat(PlusEntitlement.extend(now.plusSeconds(86400), now, 30))
                .isEqualTo(now.plusSeconds(2678400));
    }
}
