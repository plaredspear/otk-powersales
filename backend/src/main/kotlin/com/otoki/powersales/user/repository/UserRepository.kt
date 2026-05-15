package com.otoki.powersales.user.repository

import com.otoki.powersales.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {

    fun findByUsername(username: String): User?

    fun findByEmployeeCode(employeeCode: String): User?

    fun findByEmployeeCodeIn(employeeCodes: Collection<String>): List<User>

    fun findBySfid(sfid: String): User?
}
