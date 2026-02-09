package com.otoki.internal.repository

import com.otoki.internal.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * 사용자 Repository
 */
interface UserRepository : JpaRepository<User, Long> {

    /**
     * 사번으로 사용자 조회
     */
    fun findByEmployeeId(employeeId: String): Optional<User>

    /**
     * 사번 존재 여부 확인
     */
    fun existsByEmployeeId(employeeId: String): Boolean

    /**
     * 지점별 사용자 목록 조회
     */
    fun findByBranchName(branchName: String): List<User>
}
