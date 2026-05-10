package com.otoki.powersales.schedule.service.dto

/**
 * 인사발령 INSERT 실패 행.
 *
 * - [identifier] : 식별자 (employeeCode 또는 employeeCode+appointDate. 필수 누락 시 null)
 * - [reason] : 실패 사유 (예: `"EmployeeCode 필수"`, `"AppointDate YYYYMMDD 형식 오류: 2026-04-01"`)
 */
data class AppointmentInsertFailedRow(
    val identifier: String?,
    val reason: String
)
