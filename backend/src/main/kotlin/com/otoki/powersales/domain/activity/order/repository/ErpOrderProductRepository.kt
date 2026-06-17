package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ErpOrderProductRepository : JpaRepository<ErpOrderProduct, Long> {

    fun findByExternalKey(externalKey: String): ErpOrderProduct?

    fun findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber: String): List<ErpOrderProduct>

    /**
     * 보관주기 경과 ERP 주문 라인 hard delete (레거시 `Batch_ERPOrderProductDel` — `OrderDate__c < LAST_N_MONTHS:6`).
     *
     * 레거시는 라인 자체 `OrderDate__c` 기준이었으나 신규 `erp_order_product` 에는 order_date 컬럼이 없어
     * 부모(`erp_order.order_date`) 기준 하위집합으로 일원화한다 — 고아 헤더/부분 라인 잔존을 모두 방지.
     * FK 정합을 위해 부모 헤더 삭제보다 먼저 호출해야 한다.
     */
    @Modifying
    @Query(
        """
        DELETE FROM ErpOrderProduct p
        WHERE p.erpOrder.id IN (SELECT o.id FROM ErpOrder o WHERE o.orderDate < :cutoff)
        """
    )
    fun deleteByErpOrderOrderDateBefore(@Param("cutoff") cutoff: LocalDate): Int
}
