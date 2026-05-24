package com.otoki.powersales.auth.repository

import com.otoki.powersales.auth.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleRepository : JpaRepository<UserRole, Long> {
    fun findByDeveloperName(developerName: String): UserRole?
}
