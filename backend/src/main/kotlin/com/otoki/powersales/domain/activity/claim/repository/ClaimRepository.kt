package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimSfSendStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ClaimRepository : JpaRepository<Claim, Long>, ClaimRepositoryCustom {

    /** SAP 인바운드 단건 조회 (Spec #561) */
    fun findByName(name: String): Claim?

    /**
     * SF 재전송 배치 대상 claim id 조회 — SF 전송실패(sfSendStatus=SEND_FAILED) + 재시도 상한 미만.
     *
     * 신규→SF 전송상태(sfSendStatus) 로 판별하므로, SF origin 마이그레이션 row(sfSendStatus=NULL)는
     * 자연히 제외된다 — 마이그레이션 데이터는 재전송 대상이 아니고 신규 등록 건만 대상이다.
     *
     * 영구 실패(예: SF Apex 미배포로 strict 파싱 실패)가 매 배치마다 재시도되어 이력을 오염시키지
     * 않도록 `sfSendAttemptCount < maxAttempt` 로 상한을 건다. 실제 전송/상태전이는
     * [com.otoki.powersales.domain.activity.claim.service.ClaimSfDispatchService.dispatch] 가 담당하므로
     * 여기서는 id 만 조회한다(락 경합 최소화 + 건별 트랜잭션 분리).
     */
    @Query(
        """
        select c.id from Claim c
        where c.sfSendStatus = :status
          and c.sfSendAttemptCount < :maxAttempt
        order by c.id asc
        """,
    )
    fun findResendTargetIds(
        @Param("status") status: ClaimSfSendStatus,
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

    /**
     * 코스모스 전송상태 파생 승격 대상 건수 —
     * [com.otoki.powersales.domain.activity.claim.service.ClaimDeliveryStatusRule] 의 SQL 표현.
     *
     * 조건은 규칙 object 와 1:1 로 대응한다(이관분 제외 / 커트라인 이후 등록 / 상태 미확정 / 전송 증거 보유).
     * `is_deleted` 는 조건에 넣지 않는다 — 운영 claim 에 NULL 행이 존재해(V60 의 NOT NULL 이 운영에 미반영)
     * `= false` 비교가 대상 전건을 탈락시킨다. 기간별 클레임 보고서 조회도 이 컬럼을 보지 않아 정합이다.
     * 증거 필드의 blank 는 증거로 보지 않으므로 `trim(coalesce(...)) <> ''` 로 비교한다 — sync 가 조치 5필드를
     * 빈 값으로도 덮어쓰기 때문에 NULL 체크만으로는 `''` 가 걸러지지 않는다.
     */
    @Query(
        """
        select count(c) from Claim c
        where c.sfid is null
          and c.createdAt >= :promotionStartAt
          and (c.status is null or c.status in :promotableStatuses)
          and (
            c.interfaceDate is not null
            or trim(coalesce(c.cosmosKey, '')) <> ''
            or trim(coalesce(c.counselNumber, '')) <> ''
            or trim(coalesce(c.actionStatus, '')) <> ''
            or trim(coalesce(c.actionCode, '')) <> ''
            or trim(coalesce(c.reasonType, '')) <> ''
            or trim(coalesce(c.actContent, '')) <> ''
          )
        """,
    )
    fun countDeliveryStatusPromotionTargets(
        @Param("promotionStartAt") promotionStartAt: LocalDateTime,
        @Param("promotableStatuses") promotableStatuses: Collection<ClaimStatus>,
    ): Long

    /** 승격 대상 조회 — 조건은 [countDeliveryStatusPromotionTargets] 와 동일. id 오래된 순 [pageable] 상한. */
    @Query(
        """
        select c from Claim c
        where c.sfid is null
          and c.createdAt >= :promotionStartAt
          and (c.status is null or c.status in :promotableStatuses)
          and (
            c.interfaceDate is not null
            or trim(coalesce(c.cosmosKey, '')) <> ''
            or trim(coalesce(c.counselNumber, '')) <> ''
            or trim(coalesce(c.actionStatus, '')) <> ''
            or trim(coalesce(c.actionCode, '')) <> ''
            or trim(coalesce(c.reasonType, '')) <> ''
            or trim(coalesce(c.actContent, '')) <> ''
          )
        order by c.id asc
        """,
    )
    fun findDeliveryStatusPromotionTargets(
        @Param("promotionStartAt") promotionStartAt: LocalDateTime,
        @Param("promotableStatuses") promotableStatuses: Collection<ClaimStatus>,
        pageable: Pageable,
    ): List<Claim>
}
