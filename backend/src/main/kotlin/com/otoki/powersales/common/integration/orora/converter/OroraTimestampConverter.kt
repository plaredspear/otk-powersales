package com.otoki.powersales.common.integration.orora.converter

import com.otoki.powersales.common.util.TimeZones
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Orora API 와의 시각 입출력에서 사내 컨벤션(`LocalDateTime` UTC wall clock) 과
 * 외부 표현(KST `OffsetDateTime` 또는 ISO 문자열) 을 잇는 변환 헬퍼 (스펙 #564 §4.5).
 *
 * 본 컨버터는 향후 Orora 통신 코드(WorkReport 등) 에서 시각 입출력이 필요해질 때 사용한다.
 * 현재 구현된 `OroraApiServiceMock` / `OroraWorkReportRequest` 는 시각 필드를 직접 다루지 않으므로
 * 본 컨버터는 신규 외부 시스템 코드에서 점진 적용되는 형태로 도입된다.
 *
 * Nullable 입력은 호출 측에서 `?.let { OroraTimestampConverter.toUtcLocalDateTime(it) }`
 * 형태로 명시적으로 다룬다.
 */
object OroraTimestampConverter {

    private val ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun toUtcLocalDateTime(value: OffsetDateTime): LocalDateTime =
        value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()

    fun toUtcLocalDateTime(isoOffsetText: String): LocalDateTime =
        toUtcLocalDateTime(OffsetDateTime.parse(isoOffsetText, ISO_OFFSET))

    fun toSeoulOffsetDateTime(value: LocalDateTime): OffsetDateTime =
        value.atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(TimeZones.SEOUL_ZONE)
            .toOffsetDateTime()

    fun toSeoulIsoString(value: LocalDateTime): String =
        toSeoulOffsetDateTime(value).format(ISO_OFFSET)
}
