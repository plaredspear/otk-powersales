package com.otoki.internal.admin.security

object AdminRolePermissions {

    private val ALL_PERMISSIONS: Set<AdminPermission> = AdminPermission.entries.toSet()

    private val ROLE_PERMISSIONS: Map<String, Set<AdminPermission>> = mapOf(
        "조장" to ALL_PERMISSIONS,
        "지점장" to ALL_PERMISSIONS,
        "영업부장" to ALL_PERMISSIONS,
        "사업부장" to ALL_PERMISSIONS,
        "영업본부장" to ALL_PERMISSIONS,
        "영업지원실" to ALL_PERMISSIONS
    )

    fun getPermissions(role: String?): Set<AdminPermission> {
        if (role == null) return emptySet()
        return ROLE_PERMISSIONS[role] ?: emptySet()
    }
}
