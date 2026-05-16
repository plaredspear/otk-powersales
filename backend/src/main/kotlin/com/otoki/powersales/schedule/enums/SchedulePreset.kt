package com.otoki.powersales.schedule.enums

/**
 * 진열사원 스케줄 마스터 List View 프리셋 — 레거시 SF 10개 List View 매핑.
 *
 * 레거시 정의 (`force-app/main/default/objects/DisplayWorkScheduleMaster__c/listViews/`):
 * - `INPUT_TODAY` (0. 당일등록): CREATED_DATE = TODAY
 * - `ALL` (1. 모두): 필터 없음
 * - `VALID` (2-1. 유효사원): ValidData != '종료'
 * - `VALID_CONFIRMED` (2-2. 유효사원(확정)): ValidData = '유효' AND Confirmed = true
 * - `VALID_NOT_CONFIRMED` (2-3. 유효사원(미확정)): ValidData = '유효' AND Confirmed = false
 * - `FIXED_VALID` (3. 고정 유효사원): ValidData != '종료' AND TypeOfWork3 = '고정' AND Confirmed = true
 * - `BIFURCATION_VALID` (4. 격고 유효사원): ValidData != '종료' AND TypeOfWork3 = '격고' AND Confirmed = true
 * - `PATROL_VALID` (5. 순회 유효사원): ValidData != '종료' AND TypeOfWork3 = '순회' AND Confirmed = true
 * - `VALID_CONFIRMED_TEMP` (6. 유효사원(확정)_임시): ValidData = '유효' AND Confirmed = true AND TypeOfWork5 = '임시'
 * - `END` (7. 종료 사원): ValidData = '종료'
 *
 * ValidData formula 풀이 (신규 매핑):
 * - '유효' = 기존 `DisplayWorkScheduleRepositoryCustomImpl.validDataEqualsValid(date)` 동치
 *   (사원 재직 OR (사원 퇴직/비활성 AND 사원 endDate ≥ TODAY)) AND startDate ≤ TODAY AND (endDate IS NULL OR TODAY ≤ endDate)
 * - '종료' = 스케줄 endDate < TODAY (NOT NULL) — SF formula 의 '종료' 주 분기 단순화
 */
enum class SchedulePreset {
    /** 0. 당일등록 — 오늘 created_at */
    INPUT_TODAY,

    /** 1. 모두 — 필터 없음 */
    ALL,

    /** 2-1. 유효사원 — ValidData != '종료' */
    VALID,

    /** 2-2. 유효사원(확정) — ValidData = '유효' AND confirmed=true */
    VALID_CONFIRMED,

    /** 2-3. 유효사원(미확정) — ValidData = '유효' AND confirmed=false */
    VALID_NOT_CONFIRMED,

    /** 3. 고정 유효사원 — ValidData != '종료' AND typeOfWork3=고정 AND confirmed=true */
    FIXED_VALID,

    /** 4. 격고 유효사원 — ValidData != '종료' AND typeOfWork3=격고 AND confirmed=true */
    BIFURCATION_VALID,

    /** 5. 순회 유효사원 — ValidData != '종료' AND typeOfWork3=순회 AND confirmed=true */
    PATROL_VALID,

    /** 6. 유효사원(확정)_임시 — ValidData = '유효' AND confirmed=true AND typeOfWork5=임시 */
    VALID_CONFIRMED_TEMP,

    /** 7. 종료 사원 — ValidData = '종료' */
    END,
}
