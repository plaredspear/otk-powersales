package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ErpOrderRepository : JpaRepository<ErpOrder, Long> {

    fun findBySapOrderNumber(sapOrderNumber: String): ErpOrder?

    /**
     * 거래처별 출하 주문 목록 조회 (Spec #593 — 거래처별 주문 탭).
     *
     * 레거시 Heroku `OrderController#clientlistapi` → SF `ClientOrderSearch` 와 동등.
     * 거래처(account) 단위로 조회하며 담당자(employee) 필터는 적용하지 않는다
     * (특정 거래처에 대한 모든 출하 주문 = 내 주문 + 타 영업사원 주문 포함).
     * `deliveryDate` 가 null 이면 납기일 제한 없이 전체 조회한다.
     */
    @Query(
        """
        SELECT o FROM ErpOrder o
        WHERE o.account.id = :accountId
          AND (CAST(:deliveryDate AS date) IS NULL OR o.deliveryRequestDate = :deliveryDate)
          AND (o.isDeleted IS NULL OR o.isDeleted = false)
        """
    )
    fun findClientOrders(
        @Param("accountId") accountId: Long,
        @Param("deliveryDate") deliveryDate: LocalDate?,
        pageable: Pageable
    ): Page<ErpOrder>
}
