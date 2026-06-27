package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 거래처 마스터 Repository
 */
interface AccountRepository : JpaRepository<Account, Long>, AccountRepositoryCustom {

    /**
     * 거래처 외부키(SAP 코드)로 조회
     */
    fun findByExternalKey(externalKey: String): Account?

    /**
     * 거래처 ID 목록으로 일괄 조회
     */
    fun findByIdIn(ids: List<Long>): List<Account>

    /**
     * 지점 코드 목록으로 거래처 일괄 조회 (관리자 대시보드 범위 필터)
     */
    fun findByBranchCodeIn(branchCodes: List<String>): List<Account>

    /**
     * 거래처 외부키 목록으로 일괄 조회 (Excel 업로드 검증용)
     */
    fun findByExternalKeyIn(externalKeys: List<String>): List<Account>

    /**
     * 지점 코드 IN + 거래처 그룹으로 거래처 조회 (여사원 일정관리).
     *
     * SF 정합: 호출처에서 [com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander.expand]
     * 로 BranchMapping 1:N 확장된 코드 집합을 전달. SF `Util.getIncludedBranchCode` 와 동등.
     */
    fun findByBranchCodeInAndAccountGroupIn(branchCodes: Collection<String>, accountGroups: List<String>): List<Account>

    /**
     * 거래처명 부분 일치 조회 (진열스케줄 목록 필터)
     */
    fun findByNameContainingIgnoreCase(name: String): List<Account>

    /**
     * 거래처명 또는 거래처코드(외부키) 부분 일치 조회 (진열스케줄 목록 필터).
     *
     * 단일 입력값을 거래처명/거래처코드 양쪽에 OR 매칭 — 사용자가 거래처명 input 에
     * 거래처코드를 입력해도 동일하게 조회되도록 한다.
     */
    fun findByNameContainingIgnoreCaseOrExternalKeyContainingIgnoreCase(
        name: String,
        externalKey: String,
    ): List<Account>

    /**
     * 지점 코드 + 거래처 그룹 + 삭제되지 않은 거래처 조회 (조장용)
     */
    fun findByBranchCodeAndAccountGroupInAndIsDeletedNot(
        branchCode: String,
        accountGroups: List<String>,
        isDeleted: Boolean
    ): List<Account>

    /**
     * 거래처 ID 목록 + 삭제되지 않은 거래처 조회 (일반사원용)
     */
    fun findByIdInAndIsDeletedNot(ids: List<Long>, isDeleted: Boolean): List<Account>
}
