package com.otoki.powersales.external.sap.integration.converter

import com.otoki.powersales.external.sap.integration.converter.SapTimestampConverter
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
    @DisplayName("toLocalDateTime(OffsetDateTime) - 외부 OffsetDateTime → KST LocalDateTime")
    inner class ToLocalDateTimeFromOffset {

        @Test
        @DisplayName("KST(+09:00) 입력 - wall clock 그대로")
        fun kstPassthrough() {
            val kst = OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9))
            assertThat(SapTimestampConverter.toLocalDateTime(kst))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
        }

        @Test
        @DisplayName("UTC 입력 - KST wall clock 으로 변환 (+9h)")
        fun utcToKst() {
            val utc = OffsetDateTime.of(2026, 4, 28, 6, 30, 0, 0, ZoneOffset.UTC)
            assertThat(SapTimestampConverter.toLocalDateTime(utc))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
        }
    }

    @Nested
    @DisplayName("toLocalDateTime(String) - ISO offset 문자열 파싱")
    inner class ToLocalDateTimeFromString {

        @Test
        @DisplayName("KST ISO 문자열 - KST wall clock")
        fun kstIso() {
            val text = "2026-04-28T15:30:00+09:00"
            assertThat(SapTimestampConverter.toLocalDateTime(text))
                .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
        }
    }

    @Nested
    @DisplayName("toSeoulOffsetDateTime - KST LocalDateTime → KST OffsetDateTime")
    inner class ToSeoulOffset {

        @Test
        @DisplayName("KST 15:30 - +09:00 offset 부착")
        fun kstToOffset() {
            val kst = LocalDateTime.of(2026, 4, 28, 15, 30)
            assertThat(SapTimestampConverter.toSeoulOffsetDateTime(kst))
                .isEqualTo(OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9)))
        }
    }

    @Nested
    @DisplayName("toSeoulIsoString - KST LocalDateTime → KST ISO 문자열")
    inner class ToSeoulIsoString {

        @Test
        @DisplayName("KST 15:30 - '2026-04-28T15:30:00+09:00'")
        fun kstIso() {
            val kst = LocalDateTime.of(2026, 4, 28, 15, 30)
            assertThat(SapTimestampConverter.toSeoulIsoString(kst))
                .isEqualTo("2026-04-28T15:30:00+09:00")
        }
    }
}
