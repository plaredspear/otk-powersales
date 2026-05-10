package com.otoki.powersales.schedule.service.dto

/**
 * 출근 정보 INSERT 실패 행.
 *
 * - [identifier] : 식별자 (employeeCode 또는 employeeCode+date. 필수 누락 시 null)
 * - [reason] : 실패 사유 (예: `"EmployeeCode 필수"`, `"StartDate YYYYMMDD 형식 오류: 2026-04-27"`)
 */
data class AttendInfoInsertFailedRow(
    val identifier: String?,
    val reason: String
)
