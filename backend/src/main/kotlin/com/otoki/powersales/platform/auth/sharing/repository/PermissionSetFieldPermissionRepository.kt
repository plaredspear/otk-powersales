package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetFieldPermission
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetFieldPermissionRepository : JpaRepository<PermissionSetFieldPermission, Long> {
    fun findAllByPermissionSetIdAndSObjectName(permissionSetId: Long, sObjectName: String): List<PermissionSetFieldPermission>
}
