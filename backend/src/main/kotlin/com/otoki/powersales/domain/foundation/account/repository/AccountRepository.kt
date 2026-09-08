package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 거래처 마스터 Repository
 *
 * **미삭제 조건은 `is_deleted IS NULL OR is_deleted = false` 로 쓴다.**
 * `IsDeletedNot(true)` 파생 쿼리(= `is_deleted <> true`)는 안 된다 — SQL 3값 논리에서
 * `NULL <> true` 가 NULL 이라 is_deleted 가 NULL 인 거래처가 통째로 탈락한다.
 * SAP 거래처마스터 인터페이스([com.otoki.powersales.external.sap.inbound.service.SapClientMasterService])
 * 가 is_deleted 를 세팅하지 않아 NULL 이 정상 상태이며, 실제로 진열마스터가 확정·유효인데도
 * 주문서 작성 거래처가 0건이 되는 장애로 이어졌다 (2026-09-08).
 * QueryDSL 경로의 `isNotDeleted()` 와 동일 기준.
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
    @Query(
        """
        SELECT a FROM Account a
        WHERE a.branchCode = :branchCode
          AND a.accountGroup IN :accountGroups
          AND (a.isDeleted IS NULL OR a.isDeleted = false)
        """
    )
    fun findByBranchCodeAndAccountGroupInAndNotDeleted(
        @Param("branchCode") branchCode: String,
        @Param("accountGroups") accountGroups: List<String>,
    ): List<Account>

    /**
     * 거래처 ID 목록 + 삭제되지 않은 거래처 조회 (일반사원용)
     */
    @Query(
        """
        SELECT a FROM Account a
        WHERE a.id IN :ids
          AND (a.isDeleted IS NULL OR a.isDeleted = false)
        """
    )
    fun findByIdInAndNotDeleted(@Param("ids") ids: List<Long>): List<Account>

}
