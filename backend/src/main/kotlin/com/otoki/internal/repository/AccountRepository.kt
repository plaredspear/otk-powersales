package com.otoki.internal.repository

import com.otoki.internal.entity.Account
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 거래처 마스터 Repository
 */
interface AccountRepository : JpaRepository<Account, Int> {

    /**
     * 거래처 외부키(SAP 코드)로 조회
     */
    fun findByExternalKey(externalKey: String): Account?

    /**
     * 거래처 ID 목록으로 일괄 조회
     */
    fun findByIdIn(ids: List<Int>): List<Account>

    /**
     * 거래처 sfid 목록으로 일괄 조회 (스케줄 → 거래처명 매핑용)
     */
    fun findBySfidIn(sfids: List<String>): List<Account>

    /**
     * 거래처 sfid로 단건 조회 (출근 등록 시 위경도 확보)
     */
    fun findBySfid(sfid: String): Account?

    /**
     * 지점 코드 목록으로 거래처 일괄 조회 (관리자 대시보드 범위 필터)
     */
    fun findByBranchCodeIn(branchCodes: List<String>): List<Account>
}
