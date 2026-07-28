package com.otoki.powersales.domain.activity.schedule.enums

/**
 * 진열스케줄마스터 「재직상태」 조회 필터 — SF formula `ValidConditionData__c` 4분류 중 **3종만** 노출.
 *
 * 화면/엑셀의 「재직상태」 컬럼 표시값과 대응한다. SF 원본 formula 는 퇴직/퇴직예정에
 * 사원 종료일을 덧붙여 "퇴직2026-01-15" 형태로 표기하므로, 필터는 날짜 suffix 를 제외한
 * 앞부분(분류)만 값으로 사용한다.
 *
 * **퇴직예정은 필터에서 제외** — 조회 축을 사원 상태 3분류(재직/휴직/퇴직)로 맞춘 운영 요청.
 * 표시값 계산 (`ScheduleDisplayStatusCalculator.employmentStatus`) 은 formula 정합을 위해
 * "퇴직예정2026-08-15" 을 그대로 유지하므로, 해당 행은 컬럼에는 보이되 어느 필터로도 잡히지 않는다
 * ([RESIGNED] 는 종료일 과거 조건이라 퇴직예정을 포함하지 않는다).
 *
 * SF 원본 (`DisplayWorkScheduleMaster__c.ValidConditionData__c`):
 * ```
 * IF(AND(OR(Status__c="퇴직", APPLoginActive__c=false), EndDate__c < TODAY()), "퇴직"&TEXT(EndDate__c),
 * IF(AND(OR(Status__c="퇴직", APPLoginActive__c=false), EndDate__c > TODAY()), "퇴직예정"&TEXT(EndDate__c),
 * IF(Status__c="휴직", "휴직",
 * "재직")))
 * ```
 *
 * 사원 원본 컬럼 `employee.status` (재직/휴직/퇴직 3값) 와는 **다른 축** 이다 —
 * status='재직' 이어도 appLoginActive=false 면 [RESIGNED](또는 표시값 "퇴직예정") 로 판정된다.
 * 따라서 단순 status 매칭이 아니라 계산식을 SQL 로 이관해 필터링한다
 * (`DisplayWorkScheduleRepositoryCustomImpl.buildEmploymentStatusCondition`).
 *
 * 판정 로직은 화면 표시값 계산 (`ScheduleDisplayStatusCalculator.employmentStatus`) 과 동일하되,
 * 여기에 발령명 '면직' 보정 ([com.otoki.powersales.domain.org.employee.enums.DismissalPolicy]) 이
 * 얹힌다 — 여사원 현황 상태 조회와 동일 축으로, 퇴직 조회는 면직 포함 / 재직·휴직 조회는 면직 제외.
 */
enum class ScheduleEmploymentStatus(val displayName: String) {
    /** 재직 — formula 최종 fallthrough (단, 발령명 '면직' 은 제외) */
    ACTIVE("재직"),

    /** 휴직 — status='휴직' (퇴직/퇴직예정 판정에 걸리지 않은 경우, 발령명 '면직' 은 제외) */
    ON_LEAVE("휴직"),

    /** 퇴직 — ((status='퇴직' OR appLoginActive=false) AND 사원 종료일 < TODAY) OR 발령명 '면직' */
    RESIGNED("퇴직"),
    ;

    companion object {
        fun fromDisplayNameOrNull(value: String?): ScheduleEmploymentStatus? =
            entries.firstOrNull { it.displayName == value }
    }
}
