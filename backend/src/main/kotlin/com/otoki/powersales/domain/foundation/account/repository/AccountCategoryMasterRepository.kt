package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AccountCategoryMasterRepository : JpaRepository<AccountCategoryMaster, Long> {
    fun findByAccountCode(accountCode: String): AccountCategoryMaster?

    /**
     * 조회화면이용(useSearch) + 미삭제 거래처유형마스터 (거래처코드 오름차순).
     *
     * 미삭제 조건은 `is_deleted IS NULL OR = false` — `IsDeletedNot(true)` 파생 쿼리는
     * NULL 행을 통째로 탈락시킨다 ([com.otoki.powersales.domain.foundation.account.repository.AccountRepository] KDoc 참조).
     */
    @Query(
        """
        SELECT m FROM AccountCategoryMaster m
        WHERE m.useSearch = true
          AND (m.isDeleted IS NULL OR m.isDeleted = false)
        ORDER BY m.accountCode
        """
    )
    fun findByUseSearchTrueAndNotDeletedOrderByAccountCode(): List<AccountCategoryMaster>

    /**
     * spec #680 §5.3 — refreshIntegration 의 EmployeeInputCriteriaMaster lookup.
     *
     * `Account.accountType` (거래처유형마스터 Name raw 값, 예: "슈퍼") 으로 매칭되는 AccountCategoryMaster 조회.
     * legacy `MonthlyEmpIntegrationSchTriggerHandler` 의 `criteriaMap key = Category.Name + Year + Month`
     * 동등 — Category.Name 매칭. 운영 정합 dev 검증 2026-05-26.
     */
    fun findByName(name: String): AccountCategoryMaster?

    /**
     * `findByName` 의 배치 버전 — refreshIntegration 성능 (spec #680 §5.3).
     *
     * 그룹 루프 안에서 accountType 별로 반복 조회하던 것을 name IN 1회 조회로 대체.
     * 동명(Name) 중복이 없다는 마스터 특성상 name → 단건 map 으로 사용한다.
     */
    fun findByNameIn(names: Collection<String>): List<AccountCategoryMaster>

    /**
     * 거래처유형(유통형태) 검색용 — 이름 부분일치 + 조회화면이용(useSearch) + 미삭제.
     *
     * 화면 검색어로 매칭되는 거래처유형마스터 Name 목록을 얻어, 그 Name 으로 `Account.accountType`
     * (raw String) IN 조건을 직접 구성한다. `getSearchCategories` 와 동일하게
     * useSearch=true 항목만 검색 대상으로 노출.
     */
    @Query(
        """
        SELECT m FROM AccountCategoryMaster m
        WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND m.useSearch = true
          AND (m.isDeleted IS NULL OR m.isDeleted = false)
        """
    )
    fun findByNameContainingIgnoreCaseAndUseSearchTrueAndNotDeleted(
        @Param("name") name: String,
    ): List<AccountCategoryMaster>
}
