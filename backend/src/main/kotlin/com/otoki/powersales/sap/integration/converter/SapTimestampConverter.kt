package com.otoki.powersales.sap.integration.converter

import com.otoki.powersales.common.util.TimeZones
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * SAP API 와의 시각 입출력에서 사내 컨벤션(`LocalDateTime` UTC wall clock) 과
 * 외부 표현(보통 KST `OffsetDateTime` 또는 ISO 문자열) 을 잇는 변환 헬퍼 (스펙 #564 §4.5).
 *
 * Nullable 입력은 호출 측에서 `?.let { SapTimestampConverter.toUtcLocalDateTime(it) }`
 * 형태로 명시적으로 다룬다.
 */
object SapTimestampConverter {

    private val ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /**
     * SAP 외부에서 받은 `OffsetDateTime` 을 UTC wall clock `LocalDateTime` 으로 변환.
     */
    fun toUtcLocalDateTime(value: OffsetDateTime): LocalDateTime =
        value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()

    /**
     * SAP 외부에서 받은 ISO-8601 (offset 포함) 문자열을 UTC `LocalDateTime` 으로 변환.
     * 예: "2026-04-28T15:30:00+09:00" → 2026-04-28T06:30:00 (UTC).
     */
    fun toUtcLocalDateTime(isoOffsetText: String): LocalDateTime =
        toUtcLocalDateTime(OffsetDateTime.parse(isoOffsetText, ISO_OFFSET))

    /**
     * 사내 UTC `LocalDateTime` 을 SAP 가 기대하는 KST `OffsetDateTime` 으로 변환.
     */
    fun toSeoulOffsetDateTime(value: LocalDateTime): OffsetDateTime =
        ZonedDateTime.of(value, ZoneOffset.UTC)
            .withZoneSameInstant(TimeZones.SEOUL_ZONE)
            .toOffsetDateTime()

    /**
     * 사내 UTC `LocalDateTime` 을 SAP 가 기대하는 ISO-8601 KST 문자열로 변환.
     * 예: 2026-04-28T06:30:00 → "2026-04-28T15:30:00+09:00".
     */
    fun toSeoulIsoString(value: LocalDateTime): String =
        toSeoulOffsetDateTime(value).format(ISO_OFFSET)
}
