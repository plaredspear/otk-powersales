package com.otoki.powersales.domain.activity.schedule.enums

/**
 * 진열스케줄마스터 「재직상태」 조회 필터 — SF formula `ValidConditionData__c` 4분류.
 *
 * 화면/엑셀의 「재직상태」 컬럼 표시값과 1:1 대응한다. SF 원본 formula 는 퇴직/퇴직예정에
 * 사원 종료일을 덧붙여 "퇴직2026-01-15" 형태로 표기하므로, 필터는 날짜 suffix 를 제외한
 * 앞부분(분류)만 값으로 사용한다.
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
 * status='재직' 이어도 appLoginActive=false 면 [RESIGNED]/[RESIGN_PLANNED] 로 판정된다.
 * 따라서 단순 status 매칭이 아니라 계산식을 SQL 로 이관해 필터링한다
 * (`DisplayWorkScheduleRepositoryCustomImpl.buildEmploymentStatusCondition`).
 *
 * 판정 로직은 화면 표시값 계산 (`ScheduleDisplayStatusCalculator.employmentStatus`) 과 동일하다.
 */
enum class ScheduleEmploymentStatus(val displayName: String) {
    /** 재직 — formula 최종 fallthrough */
    ACTIVE("재직"),

    /** 휴직 — status='휴직' (퇴직/퇴직예정 판정에 걸리지 않은 경우) */
    ON_LEAVE("휴직"),

    /** 퇴직 — (status='퇴직' OR appLoginActive=false) AND 사원 종료일 < TODAY */
    RESIGNED("퇴직"),

    /** 퇴직예정 — (status='퇴직' OR appLoginActive=false) AND 사원 종료일 > TODAY */
    RESIGN_PLANNED("퇴직예정"),
    ;

    companion object {
        fun fromDisplayNameOrNull(value: String?): ScheduleEmploymentStatus? =
            entries.firstOrNull { it.displayName == value }
    }
}
