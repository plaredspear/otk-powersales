package com.otoki.powersales.domain.activity.promotion.service.internal

import java.time.LocalDate

/**
 * 전문행사조 마스터 「유효」 (SF `ValidData__c` formula) 계산 — 미확정 / 유효 / 예정 / 종료 4분류.
 *
 * SF 에서는 calculated formula 라 DB 컬럼이 없고 조회 때마다 평가된다. 신규 시스템도 확정여부 +
 * 시작·종료일 + TODAY 로 조회 시점에 계산한다 (진열스케줄의
 * [com.otoki.powersales.domain.activity.schedule.service.internal.ScheduleDisplayStatusCalculator] 와 동일 성격).
 *
 * 원본 `ValidData__c.field-meta.xml`:
 * ```
 * IF(Confirmed__c == false, "미확정",
 *   IF(StartDate <= TODAY AND (EndDate >= TODAY OR EndDate IS NULL), "유효",
 *     IF(StartDate >  TODAY AND (EndDate >= TODAY OR EndDate IS NULL), "예정", "종료")))
 * ```
 *
 * SAP 송신 페이로드([com.otoki.powersales.domain.activity.promotion.sap.PPTMasterPayloadFactory] 의
 * `ValidData`) 와 목록 엑셀의 「유효」 컬럼이 **같은 계산**을 쓴다 — 화면의 신호등 dot 판정
 * (web `PPTMasterPage.getValidStatus`) 과도 동일 분기다.
 */
object PPTMasterValidDataCalculator {

    fun of(isConfirmed: Boolean, startDate: LocalDate, endDate: LocalDate?, today: LocalDate): String {
        if (!isConfirmed) return "미확정"
        // 종료일이 없거나 오늘 이후 = 아직 끝나지 않음
        val notEnded = endDate == null || !endDate.isBefore(today)
        return when {
            !startDate.isAfter(today) && notEnded -> "유효"
            startDate.isAfter(today) && notEnded -> "예정"
            else -> "종료"
        }
    }
}
