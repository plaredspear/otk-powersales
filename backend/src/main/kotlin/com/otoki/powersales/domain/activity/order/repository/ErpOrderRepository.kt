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

    /**
     * 참조 주문번호(`ref_sap_order_number`)로 역참조 주문(취소/변경 등)을 조회한다.
     *
     * SAP 은 원본 주문번호를 선행 0 한 자리로 zero-pad 하여 참조 필드에 적재하므로
     * (헤더의 `sap_order_number` 는 unpadded 저장), 호출부는 padded/unpadded 양쪽 후보를 IN 절로 넘긴다
     * ([com.otoki.powersales.domain.activity.order.service.ClientOrderQueryService.getClientOrderDetail] 참조).
     */
    fun findByRefSapOrderNumberIn(refSapOrderNumbers: Collection<String>): List<ErpOrder>
}
