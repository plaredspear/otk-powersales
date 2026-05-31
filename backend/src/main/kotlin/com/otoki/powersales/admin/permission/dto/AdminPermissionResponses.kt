package com.otoki.powersales.admin.permission.dto

/**
 * Spec #803 — 권한 관리 admin 조회 응답 DTO.
 *
 * Profile / PermissionSet / Matrix 5 endpoint 의 응답 모델.
 */

data class ProfileSummary(
    val profileId: Long,
    val name: String,
    val userType: String?,
    val description: String?,
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
    val viewAllUsers: Boolean,
    val manageUsers: Boolean,
    val apiEnabled: Boolean,
    val assignedUserCount: Long,
)

data class ProfileFlagsSummary(
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
    val viewAllUsers: Boolean,
    val manageUsers: Boolean,
    val apiEnabled: Boolean,
)

/**
 * Profile 권한 비트 편집 (PUT /profiles/{id}/flags) 응답.
 *
 * system 비트 5종 + 객체/가상자원 권한 + dirty 플래그. PermissionSet 과 달리 Profile 은 SF 출처만 존재
 * (신규 Profile 생성 기능 없음) 하므로 sfOrigin 미노출 — isLocallyModified 로 신규 수정 여부만 표시.
 */
data class ProfileFlagsMutationResponse(
    val profileId: Long,
    val name: String,
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
    val viewAllUsers: Boolean,
    val manageUsers: Boolean,
    val apiEnabled: Boolean,
    val objectPermissions: Map<String, Map<String, Boolean>>,
    val customPermissions: Map<String, Map<String, Boolean>>,
    val isLocallyModified: Boolean,
)

data class ProfileDetail(
    val profileId: Long,
    val name: String,
    val userType: String?,
    val description: String?,
    val flags: ProfileFlagsSummary,
    /** SF 객체권한 (PermissionSetDetail 과 동일 행 형식). 편집 화면의 현재값 표시용. */
    val objectPermissions: List<ObjectPermissionRow>,
    /** @PermissionResource 가상 자원 권한. */
    val customPermissions: List<CustomPermissionRow>,
    /** Web admin 에서 Profile 권한이 수정되었는지 (dirty). */
    val isLocallyModified: Boolean,
    val assignedUsers: PaginatedUserList,
)

data class PermissionSetSummary(
    val permissionSetId: Long,
    val name: String,
    val label: String?,
    val description: String?,
    val permissionSetFlagsId: Long?,
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
    val objectPermissionCount: Int,
    val assignedUserCount: Long,
    /** Spec #837 — SF 출처 PS 여부 (sfid IS NOT NULL). UI 의 "출처" 컬럼 / 삭제 버튼 비활성화 판정용. */
    val sfOrigin: Boolean,
    /** Spec #837 — dirty 플래그. 신규 시스템에서 메타/비트가 수정된 SF 출처 PS 표시용. */
    val isLocallyModified: Boolean,
)

data class PermissionSetFlagsSummary(
    val permissionSetFlagsId: Long,
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
)

data class ObjectPermissionRow(
    val sfApiName: String,
    val entity: String?,
    val canRead: Boolean,
    val canCreate: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
)

/**
 * spec #808 — `@PermissionResource` 명시 등록된 가상 자원의 CRUD 권한 비트.
 * SF customPermissions 메타에 대응.
 */
data class CustomPermissionRow(
    val resource: String,
    val canRead: Boolean,
    val canCreate: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
)

data class PermissionSetDetail(
    val permissionSetId: Long,
    val name: String,
    val label: String?,
    val description: String?,
    val flags: PermissionSetFlagsSummary?,
    val objectPermissions: List<ObjectPermissionRow>,
    val customPermissions: List<CustomPermissionRow>,
    val assignedUsers: PaginatedPermissionSetUserList,
    /** Spec #837 — SF 출처 PS 여부. */
    val sfOrigin: Boolean,
    /** Spec #837 — dirty 플래그. */
    val isLocallyModified: Boolean,
)

data class AssignedPermissionSetUserSummary(
    val assignmentId: Long,
    val userId: Long,
    val username: String,
    val employeeCode: String?,
    val employeeName: String?,
)

