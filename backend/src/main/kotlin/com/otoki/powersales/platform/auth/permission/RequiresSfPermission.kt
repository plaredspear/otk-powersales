package com.otoki.powersales.platform.auth.permission

/**
 * SF 권한 모델 기반 endpoint 가드 어노테이션 (spec #801).
 *
 * controller method 에 부착하여 SF Profile / PermissionSet 데이터 기반으로 권한 검사.
 * `WebAdminContextFilter` 가 매 요청마다 본 어노테이션을 검사하여 통과/차단 결정.
 *
 * ## 사용 패턴
 *
 * (a) entity × CRUD operation:
 *   `@RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.READ)`
 *   → SF API name 변환 후 (예: DKRetail__Employee__c) PermissionSetFlags.object_permissions JSON 의
 *      allowRead/allowCreate/allowEdit/allowDelete 비트 매칭.
 *
 * (b) system permission (admin 전용 / SF 비대응):
 *   `@RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)`
 *   → ProfileFlags 또는 PermissionSetFlags 의 해당 boolean 컬럼 매칭.
 *
 * ## 판정 정책
 *
 * - `VIEW_ALL_DATA` 비트 TRUE → 모든 entity READ 통과 (SF 표준).
 * - `MODIFY_ALL_DATA` 비트 TRUE → 모든 entity 의 모든 CRUD 통과 (SF 표준).
 * - 위 모두 미통과 시 403 Forbidden.
 *
 * ## 식별자 정책
 *
 * `entity` 속성은 신규 entity 의 `@Table(name)` value (예: `"employee"`, `"account"`). SF API name
 * 직접 사용 안 함. 변환은 `EntitySfNameRegistry` 가 reflection 으로 자동 처리.
 *
 * SF `@SFObject` 어노테이션 부재인 신규 자체 entity 의 권한 가드는 operation = SYSTEM 패턴 사용.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresSfPermission(
    val entity: String = "",
    val operation: SfPermissionOperation,
    val systemPermission: SfSystemPermission = SfSystemPermission.VIEW_ALL_DATA,
)
