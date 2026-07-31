package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import com.otoki.powersales.domain.activity.suggestion.event.SuggestionRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 제안/물류클레임 등록 직후 SF `/ProposalRegist` 송신 트리거 (admin 등록 경로 전용).
 *
 * - `@TransactionalEventListener(AFTER_COMMIT)`: 등록 트랜잭션 커밋 후 실행 — suggestion + UploadFile 이
 *   영속화되고 S3 사진도 업로드된 뒤 송신하므로 "SF 성공·DB 롤백" 불일치가 없다.
 * - `@Async`: admin HTTP 응답 스레드와 분리 — SF 지연이 등록 응답 시간을 막지 않는다.
 *
 * 실제 송신/상태갱신(SENT·SEND_FAILED)은 [SuggestionSfResendService.resend] 가 수행한다 — DB snapshot
 * 기반 payload 복원이라 등록 직후에도 그대로 재사용 가능하며, 실패 시 SEND_FAILED 로 남아 재전송 배치
 * (`sf-claim-resend`) 대상이 된다. 클레임의 [com.otoki.powersales.domain.activity.claim.service.ClaimSfPushDispatcher] 정합.
 */
@Component
class SuggestionSfPushDispatcher(
    private val resendService: SuggestionSfResendService,
) {

    private val log = LoggerFactory.getLogger(SuggestionSfPushDispatcher::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSuggestionRegistered(event: SuggestionRegisteredEvent) {
        runCatching {
            // PENDING 이 아닌(이미 전송된) 건이면 resend 내부 상태 가드가 skip 처리한다.
            resendService.resend(
                suggestionId = event.suggestionId,
                allowedStatuses = setOf(SuggestionSfSendStatus.PENDING),
            )
        }.onFailure { log.warn("제안 등록 SF 송신(/ProposalRegist) 트리거 실패 suggestionId=${event.suggestionId}", it) }
    }
}
