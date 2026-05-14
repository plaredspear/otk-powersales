package com.otoki.powersales.employee.enums

/**
 * 직원 정보 출처 구분 (Spec #579).
 *
 * - [SAP] : SAP 인바운드로 등록·갱신되는 일반 직원
 * - [MANUAL] : Web Admin 에서 수동 등록된 시스템 관리자
 *
 * `MANUAL` 직원은 SAP 인바운드 upsert 갱신 대상에서 제외된다.
 */
enum class EmployeeOrigin {
    SAP,
    MANUAL
}
