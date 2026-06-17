package com.otoki.powersales.platform.batch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.DayOfWeek

/**
 * J마트(이동매장) 요일별 좌표 보정 정책.
 *
 * 레거시 `Batch_JMartLatLong` 동등 — 특정 이동매장(거래처)이 요일에 따라 물리적 위치를 옮기므로
 * 주소 기반 지오코딩으로는 좌표를 산출할 수 없다. 해당 거래처(`externalKey`)의 좌표를 요일별로 덮어쓴다.
 *
 * 기본값은 레거시 하드코딩 값(`ExternalKey__c='1015773'`, 수=양구점 / 금=원통점)이며,
 * `app.batch.jmart-coordinate.*` 로 재정의할 수 있다. 매칭되는 요일이 없으면 아무 것도 하지 않는다(레거시 동등).
 */
@ConfigurationProperties(prefix = "app.batch.jmart-coordinate")
data class JMartCoordinateProperties(
    /** 대상 이동매장 거래처 외부키 (`account.external_key`). */
    val externalKey: String = "1015773",
    /** 요일별 좌표 스케줄. */
    val schedules: List<DaySchedule> = listOf(
        DaySchedule(DayOfWeek.WEDNESDAY, "38.101772", "127.988819", "양구점"),
        DaySchedule(DayOfWeek.FRIDAY, "38.121391", "128.208204", "원통점"),
    ),
) {
    data class DaySchedule(
        val dayOfWeek: DayOfWeek,
        val latitude: String,
        val longitude: String,
        val label: String = "",
    )

    fun scheduleFor(day: DayOfWeek): DaySchedule? = schedules.firstOrNull { it.dayOfWeek == day }
}
