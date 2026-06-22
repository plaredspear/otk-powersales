package com.otoki.powersales.domain.activity.order.sap

import com.otoki.powersales.domain.activity.order.event.OrderRequestRegisteredEvent
import com.otoki.powersales.external.sap.outbox.SapOutboxBatchService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 주문 등록 직후 SAP SD03050 송신 트리거.
 *
 * - `@TransactionalEventListener(AFTER_COMMIT)`: 주문 등록 트랜잭션 커밋 후 실행 — order_request/outbox
 *   가 영속화된 뒤 송신하므로 "SAP 성공·DB 롤백" 불일치가 없다.
 * - `@Async`: 메인 요청 스레드(모바일 HTTP 응답)와 분리 — SAP 지연이 주문 응답 시간을 막지 않는다.
 *
 * 실제 송신/응답검증/상태갱신(APPROVED·SEND_FAILED)/재시도는 [SapOutboxBatchService.processOne] 가
 * 수행한다. 본 디스패처는 스케줄러(`SapOutboxBatch`) 비활성 상태에서도 주문 등록 즉시 SD03050 이
 * 호출되도록 그 처리를 트리거할 뿐이다.
 */
@Component
class OrderRequestRegisterDispatcher(
    private val sapOutboxBatchService: SapOutboxBatchService,
) {

    private val log = LoggerFactory.getLogger(OrderRequestRegisterDispatcher::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderRequestRegistered(event: OrderRequestRegisteredEvent) {
        runCatching { sapOutboxBatchService.processOne(event.outboxId) }
            .onFailure { log.warn("주문 등록 SAP 송신(SD03050) 트리거 실패 outboxId=${event.outboxId}", it) }
    }
}
