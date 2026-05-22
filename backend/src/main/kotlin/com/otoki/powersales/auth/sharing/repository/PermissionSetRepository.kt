package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.PermissionSet
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetRepository : JpaRepository<PermissionSet, Long> {
    fun findByName(name: String): PermissionSet?
    fun findBySfid(sfid: String): PermissionSet?
}
