package com.otoki.powersales.common.migration.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("HerokuTimestampConverter 테스트")
class HerokuTimestampConverterTest {

    @Nested
    @DisplayName("fromHerokuUtcWallClock - Heroku UTC wall-clock → KST LocalDateTime")
    inner class FromHerokuUtcWallClock {

        @Test
        @DisplayName("UTC 06:30 입력 - KST 15:30 으로 변환 (+9h)")
        fun utcToKst() {
            val utcWallClock = LocalDateTime.of(2026, 4, 28, 6, 30)
            assertThat(HerokuTimestampConverter.fromHerokuUtcWallClock(utcWallClock))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
        }
    }

    @Nested
    @DisplayName("toLocalDateTime(OffsetDateTime) - 외부 OffsetDateTime → KST LocalDateTime")
    inner class ToLocalDateTimeFromOffset {

        @Test
        @DisplayName("KST(+09:00) 입력 - wall clock 그대로")
        fun kstPassthrough() {
            val kst = OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9))
            assertThat(HerokuTimestampConverter.toLocalDateTime(kst))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
        }

        @Test
        @DisplayName("UTC 입력 - KST wall clock 으로 변환 (+9h)")
        fun utcToKst() {
            val utc = OffsetDateTime.of(2026, 4, 28, 6, 30, 0, 0, ZoneOffset.UTC)
            assertThat(HerokuTimestampConverter.toLocalDateTime(utc))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
        }

        @Test
        @DisplayName("+05:30 (인도) 입력 - KST wall clock 으로 정확 변환")
        fun indiaToKst() {
            val ist = OffsetDateTime.of(2026, 4, 28, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30))
            assertThat(HerokuTimestampConverter.toLocalDateTime(ist))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
        }
    }

    @Nested
    @DisplayName("toSeoulOffsetDateTime - KST LocalDateTime 을 KST OffsetDateTime 으로")
    inner class ToSeoulOffset {

        @Test
        @DisplayName("KST 15:30 - +09:00 offset 부착")
        fun kstToOffset() {
            val kst = LocalDateTime.of(2026, 4, 28, 15, 30)
            assertThat(HerokuTimestampConverter.toSeoulOffsetDateTime(kst))
                .isEqualTo(OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9)))
        }
    }
}
