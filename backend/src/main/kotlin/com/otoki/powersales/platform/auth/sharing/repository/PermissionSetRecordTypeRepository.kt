package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetRecordType
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetRecordTypeRepository : JpaRepository<PermissionSetRecordType, Long> {
    fun findAllByPermissionSetId(permissionSetId: Long): List<PermissionSetRecordType>

    fun findAllByPermissionSetIdIn(permissionSetIds: Collection<Long>): List<PermissionSetRecordType>
}
