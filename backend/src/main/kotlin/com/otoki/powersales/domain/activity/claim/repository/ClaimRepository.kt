package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClaimRepository : JpaRepository<Claim, Long>, ClaimRepositoryCustom {

    /** SAP 인바운드 단건 조회 (Spec #561) */
    fun findByName(name: String): Claim?

    /**
     * SF 재전송 배치 대상 claim id 조회 — 전송실패(SEND_FAILED) + 재시도 상한 미만.
     *
     * 영구 실패(예: SF Apex 미배포로 strict 파싱 실패)가 매 배치마다 재시도되어 이력을 오염시키지
     * 않도록 `sendAttemptCount < maxAttempt` 로 상한을 건다. 실제 전송/상태전이는
     * [com.otoki.powersales.domain.activity.claim.service.ClaimSfDispatchService.dispatch] 가 담당하므로
     * 여기서는 id 만 조회한다(락 경합 최소화 + 건별 트랜잭션 분리).
     */
    @Query(
        """
        select c.id from Claim c
        where c.status = :status
          and c.sendAttemptCount < :maxAttempt
        order by c.id asc
        """,
    )
    fun findResendTargetIds(
        @Param("status") status: ClaimStatus,
        @Param("maxAttempt") maxAttempt: Int,
    ): List<Long>

    /** SAP 인바운드 일괄 조회 (Spec #561) */
    fun findAllByNameIn(names: Collection<String>): List<Claim>

    /**
     * SF 송신 snapshot 복원용 단건 조회 — employee/account/product 를 fetch join 으로 즉시 로드.
     *
     * [com.otoki.powersales.domain.activity.claim.service.ClaimSfDispatchService.dispatch] 가
     * `@Async` + AFTER_COMMIT 로 별도 스레드/트랜잭션에서 claim 을 재로드해 SF 페이로드를 만든다.
     * 이 세 연관은 `@ManyToOne(LAZY)` 인데 bytecode enhancement(enableLazyInitialization) 환경에서
     * `findByIdOrNull` 후 접근 시 프록시가 초기화되지 않아 `claim.employee` 등이 null 로 평가된다.
     * fetch join 으로 employee/account/product 를 한 쿼리에 적재해 이 미초기화를 회피한다.
     */
    @Query(
        """
        select c from Claim c
        left join fetch c.employee
        left join fetch c.account
        left join fetch c.product
        where c.id = :id
        """,
    )
    fun findByIdWithSfRefs(@Param("id") id: Long): Claim?
}
