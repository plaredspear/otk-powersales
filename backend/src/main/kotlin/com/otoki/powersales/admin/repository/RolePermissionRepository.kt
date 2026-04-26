package com.otoki.powersales.admin.repository

import com.otoki.powersales.admin.entity.RolePermission
import org.springframework.data.jpa.repository.JpaRepository

interface RolePermissionRepository : JpaRepository<RolePermission, Long> {
    fun findByRole(role: String): List<RolePermission>
    fun deleteByRole(role: String)
}
