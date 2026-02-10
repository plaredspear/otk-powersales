package com.otoki.internal.integration

import com.otoki.internal.entity.Order
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * SAP 주문 전송 Stub 구현체
 *
 * 실제 SAP 연동이 구현되기 전까지 항상 성공을 반환합니다.
 * 실제 구현 시 이 클래스를 실제 SAP API 호출 구현체로 교체합니다.
 */
@Component
class StubSapOrderClient : SapOrderClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendOrder(order: Order): SapOrderResult {
        log.info(
            "[SAP Stub] 주문 전송 (orderRequestNumber={}, storeCode={}, itemCount={})",
            order.orderRequestNumber,
            order.store.storeCode,
            order.items.size
        )
        return SapOrderResult(success = true)
    }
}
