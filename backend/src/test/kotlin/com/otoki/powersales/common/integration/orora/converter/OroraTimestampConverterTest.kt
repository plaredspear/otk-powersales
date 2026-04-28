package com.otoki.powersales.common.integration.orora.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("OroraTimestampConverter 테스트")
class OroraTimestampConverterTest {

    @Test
    @DisplayName("toUtcLocalDateTime(OffsetDateTime) - KST 입력 UTC 변환")
    fun kstToUtc() {
        val kst = OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9))
        assertThat(OroraTimestampConverter.toUtcLocalDateTime(kst))
            .isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
    }

    @Test
    @DisplayName("toUtcLocalDateTime(String) - ISO 문자열 파싱")
    fun isoStringToUtc() {
        assertThat(OroraTimestampConverter.toUtcLocalDateTime("2026-04-28T15:30:00+09:00"))
            .isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
    }

    @Test
    @DisplayName("toSeoulOffsetDateTime - UTC → KST")
    fun utcToSeoul() {
        val utc = LocalDateTime.of(2026, 4, 28, 6, 30)
        assertThat(OroraTimestampConverter.toSeoulOffsetDateTime(utc))
            .isEqualTo(OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9)))
    }

    @Test
    @DisplayName("toSeoulIsoString - UTC → KST ISO 문자열")
    fun utcToSeoulIso() {
        val utc = LocalDateTime.of(2026, 4, 28, 6, 30)
        assertThat(OroraTimestampConverter.toSeoulIsoString(utc))
            .isEqualTo("2026-04-28T15:30:00+09:00")
    }
}
