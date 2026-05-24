package com.otoki.powersales.user.repository

import com.otoki.powersales.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {

    fun findByUsername(username: String): User?

    fun findByEmployeeCode(employeeCode: String): User?

    fun findByEmployeeCodeIn(employeeCodes: Collection<String>): List<User>

    /** Spec #803 — Profile 상세의 부여 사용자 수 + 일람용. */
    fun countByProfileId(profileId: Long): Long

    fun findFirstByProfileId(profileId: Long): User?
}
