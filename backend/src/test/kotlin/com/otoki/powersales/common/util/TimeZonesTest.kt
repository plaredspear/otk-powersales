package com.otoki.powersales.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZoneId

@DisplayName("TimeZones 상수 테스트")
class TimeZonesTest {

    @Test
    @DisplayName("SEOUL_ZONE - Asia/Seoul 식별자")
    fun seoulZone() {
        assertThat(TimeZones.SEOUL_ZONE).isEqualTo(ZoneId.of("Asia/Seoul"))
    }

    @Test
    @DisplayName("UTC_ZONE - UTC 식별자")
    fun utcZone() {
        assertThat(TimeZones.UTC_ZONE).isEqualTo(ZoneId.of("UTC"))
    }
}
