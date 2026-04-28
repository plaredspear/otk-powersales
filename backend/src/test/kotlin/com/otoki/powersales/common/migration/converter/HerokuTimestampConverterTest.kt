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
    @DisplayName("asUtcLocalDateTime - JDBC 매핑 LocalDateTime 식별 함수")
    inner class AsUtcLocalDateTime {

        @Test
        @DisplayName("LocalDateTime 입력 - 동일 값 반환")
        fun identity() {
            val input = LocalDateTime.of(2026, 4, 28, 15, 30)
            assertThat(HerokuTimestampConverter.asUtcLocalDateTime(input)).isEqualTo(input)
        }
    }

    @Nested
    @DisplayName("toUtcLocalDateTime(OffsetDateTime) - UTC 정규화")
    inner class ToUtcFromOffset {

        @Test
        @DisplayName("KST(+09:00) 입력 - UTC wall clock 으로 변환 (-9시간)")
        fun kstToUtc() {
            val kst = OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9))
            val result = HerokuTimestampConverter.toUtcLocalDateTime(kst)
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
        }

        @Test
        @DisplayName("UTC 입력 - wall clock 그대로")
        fun utcPassthrough() {
            val utc = OffsetDateTime.of(2026, 4, 28, 6, 30, 0, 0, ZoneOffset.UTC)
            assertThat(HerokuTimestampConverter.toUtcLocalDateTime(utc))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
        }

        @Test
        @DisplayName("+05:30 (인도) 입력 - UTC wall clock 으로 정확 변환")
        fun indiaToUtc() {
            val ist = OffsetDateTime.of(2026, 4, 28, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30))
            val result = HerokuTimestampConverter.toUtcLocalDateTime(ist)
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
        }
    }

    @Nested
    @DisplayName("toSeoulOffsetDateTime - UTC LocalDateTime 을 KST OffsetDateTime 으로")
    inner class ToSeoulOffset {

        @Test
        @DisplayName("UTC 06:30 - KST 15:30 (+09:00)")
        fun utcToSeoul() {
            val utc = LocalDateTime.of(2026, 4, 28, 6, 30)
            val result = HerokuTimestampConverter.toSeoulOffsetDateTime(utc)
            assertThat(result).isEqualTo(
                OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9))
            )
        }
    }
}
