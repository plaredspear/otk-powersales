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

data class ProfileDetail(
    val profileId: Long,
    val name: String,
    val userType: String?,
    val description: String?,
    val flags: ProfileFlagsSummary,
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
