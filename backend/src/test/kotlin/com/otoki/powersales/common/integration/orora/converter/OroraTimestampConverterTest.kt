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
    @DisplayName("toLocalDateTime(OffsetDateTime) - KST 입력 wall clock 그대로")
    fun kstPassthrough() {
        val kst = OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9))
        assertThat(OroraTimestampConverter.toLocalDateTime(kst))
            .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
    }

    @Test
    @DisplayName("toLocalDateTime(String) - ISO 문자열 파싱")
    fun isoString() {
        assertThat(OroraTimestampConverter.toLocalDateTime("2026-04-28T15:30:00+09:00"))
            .isEqualTo(LocalDateTime.of(2026, 4, 28, 15, 30))
    }

    @Test
    @DisplayName("toSeoulOffsetDateTime - KST LocalDateTime → +09:00 offset")
    fun toSeoulOffset() {
        val kst = LocalDateTime.of(2026, 4, 28, 15, 30)
        assertThat(OroraTimestampConverter.toSeoulOffsetDateTime(kst))
            .isEqualTo(OffsetDateTime.of(2026, 4, 28, 15, 30, 0, 0, ZoneOffset.ofHours(9)))
    }

    @Test
    @DisplayName("toSeoulIsoString - KST LocalDateTime → ISO 문자열")
    fun toSeoulIso() {
        val kst = LocalDateTime.of(2026, 4, 28, 15, 30)
        assertThat(OroraTimestampConverter.toSeoulIsoString(kst))
            .isEqualTo("2026-04-28T15:30:00+09:00")
    }
}
