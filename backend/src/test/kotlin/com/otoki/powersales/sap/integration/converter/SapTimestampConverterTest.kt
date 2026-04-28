package com.otoki.powersales.sap.integration.converter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("SapTimestampConverter 테스트")
class SapTimestampConverterTest {

    @Nested
    @DisplayName("toUtcLocalDateTime(OffsetDateTime)")
    inner class ToUtcFromOffset {

        @Test
        @DisplayName("KST(+09:00) 입력 - UTC wall clock 으로 변환")
        fun kstToUtc() {
            val kst = OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9))
            assertThat(SapTimestampConverter.toUtcLocalDateTime(kst))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
        }

        @Test
        @DisplayName("UTC 입력 - wall clock 보존")
        fun utcPassthrough() {
            val utc = OffsetDateTime.of(2026, 4, 28, 6, 30, 0, 0, ZoneOffset.UTC)
            assertThat(SapTimestampConverter.toUtcLocalDateTime(utc))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
        }
    }

    @Nested
    @DisplayName("toUtcLocalDateTime(String) - ISO offset 파싱")
    inner class ToUtcFromString {

        @Test
        @DisplayName("KST ISO 문자열 - UTC wall clock 으로")
        fun isoKstToUtc() {
            val text = "2026-04-28T15:30:00+09:00"
            assertThat(SapTimestampConverter.toUtcLocalDateTime(text))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 6, 30))
        }
    }

    @Nested
    @DisplayName("toSeoulOffsetDateTime - UTC → KST")
    inner class ToSeoulOffset {

        @Test
        @DisplayName("UTC 06:30 - KST 15:30 (+09:00)")
        fun utcToSeoul() {
            val utc = LocalDateTime.of(2026, 4, 28, 6, 30)
            assertThat(SapTimestampConverter.toSeoulOffsetDateTime(utc))
                .isEqualTo(OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9)))
        }
    }

    @Nested
    @DisplayName("toSeoulIsoString - UTC → KST ISO 문자열")
    inner class ToSeoulIsoString {

        @Test
        @DisplayName("UTC 06:30 - '2026-04-28T15:30:00+09:00'")
        fun utcToSeoulIso() {
            val utc = LocalDateTime.of(2026, 4, 28, 6, 30)
            assertThat(SapTimestampConverter.toSeoulIsoString(utc))
                .isEqualTo("2026-04-28T15:30:00+09:00")
        }
    }
}
