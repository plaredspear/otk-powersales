package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.PermissionSetFlags
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetFlagsRepository : JpaRepository<PermissionSetFlags, Long> {
    fun findByPermissionSetSfid(permissionSetSfid: String): PermissionSetFlags?
}
