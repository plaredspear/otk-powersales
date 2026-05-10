package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 거래처 마스터 Repository
 */
interface AccountRepository : JpaRepository<Account, Int>, AccountRepositoryCustom {

    /**
     * 동일 [name] + 활성(미삭제) 거래처 존재 여부.
     *
     * `is_deleted` 가 nullable Boolean 이므로 `IS NULL` 과 `= false` 두 케이스 모두 활성으로 간주.
     * (Spring Data 메서드명 표기로는 "NULL OR false" 표현이 어려워 native query 로 명시)
     *
     * Spec #640 — 관리자 웹 신규 거래처 등록 동일명 중복 검증.
     * 레거시 `AccountTriggerHandler.cls:69-72` 의 `SELECT COUNT(Name) FROM Account WHERE Name = :a.Name`
     * 와 의미 동등 (SF Account 의 `IsDeleted=false` 는 SOQL 기본 필터, Heroku Connect 잔재 NULL row 도 활성 간주).
     */
    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM account WHERE name = :name AND (is_deleted IS NULL OR is_deleted = FALSE))",
        nativeQuery = true
    )
    fun existsActiveByName(@Param("name") name: String): Boolean

    /**
     * 활성(미삭제) 거래처 단건 조회.
     *
     * `is_deleted` 가 nullable Boolean 이므로 `IS NULL` 과 `= false` 두 케이스 모두 활성으로 간주.
     * (Spring Data 메서드명 `findByIdAndIsDeletedNot` 으로는 NULL row 가 누락됨 — `existsActiveByName` 패턴 일관)
     *
     * Spec #642 — 관리자 웹 거래처 삭제 진입 시 단건 조회.
     * Spec #643 — 관리자 웹 거래처 수정 진입 시 단건 조회 (재사용).
     */
    @Query(
        value = "SELECT * FROM account WHERE account_id = :id AND (is_deleted IS NULL OR is_deleted = FALSE)",
        nativeQuery = true
    )
    fun findActiveById(@Param("id") id: Int): Account?

    /**
     * 동일 [name] + 활성(미삭제) + 자기 자신 제외 거래처 존재 여부.
     *
     * `is_deleted` 가 nullable Boolean 이므로 `IS NULL` 과 `= false` 두 케이스 모두 활성으로 간주
     * (`existsActiveByName` 패턴 일관). UPDATE 시 자기 자신은 중복 검증에서 제외하기 위해 [id] 를 받는다.
     *
     * Spec #643 — 관리자 웹 거래처 수정 동일명 중복 검증.
     * 레거시 `AccountTriggerHandler.cls:133-138` 의 `SELECT COUNT(Name) FROM Account WHERE Name = :Name`
     * 와 의미 동등 (UPDATE 흐름에서는 자기 자신을 카운트에서 제외해야 함 — `Trigger.oldMap` 비교 동등 효과).
     */
    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM account WHERE name = :name AND account_id <> :id AND (is_deleted IS NULL OR is_deleted = FALSE))",
        nativeQuery = true
    )
    fun existsActiveByNameAndIdNot(@Param("name") name: String, @Param("id") id: Int): Boolean

    /**
     * 거래처 외부키(SAP 코드)로 조회
     */
    fun findByExternalKey(externalKey: String): Account?

    /**
     * 거래처 ID 목록으로 일괄 조회
     */
    fun findByIdIn(ids: List<Int>): List<Account>

    /**
     * 지점 코드 목록으로 거래처 일괄 조회 (관리자 대시보드 범위 필터)
     */
    fun findByBranchCodeIn(branchCodes: List<String>): List<Account>

    /**
     * 거래처 외부키 목록으로 일괄 조회 (Excel 업로드 검증용)
     */
    fun findByExternalKeyIn(externalKeys: List<String>): List<Account>

    /**
     * 지점 코드 + 거래처 그룹으로 거래처 조회 (여사원 일정관리)
     */
    fun findByBranchCodeAndAccountGroupIn(branchCode: String, accountGroups: List<String>): List<Account>

    /**
     * 거래처명 부분 일치 조회 (진열스케줄 목록 필터)
     */
    fun findByNameContainingIgnoreCase(name: String): List<Account>

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
    fun findByIdInAndIsDeletedNot(ids: List<Int>, isDeleted: Boolean): List<Account>
}
