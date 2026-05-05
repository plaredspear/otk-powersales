package com.otoki.powersales.schedule.entity

/**
 * 출근 종류 (Spec #587)
 *
 * - REGULAR: 일반 거래처 출근 — `displayWorkScheduleId` 미존재
 * - DISPLAY: 진열 마스터 연계 출근 — `displayWorkScheduleId` 채워짐 (진열 마스터 기반 TMS 새 row INSERT)
 *
 * EVENT 값은 P2-B 에서 추가된다 (행사 분기).
 *
 * DB 컬럼에 CHECK 제약을 두지 않고 본 enum 으로만 검증한다 (spec.md §3.2).
 */
enum class AttendanceType {
    REGULAR,
    DISPLAY,
}
