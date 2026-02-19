/*
package com.otoki.internal.repository

import com.otoki.internal.entity.OrderItem
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/ **
 * 주문 제품 항목 Repository
 * /
@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {

    fun findByOrderId(orderId: Long): List<OrderItem>

    / **
     * 주문이력 제품 집계 (제품별 마지막 주문일, 주문 횟수)
     * Product를 JOIN하여 barcode, storageType, category 정보도 함께 조회
     * /
    @Query(
        "SELECT oi.productCode, oi.productName, p.barcode, p.storageType, " +
        "p.categoryMid, p.categorySub, MAX(o.orderDate), COUNT(DISTINCT o.id) " +
        "FROM OrderItem oi " +
        "JOIN oi.order o " +
        "LEFT JOIN Product p ON p.productCode = oi.productCode " +
        "WHERE o.user.id = :userId " +
        "AND o.orderDate BETWEEN :dateFrom AND :dateTo " +
        "AND oi.isCancelled = false " +
        "GROUP BY oi.productCode, oi.productName, p.barcode, p.storageType, p.categoryMid, p.categorySub " +
        "ORDER BY MAX(o.orderDate) DESC"
    )
    fun findOrderHistoryProducts(
        @Param("userId") userId: Long,
        @Param("dateFrom") dateFrom: LocalDate,
        @Param("dateTo") dateTo: LocalDate,
        pageable: Pageable
    ): List<Array<Any>>

    / **
     * 주문이력 제품 총 건수 (페이지네이션용)
     * /
    @Query(
        "SELECT COUNT(DISTINCT oi.productCode) " +
        "FROM OrderItem oi " +
        "JOIN oi.order o " +
        "WHERE o.user.id = :userId " +
        "AND o.orderDate BETWEEN :dateFrom AND :dateTo " +
        "AND oi.isCancelled = false"
    )
    fun countOrderHistoryProducts(
        @Param("userId") userId: Long,
        @Param("dateFrom") dateFrom: LocalDate,
        @Param("dateTo") dateTo: LocalDate
    ): Long
}
*/
