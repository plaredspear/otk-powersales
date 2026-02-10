package com.otoki.internal.repository

import com.otoki.internal.entity.OrderProcessingRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 주문 처리 현황 Repository (SAP 연동 데이터)
 */
@Repository
interface OrderProcessingRecordRepository : JpaRepository<OrderProcessingRecord, Long> {

    fun findByOrderId(orderId: Long): List<OrderProcessingRecord>
}
