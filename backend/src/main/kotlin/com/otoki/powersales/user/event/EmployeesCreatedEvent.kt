package com.otoki.powersales.user.event

/**
 * Employee 신규 생성 이벤트 (배치 단위).
 *
 * 발행: 도메인 서비스 (예: `EmployeeUpsertService`) 가 한 번의 upsert 에서 신규 INSERT 된
 * Employee **전체 집합**을 1건으로 발행.
 * 수신: [com.otoki.powersales.user.service.UserProvisioningService] (`@TransactionalEventListener(AFTER_COMMIT) + @Async`)
 *
 * 의미: SF 레거시 `IF_REST_SAP_EmployeeMaster.upsertUser(@future)` 의 비동기 후처리에 대응.
 * 레거시는 사원 N명을 future **1회 호출**(bulk: `WHERE ... IN :insertCode` 단일 SOQL + 단일 `insert`)로
 * 처리한다 (cls:165 단일 호출, cls:277·306 단일 DML). 신규도 동일하게 신규 사원 집합을 1건의 이벤트로
 * 발행하여 1개의 비동기 작업으로 일괄 처리한다 — 사원 1명당 이벤트 1개(N개) 발행 시 발생하는
 * `@Async` 작업 폭증 / executor 큐 적재를 근원 차단.
 *
 * User 생성 실패는 Employee 트랜잭션에 영향을 주지 않는다 (`AFTER_COMMIT` + 별도 트랜잭션).
 *
 * 각 스냅샷은 User 생성에 필요한 최소 필드만 담는다 (Employee entity 직접 참조 금지 — detach 회피 + 락 회피).
 */
data class EmployeesCreatedEvent(
    val employees: List<EmployeeSnapshot>,
)

/**
 * User 생성용 신규 Employee 스냅샷.
 *
 * `role`: SF `DKRetail__AppAuthority__c` picklist 4종 raw value 또는 null.
 */
data class EmployeeSnapshot(
    val employeeCode: String,
    val name: String,
    val workEmail: String?,
    val email: String?,
    val birthDate: String?,
    val role: String?,
    val appLoginActive: Boolean?,
    val costCenterCode: String? = null,
)
