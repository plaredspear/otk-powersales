package com.otoki.internal.admin.security

object AdminRolePermissions {

    private val ALL_PERMISSIONS: Set<AdminPermission> = AdminPermission.entries.toSet()

    private val READ_ONLY_SCHEDULE: Set<AdminPermission> = ALL_PERMISSIONS - AdminPermission.SCHEDULE_WRITE

    private val ROLE_PERMISSIONS: Map<String, Set<AdminPermission>> = mapOf(
        "조장" to ALL_PERMISSIONS,
        "지점장" to READ_ONLY_SCHEDULE,
        "영업부장" to READ_ONLY_SCHEDULE,
        "사업부장" to READ_ONLY_SCHEDULE,
        "영업본부장" to READ_ONLY_SCHEDULE,
        "영업지원실" to ALL_PERMISSIONS
    )

    fun getPermissions(role: String?): Set<AdminPermission> {
        if (role == null) return emptySet()
        return ROLE_PERMISSIONS[role] ?: emptySet()
    }

    fun getAllRolePermissions(): Map<String, Set<AdminPermission>> = ROLE_PERMISSIONS
}
