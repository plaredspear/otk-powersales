package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ClaimRepository : JpaRepository<Claim, Long>, ClaimRepositoryCustom {

    /** SAP 인바운드 단건 조회 (Spec #561) */
    fun findByName(name: String): Claim?

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
