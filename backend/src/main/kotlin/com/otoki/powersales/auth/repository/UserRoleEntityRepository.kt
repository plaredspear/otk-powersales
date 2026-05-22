package com.otoki.powersales.auth.repository

import com.otoki.powersales.auth.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleEntityRepository : JpaRepository<UserRole, Long> {
    fun findBySfid(sfid: String): UserRole?
    fun findByDeveloperName(developerName: String): UserRole?
}
