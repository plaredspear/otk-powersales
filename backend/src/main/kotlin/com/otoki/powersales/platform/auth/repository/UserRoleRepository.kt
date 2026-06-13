package com.otoki.powersales.platform.auth.repository

import com.otoki.powersales.platform.auth.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleRepository : JpaRepository<UserRole, Long> {
    fun findByDeveloperName(developerName: String): UserRole?
}
