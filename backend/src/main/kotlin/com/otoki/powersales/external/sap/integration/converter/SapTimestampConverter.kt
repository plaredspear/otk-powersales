package com.otoki.powersales.external.sap.integration.converter

import com.otoki.powersales.common.util.TimeZones
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * SAP API 와의 시각 입출력에서 사내 컨벤션(KST `LocalDateTime`) 과
 * 외부 표현(KST `OffsetDateTime` 또는 ISO 문자열) 을 잇는 변환 헬퍼.
 *
 * 사내 컨벤션 — JVM TZ = Asia/Seoul 환경에서 `LocalDateTime` 은 KST wall-clock 의미.
 * SAP 가 어떤 offset 으로 보내든 KST 로 정규화한 wall-clock 만 추출한다.
 *
 * Nullable 입력은 호출 측에서 `?.let { SapTimestampConverter.toLocalDateTime(it) }`
 * 형태로 명시적으로 다룬다.
 */
object SapTimestampConverter {

    private val ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /**
     * SAP 외부에서 받은 `OffsetDateTime` 을 KST `LocalDateTime` 으로 변환.
     */
    fun toLocalDateTime(value: OffsetDateTime): LocalDateTime =
        value.atZoneSameInstant(TimeZones.SEOUL_ZONE).toLocalDateTime()

    /**
     * SAP 외부에서 받은 ISO-8601 (offset 포함) 문자열을 KST `LocalDateTime` 으로 변환.
     * 예: "2026-04-28T15:30:00+09:00" → 2026-04-28T15:30:00 (KST wall-clock).
     */
    fun toLocalDateTime(isoOffsetText: String): LocalDateTime =
        toLocalDateTime(OffsetDateTime.parse(isoOffsetText, ISO_OFFSET))

    /**
     * 사내 KST `LocalDateTime` 을 SAP 가 기대하는 KST `OffsetDateTime` 으로 변환.
     */
    fun toSeoulOffsetDateTime(value: LocalDateTime): OffsetDateTime =
        value.atZone(TimeZones.SEOUL_ZONE).toOffsetDateTime()

    /**
     * 사내 KST `LocalDateTime` 을 SAP 가 기대하는 ISO-8601 KST 문자열로 변환.
     * 예: 2026-04-28T15:30:00 → "2026-04-28T15:30:00+09:00".
     */
    fun toSeoulIsoString(value: LocalDateTime): String =
        toSeoulOffsetDateTime(value).format(ISO_OFFSET)
}
