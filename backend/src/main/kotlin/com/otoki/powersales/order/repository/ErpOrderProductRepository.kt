package com.otoki.powersales.order.repository

import com.otoki.powersales.order.entity.ErpOrderProduct
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ErpOrderProductRepository : JpaRepository<ErpOrderProduct, Long> {

    fun findByExternalKey(externalKey: String): ErpOrderProduct?

    fun findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber: String): List<ErpOrderProduct>

    /**
     * 거래처 주문이력(제품 선택용) — 레거시 SF `OrderHistory` 정합.
     * 거래처(sapAccountCode) + 영업사원(employeeCode) + 주문일(orderDate) 범위로 조회한다.
     * 그룹핑/중복제거는 서비스에서 수행한다.
     */
    @Query(
        """
        select new com.otoki.powersales.order.repository.OrderHistoryRow(
            o.orderDate, p.productCode, p.productName
        )
        from ErpOrderProduct p
        join p.erpOrder o
        where o.sapAccountCode = :accountCode
          and o.employeeCode = :employeeCode
          and o.orderDate between :startDate and :endDate
          and (o.isDeleted is null or o.isDeleted = false)
          and p.productCode is not null
        order by o.orderDate desc, p.productName asc
        """
    )
    fun findOrderHistory(
        @Param("accountCode") accountCode: String,
        @Param("employeeCode") employeeCode: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<OrderHistoryRow>
}

/** 거래처 주문이력 조회 행 (주문일 + 제품). */
data class OrderHistoryRow(
    val orderDate: LocalDate?,
    val productCode: String?,
    val productName: String?
)
