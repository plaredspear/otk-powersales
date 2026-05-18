package com.otoki.powersales.admin.repository

import com.otoki.powersales.admin.entity.QRolePermission.Companion.rolePermission
import com.otoki.powersales.admin.entity.RolePermission
import com.querydsl.jpa.impl.JPAQueryFactory

class RolePermissionRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : RolePermissionRepositoryCustom {

    override fun findByRoleName(roleName: String): List<RolePermission> {
        return queryFactory
            .selectFrom(rolePermission)
            .where(rolePermission.role.eq(roleName))
            .fetch()
    }

    override fun deleteByRoleName(roleName: String): Long {
        return queryFactory
            .delete(rolePermission)
            .where(rolePermission.role.eq(roleName))
            .execute()
    }
}