data class PaginatedPermissionSetUserList(
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val content: List<AssignedPermissionSetUserSummary>,
)

data class AssignedUserSummary(
    val userId: Long,
    val username: String,
    val employeeCode: String?,
    val employeeName: String?,
)

data class PaginatedUserList(
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val content: List<AssignedUserSummary>,
)

data class PermissionMatrixProfile(
    val profileId: Long,
    val name: String,
)

data class EntityProfilePermission(
    val profileId: Long,
    val canRead: Boolean,
    val canCreate: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
)

data class EntityProfileRow(
    val entity: String,
    val byProfile: List<EntityProfilePermission>,
)

data class PermissionMatrix(
    val profiles: List<PermissionMatrixProfile>,
    val rows: List<EntityProfileRow>,
)

/**
 * "페이지별 필요 권한" 가이드 페이지가 사용. PermissionSet 일람 + 각 PS 의 시스템 권한 flag
 * + entity 객체권한 매트릭스를 한 번의 endpoint 호출로 반환.
 */
data class PermissionSetMatrixEntry(
    val permissionSetId: Long,
    val name: String,
    val label: String?,
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
    val objectPermissions: List<ObjectPermissionRow>,
)

data class PermissionSetMatrix(
    val permissionSets: List<PermissionSetMatrixEntry>,
)

// ── Spec #837 — PermissionSet 자체 관리 응답 DTO ─────────────────────────

/**
 * Spec #837 — PS 등록 / 메타·비트 수정 후 반환. 본 응답에는 매트릭스 편집 UI 가 재로드 필요한
 * 모든 필드 포함 (메타 + flags + 권한 비트 본문 + dirty/origin 식별).
 *
 * 정책 (#837 결정 1-A): sfid 자체는 노출하지 않고 `sfOrigin: Boolean` 으로 SF 출처 여부만 표시.
 *
 * 형식 차이 (#837 결정 C): [objectPermissions]/[customPermissions] 는 매트릭스 UI 저장-전송
 * round-trip 을 위해 `Map<String, Map<String, Boolean>>` (DB jsonb 본문 그대로) 형식. 반면
 * Inspection 의 [PermissionSetDetail] 은 표 렌더링용 [ObjectPermissionRow] 리스트. Web client 는
 * PermissionMatrixEditor 컴포넌트 안의 어댑터로 두 형식을 상호 변환한다.
 */
data class PermissionSetMutationResponse(
    val permissionSetId: Long,
    val name: String,
    val label: String?,
    val description: String?,
    val sfOrigin: Boolean,
    val permissionSetFlagsId: Long?,
    val viewAllData: Boolean,
    val modifyAllData: Boolean,
    val objectPermissions: Map<String, Map<String, Boolean>>,
    val customPermissions: Map<String, Map<String, Boolean>>,
    val isLocallyModified: Boolean,
)

/**
 * Spec #837 — 변경 이력 조회 응답.
 * before/after 는 JSON 문자열 그대로 반환 — 클라이언트가 diff 모달에서 파싱.
 */
data class PermissionSetChangeLogResponse(
    val changeLogId: Long,
    val permissionSetId: Long?,
    val eventType: String,
    val beforeSnapshot: String?,
    val afterSnapshot: String?,
    val changedById: Long,
    val changedByName: String?,
    val changedAt: java.time.LocalDateTime,
    val changeReason: String?,
)

data class PaginatedPermissionSetChangeLogList(
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val content: List<PermissionSetChangeLogResponse>,
)

/**
 * Spec #837 — 권한 비트 매트릭스 편집 UI 의 자원 카탈로그.
 *
 * - [sfObjects]       : EntitySfNameRegistry.snapshot() (SF API name → 신규 entity table name)
 * - [customResources] : EntitySfNameRegistry.allResources() 중 SF 매핑 외 자원 (`@PermissionResource`)
 */
data class AvailablePermissionResources(
    val sfObjects: List<SfObjectResource>,
    val customResources: List<String>,
)

data class SfObjectResource(
    val sfApiName: String,
    val entity: String,
)
