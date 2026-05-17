package com.otoki.powersales.admin.repository

import com.otoki.powersales.admin.entity.UserPermission
import org.springframework.data.jpa.repository.JpaRepository

interface UserPermissionRepository : JpaRepository<UserPermission, Long> {
    fun findByUserId(userId: Long): List<UserPermission>
    fun deleteByUserId(userId: Long)
}
