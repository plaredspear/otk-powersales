package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * 제안 Repository.
 *
 * `proposal_number` 채번은 PostgreSQL sequence `suggestion_proposal_number_seq` (P1-B §2.3) 사용 — race condition free.
 */
@Repository
interface SuggestionRepository : JpaRepository<Suggestion, Long>, SuggestionRepositoryCustom {

    /**
     * 본인 작성 목록 조회 (paged, soft-delete 제외, created_at DESC).
     */
    fun findByEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(
        employeeId: Long,
        pageable: Pageable
    ): Page<Suggestion>

    /**
     * 본인 작성 목록 조회 — 분류 필터 (paged, soft-delete 제외, created_at DESC).
     *
     * 레거시 `logisticsclaimlist` (물류클레임 전용 조회) 대응 — category=LOGISTICS_CLAIM 으로 호출한다.
     */
    fun findByEmployeeIdAndCategoryAndIsDeletedFalseOrderByCreatedAtDesc(
        employeeId: Long,
        category: SuggestionCategory,
        pageable: Pageable
    ): Page<Suggestion>

    /**
     * 단건 조회 (soft-delete 제외).
     */
    fun findByIdAndIsDeletedFalse(id: Long): Suggestion?

    /**
     * 원가센터 단위 분류 목록 조회 (paged, soft-delete 제외, created_at DESC).
     *
     * 레거시 `LogisticsClaimSearch` 의 조장/지점장(여사원 외) 권한 분기 — `CostCenterCode` 기준 전체 조회 대응.
     * (`org_cost_center_code` 는 등록 시 `OrgCostCenterMatchService` 로 정규화된 값. 조회자도 동일 매핑으로 비교.)
     */
    fun findByOrgCostCenterCodeAndCategoryAndIsDeletedFalseOrderByCreatedAtDesc(
        orgCostCenterCode: String,
        category: SuggestionCategory,
        pageable: Pageable
    ): Page<Suggestion>

    /**
     * `suggestion_proposal_number_seq` 의 다음 sequence 값 조회.
     *
     * PostgreSQL `nextval()` 은 원자성 보장 — race condition free.
     * Service `create()` mapping step 에서 `'S-' + YYYYMMDD + '-' + LPAD(nextval, 6, '0')` 로 합성.
     */
    @Query(
        value = "SELECT nextval('powersales.suggestion_proposal_number_seq')",
        nativeQuery = true
    )
    fun nextProposalNumberSeqValue(): Long
}
