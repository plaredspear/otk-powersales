package com.otoki.internal.common.repository

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.common.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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
     * 조직별 사용자 목록 조회
     */
    fun findByOrgName(orgName: String): List<User>

    /**
     * 지점 코드 목록 + 상태로 사용자 조회 (관리자 대시보드 기본현황)
     */
    fun findByCostCenterCodeInAndStatus(costCenterCodes: List<String>, status: String): List<User>

    /**
     * 상태별 사용자 전체 조회 (관리자 대시보드 - 전체 범위)
     */
    fun findByStatus(status: String): List<User>

    /**
     * sfid 목록으로 사용자 조회
     */
    fun findBySfidIn(sfids: List<String>): List<User>

    @Query("""
        SELECT new com.otoki.internal.branch.dto.response.BranchResponse(
            COALESCE(u.costCenterCode, u.orgName),
            u.orgName
        )
        FROM User u
        WHERE u.orgName IS NOT NULL AND u.isDeleted IS NOT TRUE
        GROUP BY u.orgName, u.costCenterCode
        ORDER BY u.orgName
    """)
    fun findDistinctBranches(): List<BranchResponse>
}
