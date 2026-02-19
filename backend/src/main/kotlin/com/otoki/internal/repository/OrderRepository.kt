/*
package com.otoki.internal.repository

import com.otoki.internal.entity.ApprovalStatus
import com.otoki.internal.entity.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/ **
 * 주문 Repository
 * /
@Repository
interface OrderRepository : JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    / **
     * 사용자별 주문 조회 (동적 필터링 + 페이지네이션)
     * 모든 필터는 선택적 (null이면 조건 미적용)
     * /
    @Query(
        "SELECT o FROM Order o " +
        "JOIN FETCH o.store s " +
        "WHERE o.user.id = :userId " +
        "AND (:storeId IS NULL OR o.store.id = :storeId) " +
        "AND (:status IS NULL OR o.approvalStatus = :status) " +
        "AND (:deliveryDateFrom IS NULL OR o.deliveryDate >= :deliveryDateFrom) " +
        "AND (:deliveryDateTo IS NULL OR o.deliveryDate <= :deliveryDateTo)",
        countQuery = "SELECT COUNT(o) FROM Order o " +
        "WHERE o.user.id = :userId " +
        "AND (:storeId IS NULL OR o.store.id = :storeId) " +
        "AND (:status IS NULL OR o.approvalStatus = :status) " +
        "AND (:deliveryDateFrom IS NULL OR o.deliveryDate >= :deliveryDateFrom) " +
        "AND (:deliveryDateTo IS NULL OR o.deliveryDate <= :deliveryDateTo)"
    )
    fun findByUserIdWithFilters(
        @Param("userId") userId: Long,
        @Param("storeId") storeId: Long?,
        @Param("status") status: ApprovalStatus?,
        @Param("deliveryDateFrom") deliveryDateFrom: LocalDate?,
        @Param("deliveryDateTo") deliveryDateTo: LocalDate?,
        pageable: Pageable
    ): Page<Order>
}
*/
