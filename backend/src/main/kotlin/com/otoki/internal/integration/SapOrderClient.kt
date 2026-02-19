/*
package com.otoki.internal.integration

import com.otoki.internal.entity.Order

/ **
 * SAP/Orora 주문 전송 클라이언트 인터페이스
 *
 * 실제 SAP 연동이 구현되면 이 인터페이스의 구현체를 교체합니다.
 * /
interface SapOrderClient {

    / **
     * SAP/Orora 시스템으로 주문 데이터를 전송합니다.
     *
     * @param order 전송할 주문
     * @return 전송 결과
     * /
    fun sendOrder(order: Order): SapOrderResult
}

/ **
 * SAP 주문 전송 결과
 * /
data class SapOrderResult(
    val success: Boolean,
    val failureReason: String? = null
)
*/
