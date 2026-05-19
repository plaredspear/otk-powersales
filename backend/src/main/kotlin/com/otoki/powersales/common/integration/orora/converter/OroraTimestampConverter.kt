package com.otoki.powersales.common.integration.orora.converter

import com.otoki.powersales.common.util.TimeZones
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Orora API 와의 시각 입출력에서 사내 컨벤션(KST `LocalDateTime`) 과
 * 외부 표현(KST `OffsetDateTime` 또는 ISO 문자열) 을 잇는 변환 헬퍼.
 *
 * 사내 컨벤션 — JVM TZ = Asia/Seoul 환경에서 `LocalDateTime` 은 KST wall-clock 의미.
 * 본 컨버터는 향후 Orora 통신 코드(WorkReport 등) 에서 시각 입출력이 필요해질 때 사용한다.
 *
 * Nullable 입력은 호출 측에서 `?.let { OroraTimestampConverter.toLocalDateTime(it) }`
 * 형태로 명시적으로 다룬다.
 */
object OroraTimestampConverter {

    private val ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun toLocalDateTime(value: OffsetDateTime): LocalDateTime =
        value.atZoneSameInstant(TimeZones.SEOUL_ZONE).toLocalDateTime()

    fun toLocalDateTime(isoOffsetText: String): LocalDateTime =
        toLocalDateTime(OffsetDateTime.parse(isoOffsetText, ISO_OFFSET))

    fun toSeoulOffsetDateTime(value: LocalDateTime): OffsetDateTime =
        value.atZone(TimeZones.SEOUL_ZONE).toOffsetDateTime()

    fun toSeoulIsoString(value: LocalDateTime): String =
        toSeoulOffsetDateTime(value).format(ISO_OFFSET)
}
