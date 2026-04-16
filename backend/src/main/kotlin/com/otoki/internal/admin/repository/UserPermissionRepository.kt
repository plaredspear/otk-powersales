package com.otoki.internal.admin.repository

import com.otoki.internal.admin.entity.UserPermission
import org.springframework.data.jpa.repository.JpaRepository

interface UserPermissionRepository : JpaRepository<UserPermission, Long> {
    fun findByEmployeeId(employeeId: Long): List<UserPermission>
    fun deleteByEmployeeId(employeeId: Long)
}
