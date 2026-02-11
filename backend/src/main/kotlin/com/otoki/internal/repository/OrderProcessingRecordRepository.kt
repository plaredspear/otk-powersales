package com.otoki.internal.repository

import com.otoki.internal.entity.OrderProcessingRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 주문 처리 현황 Repository (SAP 연동 데이터)
 */
@Repository
interface OrderProcessingRecordRepository : JpaRepository<OrderProcessingRecord, Long> {

    fun findByOrderId(orderId: Long): List<OrderProcessingRecord>

    /**
     * 거래처 + 납기일 기준으로 SAP 주문번호별 그룹핑 조회
     * 거래처별 주문 목록 조회용 (F28)
     *
     * @return [sapOrderNumber, clientId, clientName, totalAmount] 배열 목록
     */
    @Query(
        """
        SELECT opr.sapOrderNumber, o.store.id, o.store.storeName, SUM(oi.amount)
        FROM OrderProcessingRecord opr
        JOIN opr.order o
        JOIN o.store s
        JOIN OrderItem oi ON oi.order = o AND oi.productCode = opr.productCode
        WHERE o.store.id = :storeId
        AND o.deliveryDate = :deliveryDate
        GROUP BY opr.sapOrderNumber, o.store.id, o.store.storeName
        ORDER BY opr.sapOrderNumber DESC
        """
    )
    fun findClientOrderSummaries(
        @Param("storeId") storeId: Long,
        @Param("deliveryDate") deliveryDate: LocalDate
    ): List<Array<Any>>

    /**
     * SAP 주문번호로 처리 기록 목록 조회
     * 거래처별 주문 상세 조회용 (F28)
     */
    fun findBySapOrderNumber(sapOrderNumber: String): List<OrderProcessingRecord>
}
