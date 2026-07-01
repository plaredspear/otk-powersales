package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderRepository : JpaRepository<ErpOrder, Long>, ErpOrderRepositoryCustom {

    fun findBySapOrderNumber(sapOrderNumber: String): ErpOrder?

    /**
     * 대량 UPSERT 시 헤더 개별 조회(N+1)를 IN 절 1회로 대체하기 위한 일괄 조회.
     * [com.otoki.powersales.domain.activity.order.service.ErpOrderUpsertService.upsert] 참조.
     */
    fun findBySapOrderNumberIn(sapOrderNumbers: Collection<String>): List<ErpOrder>
}
