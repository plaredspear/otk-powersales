package com.otoki.powersales.domain.org.employee.service.dto

/**
 * 직원 마스터 UPSERT 도메인 입력 커맨드.
 *
 * - UPSERT 키: [employeeCode] (= [com.otoki.powersales.domain.org.employee.entity.Employee.employeeCode])
 *
 * 외부 채널(SAP 인바운드 등) 의 페이로드를 [com.otoki.powersales.domain.org.employee.service.EmployeeUpsertService] 가 받기 위한 도메인 용어 모델.
 * 날짜/Gender/LockingFlag 변환은 도메인 측이 String 으로 받아 변환 + 행 단위 검증 분기.
 */
data class EmployeeUpsertCommand(
    val employeeCode: String?,
    val employeeName: String?,
    val gender: String?,
    val homePhone: String?,
    val workPhone: String?,
    val workEmail: String?,
    val email: String?,
    val startDate: String?,
    val endDate: String?,
    val status: String?,
    val birthdate: String?,
    val orgCode: String?,
    val lockingFlag: String?
)
