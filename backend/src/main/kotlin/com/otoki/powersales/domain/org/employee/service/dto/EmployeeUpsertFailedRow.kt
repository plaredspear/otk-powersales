package com.otoki.powersales.domain.org.employee.service.dto

/**
 * 직원 마스터 UPSERT 실패 행. 도메인 결과 [EmployeeUpsertResult.failures] 의 원소.
 *
 * - [identifier] : 식별자 (현재 채택은 employeeCode 값. 필수 필드 누락 시 null)
 * - [reason] : 실패 사유 (예: `"EmployeeCode 필수"`, `"StartDate 형식 오류"`)
 */
data class EmployeeUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
