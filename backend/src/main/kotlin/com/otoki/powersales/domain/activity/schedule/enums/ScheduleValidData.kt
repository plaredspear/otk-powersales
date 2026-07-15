package com.otoki.powersales.domain.activity.schedule.enums

/**
 * 진열스케줄마스터 「유효여부」 조회 필터 — SF formula `ValidData__c` 3분류.
 *
 * 화면 「유효」 컬럼(신호등 dot)의 판정값과 1:1 대응한다:
 * - [VALID] 유효(GREEN), [PLANNED] 예정(YELLOW), [END] 종료(RED).
 *
 * 판정 로직은 사원 재직상태(status/appLoginActive/endDate) + 스케줄 시작·종료일 + TODAY 의 복합식이며,
 * dot 계산 (`ScheduleDisplayStatusCalculator.validData`) 과 동일하게 SQL 로 이관되어 있다
 * (`DisplayWorkScheduleRepositoryCustomImpl.buildValidDataCondition`).
 *
 * 기존 [SchedulePreset] 은 SF List View 10종(확정/근무유형 조합 포함) 매핑 전용이라 의미가 달라
 * 유효여부 단독 필터는 본 enum 으로 분리한다.
 */
enum class ScheduleValidData(val displayName: String) {
    /** 유효 — GREEN dot */
    VALID("유효"),

    /** 예정 — YELLOW dot */
    PLANNED("예정"),

    /** 종료 — RED dot */
    END("종료"),
    ;

    companion object {
        fun fromDisplayNameOrNull(value: String?): ScheduleValidData? =
            entries.firstOrNull { it.displayName == value }
    }
}
