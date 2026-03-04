/* Order 모듈 전체 비활성화 — DB 테이블 미존재
package com.otoki.internal.order.repository

import com.otoki.internal.order.entity.OrderProcessingRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 주문 처리 현황 Repository (SAP 연동 데이터)
 */
@Repository
interface OrderProcessingRecordRepository : JpaRepository<OrderProcessingRecord, Long> {

    fun findByOrderId(orderId: Long): List<OrderProcessingRecord>

    // TODO: OrderItem 엔티티 활성화 시 복원
    // fun findClientOrderSummaries(...)

    /**
     * SAP 주문번호로 처리 기록 목록 조회
     * 거래처별 주문 상세 조회용 (F28)
     */
    fun findBySapOrderNumber(sapOrderNumber: String): List<OrderProcessingRecord>
}
*/
