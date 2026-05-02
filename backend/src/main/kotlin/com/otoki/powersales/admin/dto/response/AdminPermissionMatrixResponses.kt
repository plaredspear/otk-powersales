package com.otoki.powersales.admin.dto.response

data class PermissionMatrixResponse(
    val permissions: List<PermissionDetail>,
    val roles: List<RolePermissions>,
    val currentUser: CurrentUserPermission
)

data class PermissionDetail(
    val code: String,
    val description: String,
    val menus: List<String>
)

data class RolePermissions(
    val role: String,
    val roleLabel: String,
    val permissions: List<String>
)

data class CurrentUserPermission(
    val role: String,
    val roleLabel: String,
    val permissions: List<String>,
    val canManagePermissions: Boolean = false
)
