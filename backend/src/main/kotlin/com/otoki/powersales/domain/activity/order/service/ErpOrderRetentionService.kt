package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 보관주기(6개월) 경과 ERP 주문/라인 정리 서비스.
 *
 * 레거시 `Batch_ERPOrderDel` + `Batch_ERPOrderProductDel` (`OrderDate__c < LAST_N_MONTHS:6`) 동등 — hard delete.
 * FK(`erp_order_product.erp_order_id`) 정합을 위해 **자식 라인 → 부모 헤더** 순서로 삭제한다.
 */
@Service
class ErpOrderRetentionService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository,
) {

    private val log = LoggerFactory.getLogger(ErpOrderRetentionService::class.java)

    /**
     * `order_date < (오늘 − 6개월)` 인 ERP 주문 라인/헤더를 영구 삭제한다.
     *
     * @return 삭제 결과 (기준일 + 삭제 건수)
     */
    @Transactional
    fun purgeExpired(): ErpOrderPurgeResult {
        val cutoff = LocalDate.now().minusMonths(RETENTION_MONTHS)
        val deletedLines = erpOrderProductRepository.deleteByErpOrderOrderDateBefore(cutoff)
        val deletedOrders = erpOrderRepository.deleteByOrderDateBefore(cutoff)
        log.info(
            "ERP_ORDER_RETENTION_PURGE cutoff={} deletedOrders={} deletedLines={}",
            cutoff, deletedOrders, deletedLines,
        )
        return ErpOrderPurgeResult(cutoff, deletedOrders, deletedLines)
    }

    companion object {
        /** 보관주기 (레거시 `LAST_N_MONTHS:6`). */
        const val RETENTION_MONTHS = 6L
    }
}

data class ErpOrderPurgeResult(
    val cutoff: LocalDate,
    val deletedOrders: Int,
    val deletedLines: Int,
)
