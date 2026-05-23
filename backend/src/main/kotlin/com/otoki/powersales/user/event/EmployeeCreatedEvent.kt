package com.otoki.powersales.user.event

/**
 * Employee 신규 생성 이벤트.
 *
 * 발행: 도메인 서비스 (예: `EmployeeUpsertService`) 가 Employee INSERT 후 발행.
 * 수신: [com.otoki.powersales.user.service.UserProvisioningService] (`@TransactionalEventListener(AFTER_COMMIT) + @Async`)
 *
 * 의미: SF 레거시 `IF_REST_SAP_EmployeeMaster.upsertUser(@future)` 의 비동기 후처리에 대응.
 * Employee 트랜잭션 commit 후 별도 스레드 + 별도 트랜잭션에서 매칭 User 행을 생성한다.
 * User 생성 실패는 Employee 트랜잭션에 영향을 주지 않는다.
 *
 * 필드: User 생성에 필요한 최소 스냅샷만 담는다 (Employee entity 직접 참조 금지 — detach 회피 + 락 회피).
 *
 * `role`: SF `DKRetail__AppAuthority__c` picklist 4종 raw value 또는 null.
 */
data class EmployeeCreatedEvent(
    val employeeCode: String,
    val name: String,
    val workEmail: String?,
    val email: String?,
    val birthDate: String?,
    val role: String?,
    val appLoginActive: Boolean?,
    val costCenterCode: String? = null,
)
