package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.PermissionSet
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionSetRepository : JpaRepository<PermissionSet, Long> {
    fun findByName(name: String): PermissionSet?
}
