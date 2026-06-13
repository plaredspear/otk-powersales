package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetFlags
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetFlagsRepository : JpaRepository<PermissionSetFlags, Long> {
    fun findByPermissionSetId(permissionSetId: Long): PermissionSetFlags?
}
