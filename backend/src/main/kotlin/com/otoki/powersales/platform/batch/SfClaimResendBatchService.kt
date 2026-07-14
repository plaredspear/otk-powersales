package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.claim.enums.ClaimSfSendStatus
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.domain.activity.claim.service.ClaimSfDispatchService
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.domain.activity.suggestion.service.SuggestionSfResendService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * SF 전송실패(SEND_FAILED) 건 재전송 배치 실행 서비스 — 제품클레임 + 물류클레임(제안) 통합.
 *
 * 두 도메인이 동일한 SF 서버(`SfOutboundClient` / OAuth 토큰)를 공유하므로 한 잡에서 순차 처리해 SF 부하를
 * 자연 직렬화한다. 도메인 간 실패는 [runCatching] 으로 격리 — 한 도메인의 조회/루프 예외가 다른 도메인
 * 처리를 막지 않는다(건별 SF 실패는 dispatch/resend 가 이미 흡수해 SEND_FAILED 로 보존).
 *
 * 재전송 대상: `상태 = SEND_FAILED AND 시도횟수 < maxAttempt`. 영구 실패(예: SF Apex 미배포로 strict
 * 파싱 실패)가 매 배치마다 재시도되어 이력을 오염시키지 않도록 시도횟수 상한을 건다.
 *
 * 개별 전송/상태전이는 각 도메인 서비스에 위임한다:
 *  - 제품클레임: [ClaimSfDispatchService.dispatch] (등록 직후 비동기 전송과 동일 경로 공유)
 *  - 물류클레임: [SuggestionSfResendService.resend]
 */
@Service
class SfClaimResendBatchService(
    private val claimRepository: ClaimRepository,
    private val claimSfDispatchService: ClaimSfDispatchService,
    private val suggestionRepository: SuggestionRepository,
    private val suggestionSfResendService: SuggestionSfResendService,
    @Value("\${app.sf.resend.max-attempt:3}") private val maxAttempt: Int,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun resendAll(context: ScheduledJobRunContext? = null) {
        val claim = runCatching { resendClaims() }
            .onFailure { log.warn("[sf-resend] 제품클레임 재전송 단계 실패", it) }
            .getOrDefault(DomainResult.error())

        val suggestion = runCatching { resendSuggestions() }
            .onFailure { log.warn("[sf-resend] 물류클레임 재전송 단계 실패", it) }
            .getOrDefault(DomainResult.error())

        log.info(
            "SF_RESEND_BATCH maxAttempt={} claim(target={} sent={} failed={} skipped={} error={}) suggestion(target={} sent={} failed={} skipped={} error={})",
            maxAttempt,
            claim.target, claim.sent, claim.failed, claim.skipped, claim.error,
            suggestion.target, suggestion.sent, suggestion.failed, suggestion.skipped, suggestion.error,
        )
        context?.metadata(
            mapOf(
                "maxAttempt" to maxAttempt,
                "claim" to claim.toMetadata(),
                "suggestion" to suggestion.toMetadata(),
            )
        )
    }

    private fun resendClaims(): DomainResult {
        val targetIds = claimRepository.findResendTargetIds(ClaimSfSendStatus.SEND_FAILED, maxAttempt)
        var sent = 0
        var failed = 0
        var skipped = 0
        for (id in targetIds) {
            // dispatch 는 SF 실패를 throw 하지 않고 sfSendStatus 로 반영하나, 조회/트랜잭션 예외는 건별 격리한다.
            val outcome = runCatching {
                claimSfDispatchService.dispatch(
                    claimId = id,
                    allowedStatuses = setOf(ClaimSfSendStatus.SEND_FAILED),
                    onStatusMismatch = { /* 배치 조회 후 상태 변동(이미 SENT 등) — skip */ },
                )
            }.onFailure { log.warn("[sf-resend] 제품클레임 재전송 실패 claimId={}", id, it) }

            when {
                // dispatch 가 null = 상태 가드 mismatch(이미 SENT 등) → skip (실패 아님).
                outcome.isSuccess && outcome.getOrNull() == null -> skipped++
                outcome.getOrNull()?.sfSendStatus == ClaimSfSendStatus.SENT -> sent++
                else -> failed++
            }
        }
        return DomainResult(target = targetIds.size, sent = sent, failed = failed, skipped = skipped)
    }

    private fun resendSuggestions(): DomainResult {
        val targetIds = suggestionRepository.findResendTargetIds(SuggestionSfSendStatus.SEND_FAILED, maxAttempt)
        var sent = 0
        var failed = 0
        var skipped = 0
        for (id in targetIds) {
            val outcome = runCatching {
                suggestionSfResendService.resend(id)
            }.onFailure { log.warn("[sf-resend] 물류클레임 재전송 실패 suggestionId={}", id, it) }

            when {
                // resend 가 null = 상태 가드 mismatch(이미 SENT 등) → skip (실패 아님).
                outcome.isSuccess && outcome.getOrNull() == null -> skipped++
                outcome.getOrNull()?.success == true -> sent++
                else -> failed++
            }
        }
        return DomainResult(target = targetIds.size, sent = sent, failed = failed, skipped = skipped)
    }

    /** 도메인별 재전송 집계. [skipped] = 상태 가드 mismatch(이미 전송됨) 로 건너뜀. [error] = 도메인 조회/루프 단계 자체가 예외. */
    private data class DomainResult(
        val target: Int,
        val sent: Int,
        val failed: Int,
        val skipped: Int = 0,
        val error: Boolean = false,
    ) {
        fun toMetadata(): Map<String, Any?> =
            mapOf("target" to target, "sent" to sent, "failed" to failed, "skipped" to skipped, "error" to error)

        companion object {
            fun error() = DomainResult(target = 0, sent = 0, failed = 0, skipped = 0, error = true)
        }
    }
}
