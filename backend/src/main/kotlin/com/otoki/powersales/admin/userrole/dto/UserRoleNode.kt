package com.otoki.powersales.admin.userrole.dto

/**
 * UserRole 트리 node DTO — 자기참조 children 으로 hierarchy 표현.
 */
data class UserRoleNode(
    val userRoleId: Long,
    val name: String,
    val developerName: String?,
    val rollupDescription: String?,
    val parentUserRoleId: Long?,
    val parentName: String?,
    val children: List<UserRoleNode>,
)
