package com.otoki.powersales.user.repository

import com.otoki.powersales.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): User?

    fun findByEmployeeNumber(employeeNumber: String): User?

    fun findByEmployeeNumberIn(employeeNumbers: Collection<String>): List<User>

    fun findBySfid(sfid: String): User?
}
