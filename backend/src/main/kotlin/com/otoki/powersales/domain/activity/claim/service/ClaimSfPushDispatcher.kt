package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.enums.ClaimSfSendStatus
import com.otoki.powersales.domain.activity.claim.event.ClaimRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.stereotype.Component

/**
 * 클레임 등록 직후 SF `/ClaimRegist` 송신 트리거.
 *
 * - `@TransactionalEventListener(AFTER_COMMIT)`: 등록 트랜잭션 커밋 후 실행 — claim + photo 가 영속화되고
 *   S3 이미지도 업로드된 뒤 송신하므로 "SF 성공·DB 롤백" 불일치가 없다.
 * - `@Async`: 메인 요청 스레드(모바일/web HTTP 응답)와 분리 — SF 지연이 등록 응답 시간을 막지 않는다.
 *
 * 실제 송신/응답검증/상태갱신(SENT·SEND_FAILED)은 [ClaimSfDispatchService.dispatch] 가 수행하며,
 * 수동 재전송([AdminClaimResendService])과 동일 경로를 공유한다. SF 실패 시 claim 은 SEND_FAILED 로
 * 보존되어 web 상세 화면에서 수동 재전송할 수 있다.
 */
@Component
class ClaimSfPushDispatcher(
    private val dispatchService: ClaimSfDispatchService,
) {

    private val log = LoggerFactory.getLogger(ClaimSfPushDispatcher::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onClaimRegistered(event: ClaimRegisteredEvent) {
        runCatching {
            dispatchService.dispatch(
                claimId = event.claimId,
                allowedStatuses = setOf(ClaimSfSendStatus.PENDING),
                // 이미 전송된(PENDING 이 아닌) claim 이면 중복 전송하지 않고 skip.
                onStatusMismatch = { status ->
                    log.warn("클레임 SF 송신 트리거 skip — claimId={} sfSendStatus={}", event.claimId, status)
                },
            )
        }.onFailure { log.warn("클레임 등록 SF 송신(/ClaimRegist) 트리거 실패 claimId=${event.claimId}", it) }
    }
}
