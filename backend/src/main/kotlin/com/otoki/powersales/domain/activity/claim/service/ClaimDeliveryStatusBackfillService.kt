package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스모스 전송상태 파생 승격 **존량 백필** — [ClaimDeliveryStatusRule] 을 기존 claim 행에 소급 적용한다.
 *
 * 클레임 마스터 sync 가 Status 를 회수하기 전(2026-09-08 이전)에 등록돼 '임시저장' 으로 잔류 중인 건 중,
 * 조치/처리 정보가 이미 들어와 있어 코스모스 전송이 확인되는 건을 '전송완료' 로 올린다. 이 값이 없으면
 * 기간별 클레임 보고서(status='전송완료' 필터)에서 통째로 누락된다.
 *
 * ## 운영 특성
 * - **멱등**: 승격된 행은 status='전송완료' 가 되어 다음 실행의 조회 대상에서 자연히 빠진다. INSERT 없음.
 * - **분할 실행**: 한 번에 [DEFAULT_LIMIT] (최대 [MAX_LIMIT]) 건씩 id 오래된 순으로 처리한다.
 *   `remaining` 이 0 이 될 때까지 반복 호출하면 된다.
 * - **실행 주체**: [ClaimDeliveryStatusBackfillRunner] 가 부팅 시 `remaining` 이 0 이 될 때까지 호출한다.
 *
 * 향후 신규 등록분은 sync 경로([AdminClaimMasterSyncTestService.applyUpdates])가 같은 규칙을 태우므로
 * 본 백필은 존량 정리 용도다.
 */
@Service
class ClaimDeliveryStatusBackfillService(
    private val claimRepository: ClaimRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 현재 승격 대상 건수 (조회 전용 — preview). */
    @Transactional(readOnly = true)
    fun countTargets(): Long = claimRepository.countDeliveryStatusPromotionTargets(
        ClaimDeliveryStatusRule.PROMOTION_START_AT,
        ClaimDeliveryStatusRule.PROMOTABLE_STATUSES,
    )

    /**
     * 승격 대상 최대 [limit] 건을 '전송완료' 로 올린다.
     *
     * 조회 조건과 [ClaimDeliveryStatusRule.promoteIfDelivered] 판정을 이중으로 통과한 건만 갱신하므로,
     * 쿼리 조건이 규칙보다 넓어지더라도 규칙이 최종 게이트가 된다.
     */
    @Transactional
    fun backfill(limit: Int = DEFAULT_LIMIT): BackfillResult {
        val effectiveLimit = limit.coerceIn(1, MAX_LIMIT)
        // 갱신 전에 총량을 먼저 센다 — 승격 후 세면 Hibernate auto-flush 로 이미 빠진 행까지 다시 빼게 된다.
        val total = countTargets()
        val targets = claimRepository.findDeliveryStatusPromotionTargets(
            ClaimDeliveryStatusRule.PROMOTION_START_AT,
            ClaimDeliveryStatusRule.PROMOTABLE_STATUSES,
            PageRequest.of(0, effectiveLimit),
        )
        val promoted = targets.count { ClaimDeliveryStatusRule.promoteIfDelivered(it) }
        val remaining = (total - promoted).coerceAtLeast(0)

        log.info(
            "[claim-delivery-status-backfill] scanned={} promoted={} remaining={} cutoff={}",
            targets.size, promoted, remaining, ClaimDeliveryStatusRule.PROMOTION_START_DATE,
        )
        return BackfillResult(scanned = targets.size, promoted = promoted, remaining = remaining)
    }

    /**
     * @property scanned 이번 실행에서 조회한 대상 건수
     * @property promoted 실제로 '전송완료' 로 올린 건수
     * @property remaining 이번 실행 후 남은 대상 건수 (0 이 될 때까지 재실행)
     */
    data class BackfillResult(
        val scanned: Int,
        val promoted: Int,
        val remaining: Long,
    )

    companion object {
        const val DEFAULT_LIMIT = 1000
        const val MAX_LIMIT = 5000
    }
}
