package com.otoki.powersales.domain.activity.order.event

/**
 * 주문 등록 SAP 송신(SD03050) 트리거 이벤트.
 *
 * 발행: [com.otoki.powersales.domain.activity.order.service.OrderRequestCreateService] 가
 *       `sap_outbox` 적재 직후 발행.
 * 수신: [com.otoki.powersales.domain.activity.order.sap.OrderRequestRegisterDispatcher]
 *       (`@TransactionalEventListener(AFTER_COMMIT) + @Async`).
 *
 * 의미: SAP outbox 워커(`SapOutboxBatch`) 스케줄러가 비활성 상태여도 주문 등록 즉시 SD03050 이
 * 호출되도록 커밋 후 송신을 트리거한다. 송신 실패 시 outbox row 는 RETRY/SEND_FAILED 로 남아
 * 재전송(F18)/스케줄러 재활성 시 복구 가능하다.
 */
data class OrderRequestRegisteredEvent(
    val outboxId: Long,
)
