package com.otoki.powersales.suggestion.repository

import com.otoki.powersales.suggestion.entity.Suggestion
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
interface SuggestionRepository : JpaRepository<Suggestion, Long> {

    /**
     * 본인 작성 목록 조회 (paged, soft-delete 제외, created_at DESC).
     */
    fun findByEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(
        employeeId: Long,
        pageable: Pageable
    ): Page<Suggestion>

    /**
     * 단건 조회 (soft-delete 제외).
     */
    fun findByIdAndIsDeletedFalse(id: Long): Suggestion?

    /**
     * `suggestion_proposal_number_seq` 의 다음 sequence 값 조회.
     *
     * PostgreSQL `nextval()` 은 원자성 보장 — race condition free.
     * Service `create()` mapping step 에서 `'S-' + YYYYMMDD + '-' + LPAD(nextval, 6, '0')` 로 합성.
     */
    @Query(
        value = "SELECT nextval('suggestion_proposal_number_seq')",
        nativeQuery = true
    )
    fun nextProposalNumberSeqValue(): Long
}
