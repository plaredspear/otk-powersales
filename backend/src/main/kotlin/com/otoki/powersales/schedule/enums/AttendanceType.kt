package com.otoki.powersales.schedule.enums

/**
 * 출근 종류 (Spec #587)
 *
 * - REGULAR: 일반 거래처 출근 — `displayWorkScheduleId` / `eventScheduleId` 둘 다 미존재
 * - DISPLAY: 진열 마스터 연계 출근 — `displayWorkScheduleId` 채워짐 (진열 마스터 기반 TMS 새 row INSERT)
 * - EVENT: 행사/팀 일정 연계 출근 — `eventScheduleId` 채워짐 (사전 배정된 TMS row UPDATE)
 *
 * DB 컬럼에 CHECK 제약을 두지 않고 본 enum 으로만 검증한다 (spec.md §3.2).
 */
enum class AttendanceType {
    REGULAR,
    DISPLAY,
    EVENT,
}
