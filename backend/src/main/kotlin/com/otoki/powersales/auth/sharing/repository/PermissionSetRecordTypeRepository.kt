package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.PermissionSetRecordType
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetRecordTypeRepository : JpaRepository<PermissionSetRecordType, Long> {
    fun findAllByPermissionSetId(permissionSetId: Long): List<PermissionSetRecordType>
}
