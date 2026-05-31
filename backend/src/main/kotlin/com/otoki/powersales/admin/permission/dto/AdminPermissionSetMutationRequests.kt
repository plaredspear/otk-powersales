package com.otoki.powersales.admin.permission.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Spec #837 — PermissionSet 자체 관리 endpoint 의 request DTO.
 *
 * - [PermissionSetCreateRequest]      : POST /api/v1/admin/permissions/permission-sets
 * - [PermissionSetUpdateMetaRequest]  : PUT  /api/v1/admin/permissions/permission-sets/{id}
 * - [PermissionSetUpdateFlagsRequest] : PUT  /api/v1/admin/permissions/permission-sets/{id}/flags
 *
 * name 의 형식 검증 (영문/숫자/언더스코어) 은 service 에서 정규식 검증 — Bean Validation 으로는 사후
 * 의미 있는 에러코드 (`INVALID_NAME`) 노출이 어려워 service 단에서 throw.
 */

data class PermissionSetCreateRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val name: String,

    @field:Size(max = 255)
    val label: String? = null,

    @field:Size(max = 1024)
    val description: String? = null,
)

data class PermissionSetUpdateMetaRequest(
    @field:Size(max = 255)
    val label: String? = null,

    @field:Size(max = 1024)
    val description: String? = null,
)

/**
 * 전체 교체 방식. 클라이언트는 매트릭스 UI 의 현재 비트 set 을 그대로 전송한다.
 * 부분 patch 아님 — 누락된 키는 "권한 부여 안 함" 으로 해석.
 *
 * @property objectPermissions SF API name → 6비트 boolean Map. 키는 EntitySfNameRegistry 의 SF 매핑 entity 만 허용.
 * @property customPermissions 가상 자원 name → 4비트 boolean Map. 키는 EntitySfNameRegistry 의 allResources 중 SF 매핑 외의 자원만 허용 (`@PermissionResource`).
 */
data class PermissionSetUpdateFlagsRequest(
    val viewAllData: Boolean = false,
    val modifyAllData: Boolean = false,
    val objectPermissions: Map<String, Map<String, Boolean>> = emptyMap(),
    val customPermissions: Map<String, Map<String, Boolean>> = emptyMap(),
)

/**
 * Profile 권한 비트 전체 교체 request — PUT /api/v1/admin/permissions/profiles/{id}/flags.
 *
 * SF 레거시 정합 — Profile 도 객체권한을 보유 (발령 시 직책 → Profile 로 화면권한 자동 전파).
 * PermissionSet 과 달리 Profile 은 system 비트 5종 전부 (viewAllUsers / manageUsers / apiEnabled 포함) 를 다룬다.
 * 전체 교체 방식 — 누락 키는 "권한 없음" 으로 해석.
 */
data class ProfileUpdateFlagsRequest(
    val viewAllData: Boolean = false,
    val modifyAllData: Boolean = false,
    val viewAllUsers: Boolean = false,
    val manageUsers: Boolean = false,
    val apiEnabled: Boolean = false,
    val objectPermissions: Map<String, Map<String, Boolean>> = emptyMap(),
    val customPermissions: Map<String, Map<String, Boolean>> = emptyMap(),
)
