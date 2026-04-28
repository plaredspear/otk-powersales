package com.otoki.powersales.common.migration.converter

import com.otoki.powersales.common.util.TimeZones
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Heroku 레거시 DB(salesforce / salesforce2 스키마) 의 시각 컬럼을 사내 컨벤션
 * (`LocalDateTime` UTC wall clock) 으로 받아주는 변환 헬퍼 (스펙 #564 §4.5).
 *
 * 레거시 DB 는 `TIMESTAMP WITHOUT TIME ZONE` 컬럼에 UTC wall clock 으로 저장한다
 * (Spec #549 시나리오 A 결론). 따라서 JDBC 가 매핑한 `LocalDateTime` 을 그대로
 * 받아도 절대 시점이 보존된다 — 본 헬퍼는 동작 변경 없이 의도(UTC 기준)를 명시한다.
 *
 * 외부에서 `OffsetDateTime` 을 받아오는 경우(ETL 도구 / API) 에만 실제 변환이 발생한다.
 *
 * Nullable 입력은 호출 측에서 `?.let { HerokuTimestampConverter.toUtcLocalDateTime(it) }`
 * 형태로 명시적으로 다룬다.
 */
object HerokuTimestampConverter {

    /**
     * JDBC 매핑된 `LocalDateTime` 을 그대로 반환한다 (UTC wall clock 가정).
     * 호출 측이 "이 값은 UTC 다" 를 코드로 표현하기 위한 no-op 식별 함수.
     */
    fun asUtcLocalDateTime(value: LocalDateTime): LocalDateTime = value

    /**
     * 외부에서 `OffsetDateTime` 으로 받은 값을 UTC wall clock `LocalDateTime` 으로 변환.
     * offset 이 어떤 값이든 UTC 로 정규화한 뒤 wall clock 만 추출한다.
     */
    fun toUtcLocalDateTime(value: OffsetDateTime): LocalDateTime =
        value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()

    /**
     * 사내 UTC `LocalDateTime` 을 KST `OffsetDateTime` 으로 표시용 변환.
     * 외부 시스템에 "한국 시간" 을 요구하는 응답을 만들 때 사용.
     */
    fun toSeoulOffsetDateTime(value: LocalDateTime): OffsetDateTime =
        value.atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(TimeZones.SEOUL_ZONE)
            .toOffsetDateTime()
}
