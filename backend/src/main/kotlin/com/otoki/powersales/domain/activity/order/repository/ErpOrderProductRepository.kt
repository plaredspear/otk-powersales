package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderProductRepository : JpaRepository<ErpOrderProduct, Long>, ErpOrderProductRepositoryCustom {

    fun findByExternalKey(externalKey: String): ErpOrderProduct?

    /**
     * 대량 UPSERT 시 라인 개별 조회(N+1)를 IN 절 1회로 대체하기 위한 일괄 조회.
     * [com.otoki.powersales.domain.activity.order.service.ErpOrderUpsertService.upsert] 참조.
     */
    fun findByExternalKeyIn(externalKeys: Collection<String>): List<ErpOrderProduct>

    fun findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber: String): List<ErpOrderProduct>

    /**
     * 여러 SAP 주문번호의 라인을 IN 절 1회로 일괄 조회.
     * 내 주문 상세 처리현황이 총납품수량(ConfirmQuantity_Box)을 주입할 때 사용
     * ([com.otoki.powersales.domain.activity.order.service.OrderRequestService] 참조).
     */
    fun findBySapOrderNumberIn(sapOrderNumbers: Collection<String>): List<ErpOrderProduct>
}
