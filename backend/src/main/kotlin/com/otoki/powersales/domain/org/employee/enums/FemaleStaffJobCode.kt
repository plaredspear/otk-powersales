package com.otoki.powersales.domain.org.employee.enums

/**
 * Employee.jobCode 중 현장 여사원 직무의 표준 값 (SF `DKRetail__JobCode__c`).
 *
 * SF prod 메타 `DKRetail__JobCode__c` 는 `type=string`, `picklistValues=[]` (free-form string, length 40)
 * 이므로 강제 변환은 적용하지 않으며 [com.otoki.powersales.domain.org.employee.entity.Employee.jobCode]
 * 필드 타입은 `String?` 그대로 유지한다. 본 enum 은 Repository 필터 / 집계 분기의 [code] 인용으로만 사용한다.
 *
 * ## 레이디직 = OSC직 구 명칭
 *
 * SAP 발령 직무코드 `A053`(레이디직) 이 2024-01-02 `A055`(OSC직) 으로 개명됐으나, 개명 이전 적재분이
 * `레이디직` 문자열 그대로 남아 있어 레거시는 **판촉직 / 레이디직 / OSC직 3값 OR** 를 관용구로 유지한다
 * (`EmployeeTriggerHandler.cls:47`, `AttendInfoTriggerHandler.cls:74-76` — 후자 주석
 * "20240102 기존 직위명 (레이디직->OSC)직으로 변경", `PostponedAppointmentBatch.cls:116`).
 * 따라서 조회 / 집계에서 `OSC직` 을 다룰 때는 항상 [OSC_CODES] 로 레이디직을 함께 포함해야 한다.
 *
 * 직군 판정 축은 `jobCode` 단일이다 — `jikwee`(직위명) / `jikjong`(직종명) 은 레거시가 직군 판정에
 * 사용한 적이 없다.
 */
enum class FemaleStaffJobCode(val code: String) {
    PROMOTION("판촉직"),

    OSC("OSC직"),

    /** 구 OSC (SAP A053, 2024-01-02 개명 이전 적재분). 조회·집계 시 [OSC] 로 합산한다. */
    LADY("레이디직"),
    ;

    companion object {
        /** 현장 여사원 직무 전체 — 레거시 3값 OR 관용구 정합. */
        val ALL_CODES: Set<String> = entries.map { it.code }.toSet()

        /** OSC직 조회용 — 구 명칭 `레이디직` 을 함께 포함한다. */
        val OSC_CODES: Set<String> = setOf(OSC.code, LADY.code)

        /**
         * 화면 표시용 정규화 — 구 명칭 `레이디직` 을 `OSC직` 으로 흡수한다.
         * 여사원 직무 3값 외/null 은 정규화하지 않고 null 을 반환한다(호출부가 원본값 fallback 을 결정).
         */
        fun normalizeOrNull(jobCode: String?): String? =
            when (jobCode?.trim()) {
                PROMOTION.code -> PROMOTION.code
                OSC.code, LADY.code -> OSC.code
                else -> null
            }

        /** 필터 요청값(화면 Select value) → 매칭 대상 jobCode 집합. 유효하지 않은 값이면 null. */
        fun matchingCodesOrNull(displayValue: String?): Set<String>? =
            when (displayValue?.trim()) {
                PROMOTION.code -> setOf(PROMOTION.code)
                OSC.code -> OSC_CODES
                else -> null
            }
    }
}
