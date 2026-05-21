package com.otoki.powersales.auth.repository

import com.otoki.powersales.auth.entity.UserRoleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleEntityRepository : JpaRepository<UserRoleEntity, Long> {
    fun findBySfid(sfid: String): UserRoleEntity?
}
