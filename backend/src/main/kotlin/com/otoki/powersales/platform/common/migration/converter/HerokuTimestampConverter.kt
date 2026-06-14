package com.otoki.powersales.platform.common.migration.converter

import com.otoki.powersales.platform.common.util.TimeZones
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Heroku 레거시 DB(salesforce / salesforce2 스키마) 의 시각 컬럼을 사내 컨벤션
 * (KST `LocalDateTime`) 으로 받아주는 변환 헬퍼.
 *
 * 레거시 DB 는 `TIMESTAMP WITHOUT TIME ZONE` 컬럼에 UTC wall-clock 으로 저장한다
 * (Spec #549 시나리오 A 결론). JDBC 가 native 매핑한 결과는 UTC 의미라
 * 신규 시스템의 KST `LocalDateTime` 으로 정합하려면 +9h shift 가 필요하다.
 *
 * 외부에서 `OffsetDateTime` 을 받아오는 경우(ETL 도구 / API) 에는 offset 인식 후 KST 변환.
 *
 * Nullable 입력은 호출 측에서 `?.let { HerokuTimestampConverter.toLocalDateTime(it) }`
 * 형태로 명시적으로 다룬다.
 */
object HerokuTimestampConverter {

    /**
     * JDBC 가 UTC wall-clock 으로 매핑한 `LocalDateTime` 을 KST `LocalDateTime` 으로 변환.
     * UTC 시각 + 9h = KST 시각.
     */
    fun fromHerokuUtcWallClock(value: LocalDateTime): LocalDateTime =
        value.atOffset(ZoneOffset.UTC).atZoneSameInstant(TimeZones.SEOUL_ZONE).toLocalDateTime()

    /**
     * 외부에서 `OffsetDateTime` 으로 받은 값을 KST `LocalDateTime` 으로 변환.
     */
    fun toLocalDateTime(value: OffsetDateTime): LocalDateTime =
        value.atZoneSameInstant(TimeZones.SEOUL_ZONE).toLocalDateTime()

    /**
     * 사내 KST `LocalDateTime` 을 KST `OffsetDateTime` 으로 표시용 변환.
     * 외부 시스템에 "한국 시간" 을 요구하는 응답을 만들 때 사용.
     */
    fun toSeoulOffsetDateTime(value: LocalDateTime): OffsetDateTime =
        value.atZone(TimeZones.SEOUL_ZONE).toOffsetDateTime()
}
