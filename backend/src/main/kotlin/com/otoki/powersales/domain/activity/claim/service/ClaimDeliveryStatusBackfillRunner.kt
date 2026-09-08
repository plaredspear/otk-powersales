package com.otoki.powersales.domain.activity.claim.service

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * 코스모스 전송상태 파생 승격 **부팅 catch-up** — [ClaimDeliveryStatusBackfillService] 존량 처리.
 *
 * ## 배경
 * 클레임 마스터 sync 가 Status 를 회수하기 전에 등록된 클레임이 '임시저장' 으로 잔류해, 기간별 클레임
 * 보고서(status='전송완료' 필터)에서 통째로 누락됐다. sync 경로에는 같은 규칙이 이미 걸려 있지만
 * ([AdminClaimMasterSyncTestService.applyUpdates]) 그 경로는 `MOD_DT=오늘` 변경분만 훑으므로 이미 쌓인
 * 존량은 따라잡지 못한다. 본 Runner 가 그 존량을 부팅 시 1회 정리한다.
 *
 * ## 운영 특성
 * - **멱등**: 승격된 행은 status='전송완료' 가 되어 다음 부팅의 조회 대상에서 빠진다. 정상 상태에서는
 *   빈 결과 SELECT 1회로 끝난다. INSERT 없이 기존 행 UPDATE 만 한다.
 * - **분할 처리**: [ClaimDeliveryStatusBackfillService.DEFAULT_LIMIT] 씩 트랜잭션을 나눠 반복하고,
 *   무한 루프 방지를 위해 [MAX_ROUNDS] 회에서 멈춘다(남은 건은 다음 부팅이 이어받는다).
 * - **부팅 비차단**: 실패해도 예외를 삼킨다 — 보고서 표시용 보정이라 부팅을 막을 이유가 없고, 다음
 *   부팅/다음 sync 가 따라잡는다.
 * - **다중 인스턴스**: 동시 부팅 시 같은 대상을 각자 UPDATE 해도 결과값이 같아(SENT) 별도 락을 두지 않는다.
 * - `local` 은 시드 데이터뿐이라 제외한다.
 */
@Component
@Profile("dev | prod")
@Order(200)
class ClaimDeliveryStatusBackfillRunner(
    private val backfillService: ClaimDeliveryStatusBackfillService,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        var promotedTotal = 0
        var rounds = 0
        try {
            while (rounds < MAX_ROUNDS) {
                val result = backfillService.backfill()
                rounds++
                promotedTotal += result.promoted
                // 대상 소진(scanned=0) 또는 규칙 게이트로 한 건도 못 올린 경우 — 더 돌아도 진전이 없다.
                if (result.scanned == 0 || result.promoted == 0) break
                if (result.remaining <= 0) break
            }
        } catch (e: Exception) {
            log.warn("클레임 전송상태 파생 승격 catch-up 실패 — 다음 부팅에서 재시도: {}", e.message, e)
            return
        }
        if (promotedTotal > 0 || rounds > 1) {
            log.info(
                "클레임 전송상태 파생 승격 catch-up 완료: promoted={}, rounds={}, cutoff={}",
                promotedTotal, rounds, ClaimDeliveryStatusRule.PROMOTION_START_DATE,
            )
        }
    }

    companion object {
        /** 한 부팅에서 반복할 최대 라운드 (라운드당 [ClaimDeliveryStatusBackfillService.DEFAULT_LIMIT] 건). */
        private const val MAX_ROUNDS = 20
    }
}
