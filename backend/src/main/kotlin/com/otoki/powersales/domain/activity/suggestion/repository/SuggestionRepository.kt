package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 제안 Repository.
 *
 * `proposal_number` 채번은 PostgreSQL sequence `suggestion_proposal_number_seq` (P1-B §2.3) 사용 — race condition free.
 */
@Repository
interface SuggestionRepository : JpaRepository<Suggestion, Long>, SuggestionRepositoryCustom {

    /**
     * 단건 조회 (soft-delete 제외).
     *
     * 목록 조회(본인분/원가센터 스코프 + 거래처·기간 필터)는 [SuggestionRepositoryCustom.searchMine] 참고.
     */
    fun findByIdAndIsDeletedFalse(id: Long): Suggestion?

    /**
     * SF 재전송용 단건 조회 — employee/product/account 를 fetch join 으로 즉시 로드.
     *
     * SF 재전송([com.otoki.powersales.domain.activity.suggestion.service.SuggestionSfResendService]) 시
     * `employee.employeeCode` / `product.productCode` 등 `@ManyToOne(LAZY)` 연관을 참조하는데, bytecode
     * enhancement 환경에서는 readOnly 트랜잭션 안에서도 LAZY 프록시가 미초기화되어 null 로 평가되는 함정이
     * 있다(클레임 `findByIdWithSfRefs` 정합). fetch join 으로 미리 로드해 이를 회피한다.
     */
    @Query(
        """
        select s from Suggestion s
        left join fetch s.employee
        left join fetch s.product
        left join fetch s.account
        where s.id = :id
        """,
    )
    fun findByIdWithSfRefs(@Param("id") id: Long): Suggestion?

    /**
     * SF 재전송 배치 대상 suggestion id 조회 — 전송실패(SEND_FAILED) + 재시도 상한 미만.
     *
     * 영구 실패가 매 배치마다 재시도되어 이력을 오염시키지 않도록 `sfSendAttemptCount < maxAttempt` 상한을
     * 건다(클레임 [com.otoki.powersales.domain.activity.claim.repository.ClaimRepository.findResendTargetIds]
     * 정합). soft-delete row 도 전송 대상이 될 수 있으므로 삭제 필터는 두지 않는다.
     */
    @Query(
        """
        select s.id from Suggestion s
        where s.sfSendStatus = :status
          and s.sfSendAttemptCount < :maxAttempt
        order by s.id asc
        """,
    )
    fun findResendTargetIds(
        @Param("status") status: SuggestionSfSendStatus,
        @Param("maxAttempt") maxAttempt: Int,
    ): List<Long>

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
