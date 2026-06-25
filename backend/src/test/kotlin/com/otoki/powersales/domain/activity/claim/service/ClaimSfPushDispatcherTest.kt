package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.event.ClaimRegisteredEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Async
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import kotlin.reflect.full.findAnnotation

/**
 * ClaimSfPushDispatcher 테스트 — 등록 직후 SF 송신 트리거의 계약.
 *
 * 핵심 계약: ① AFTER_COMMIT(커밋 후) + @Async(요청 스레드 분리), ② SF_PENDING 가드 전달,
 * ③ 디스패치 실패가 전파되지 않음(runCatching 격리 — HTTP 응답에 영향 없음).
 */
@DisplayName("ClaimSfPushDispatcher 테스트")
class ClaimSfPushDispatcherTest {

    private val dispatchService: ClaimSfDispatchService = mockk()
    private val dispatcher = ClaimSfPushDispatcher(dispatchService)

    @Test
    @DisplayName("AFTER_COMMIT + @Async 애너테이션 — 비동기 분리 계약 회귀 가드")
    fun listenerAnnotationContract() {
        val listener = ClaimSfPushDispatcher::onClaimRegistered
        assertThat(listener.findAnnotation<Async>())
            .withFailMessage("@Async 가 없으면 SF 지연이 등록 응답 스레드를 막는다")
            .isNotNull()
        assertThat(listener.findAnnotation<TransactionalEventListener>()?.phase)
            .withFailMessage("AFTER_COMMIT 이 아니면 등록 롤백 시에도 SF 가 전송될 수 있다")
            .isEqualTo(TransactionPhase.AFTER_COMMIT)
    }

    @Test
    @DisplayName("SF_PENDING 만 허용하는 가드로 dispatch 위임")
    fun delegatesWithPendingGuard() {
        val allowedSlot = slot<Set<ClaimStatus>>()
        every { dispatchService.dispatch(7L, capture(allowedSlot), any()) } returns null

        dispatcher.onClaimRegistered(ClaimRegisteredEvent(7L))

        assertThat(allowedSlot.captured).containsExactly(ClaimStatus.SF_PENDING)
        verify { dispatchService.dispatch(7L, any(), any()) }
    }

    @Test
    @DisplayName("dispatch 예외는 격리 — 트리거 밖으로 전파되지 않음")
    fun swallowsDispatchException() {
        every { dispatchService.dispatch(any(), any(), any()) } throws RuntimeException("SF down")

        assertThatCode { dispatcher.onClaimRegistered(ClaimRegisteredEvent(7L)) }
            .doesNotThrowAnyException()
    }
}
