package com.wedu.exam_creation.payment;

import com.wedu.exam_creation.user.domain.entity.PlusEntitlement;
import com.wedu.exam_creation.user.domain.entity.UserEntity;
import com.wedu.exam_creation.user.dto.mapper.UserDTOMapper;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class PlusEntitlementTest {
    final Instant now = Instant.parse("2026-11-03T00:00:00Z");
    @Test void expiresExactlyAtBoundaryAndPreservesLegacyPlus() {
        assertThat(PlusEntitlement.effectiveType("PLUS", now, now)).isEqualTo("FREE");
        assertThat(PlusEntitlement.effectiveType("PLUS", now.plusSeconds(1), now)).isEqualTo("PLUS");
        assertThat(PlusEntitlement.effectiveType("PLUS", null, now)).isEqualTo("PLUS");
        assertThat(PlusEntitlement.effectiveType("FREE", now.plusSeconds(1), now)).isEqualTo("FREE");
    }
    @Test void renewalAddsThirtyDaysFromLatestExpiryOrNow() {
        assertThat(PlusEntitlement.extend(null, now, 30)).isEqualTo(now.plusSeconds(2592000));
        assertThat(PlusEntitlement.extend(now.minusSeconds(1), now, 30)).isEqualTo(now.plusSeconds(2592000));
        assertThat(PlusEntitlement.extend(now.plusSeconds(86400), now, 30)).isEqualTo(now.plusSeconds(2678400));
    }
    @Test void authAndProfileDtoExposeEffectiveTypeAndExpiry() {
        UserEntity user = new UserEntity();
        user.setAccountType("PLUS");
        user.setPlusExpiresAt(Instant.EPOCH);
        UserDTOMapper mapper = Mappers.getMapper(UserDTOMapper.class);
        assertThat(mapper.toCommonAllDTO(user).getAccountType()).isEqualTo("FREE");
        assertThat(mapper.toCommonDTO(user).getAccountType()).isEqualTo("FREE");
        assertThat(mapper.toCommonDTO(user).getPlusExpiresAt()).isEqualTo(Instant.EPOCH);
        assertThat(user.getAccountType()).isEqualTo("PLUS");
    }
}
