package com.otoki.internal.admin.repository

import com.otoki.internal.admin.entity.RolePermission
import org.springframework.data.jpa.repository.JpaRepository

interface RolePermissionRepository : JpaRepository<RolePermission, Long> {
    fun findByRole(role: String): List<RolePermission>
    fun deleteByRole(role: String)
}
