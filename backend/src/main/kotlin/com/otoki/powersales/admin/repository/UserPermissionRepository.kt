package com.otoki.powersales.admin.repository

import com.otoki.powersales.admin.entity.UserPermission
import org.springframework.data.jpa.repository.JpaRepository

interface UserPermissionRepository : JpaRepository<UserPermission, Long> {
    fun findByEmployeeId(employeeId: Long): List<UserPermission>
    fun deleteByEmployeeId(employeeId: Long)
}
