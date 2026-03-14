package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.User
import com.otoki.internal.common.repository.UserRepositoryCustom
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * 사용자 Repository
 */
interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {

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

    /**
     * 진열스케줄 템플릿용 사원 조회
     * 조건: costCenterCode 일치, appAuthority IS NULL, appLoginActive=true, status 일치
     */
    fun findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
        costCenterCode: String,
        status: String
    ): List<User>

    /**
     * 사원번호 목록으로 일괄 조회 (Excel 업로드 검증용)
     */
    fun findByEmployeeIdIn(employeeIds: List<String>): List<User>
}
