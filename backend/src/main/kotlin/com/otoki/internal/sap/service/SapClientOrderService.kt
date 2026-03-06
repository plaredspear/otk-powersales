package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapClientOrderRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.ErpOrder
import com.otoki.internal.sap.entity.ErpOrderProduct
import com.otoki.internal.sap.repository.ErpOrderProductRepository
import com.otoki.internal.sap.repository.ErpOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapClientOrderService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository
) : SapSyncService<SapClientOrderRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(items: List<SapClientOrderRequest.ReqItem>): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        items.forEachIndexed { index, item ->
            try {
                syncOrderWithDetails(item)
                successCount++
            } catch (e: Exception) {
                log.warn("주문 동기화 실패: index={}, sapOrderNumber={}, error={}",
                    index, item.sapOrderNumber, e.message)
                errors.add(
                    SapSyncError(
                        index = index,
                        field = "sap_order_number",
                        value = item.sapOrderNumber,
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        return SapSyncResult(
            successCount = successCount,
            failCount = errors.size,
            errors = errors
        )
    }

    private fun syncOrderWithDetails(item: SapClientOrderRequest.ReqItem) {
        val order = syncOrderHeader(item)
        erpOrderRepository.flush()

        item.itemDetailList.forEach { detail ->
            syncOrderDetail(detail, order)
        }
    }

    private fun syncOrderHeader(item: SapClientOrderRequest.ReqItem): ErpOrder {
        val sapOrderNumber = item.sapOrderNumber
            ?: throw IllegalArgumentException("sap_order_number is required")

        val existing = erpOrderRepository.findBySapOrderNumber(sapOrderNumber)
        val now = LocalDateTime.now()

        return if (existing != null) {
            mapHeaderFields(existing, item)
            existing.updatedAt = now
            erpOrderRepository.save(existing)
        } else {
            val order = ErpOrder(sapOrderNumber = sapOrderNumber)
            mapHeaderFields(order, item)
            erpOrderRepository.save(order)
        }
    }

    private fun mapHeaderFields(order: ErpOrder, item: SapClientOrderRequest.ReqItem) {
        order.sapAccountCode = item.sapAccountCode
        order.sapAccountName = item.sapAccountName
        order.deliveryRequestDate = item.deliveryRequestDate
        order.orderDate = item.orderDate
        order.employeeCode = item.employeeCode
        order.employeeName = item.employeeName
        order.orderSalesAmount = parseDouble(item.orderSalesAmount)
        order.orderChannel = item.orderChannel
        order.orderChannelNm = item.orderChannelNm
        order.orderType = item.orderType
        order.orderTypeNm = item.orderTypeNm
    }

    private fun syncOrderDetail(detail: SapClientOrderRequest.ItemDetail, order: ErpOrder): ErpOrderProduct {
        val sapOrderNumber = detail.sapOrderNumber
            ?: throw IllegalArgumentException("item sap_order_number is required")
        val lineNumber = detail.lineNumber
            ?: throw IllegalArgumentException("line_number is required")

        val externalKey = if (!detail.shippingVehicle.isNullOrBlank()) {
            sapOrderNumber + lineNumber + detail.shippingVehicle
        } else {
            sapOrderNumber + lineNumber
        }

        val existing = erpOrderProductRepository.findByExternalKey(externalKey)
        val now = LocalDateTime.now()

        return if (existing != null) {
            existing.erpOrder = order
            mapDetailFields(existing, detail)
            existing.deliveryStatus = determineDeliveryStatus(detail)
            existing.updatedAt = now
            erpOrderProductRepository.save(existing)
        } else {
            val product = ErpOrderProduct(
                erpOrder = order,
                sapOrderNumber = sapOrderNumber,
                lineNumber = lineNumber,
                externalKey = externalKey
            )
            mapDetailFields(product, detail)
            product.deliveryStatus = determineDeliveryStatus(detail)
            erpOrderProductRepository.save(product)
        }
    }

    private fun mapDetailFields(product: ErpOrderProduct, detail: SapClientOrderRequest.ItemDetail) {
        product.productCode = detail.productCode
        product.productName = detail.productName
        product.orderQuantity = parseDouble(detail.orderQuantity)
        product.unit = detail.unit
        product.confirmQuantityBox = parseDouble(detail.confirmQuantityBox)
        product.confirmQuantity = parseDouble(detail.confirmQuantity)
        product.confirmUnit = detail.confirmUnit
        product.defaultReason = detail.defaultReason
        product.lineItemStatus = detail.lineItemStatus
        product.shippingDriverName = detail.shippingDriverName
        product.shippingVehicle = detail.shippingVehicle
        product.shippingDriverPhone = detail.shippingDriverPhone
        product.shippingScheduleTime = normalizeTime(detail.shippingScheduleTime)
        product.shippingCompleteTime = normalizeTime(detail.shippingCompleteTime)
        product.shippingQuantityBox = parseDouble(detail.shippingQuantityBox)
        product.shippingQuantity = parseDouble(detail.shippingQuantity)
        product.orderSalesLineAmount = parseDouble(detail.orderSalesLineAmount)
        product.shippingAmount = parseDouble(detail.shippingAmount)
        product.plant = detail.plant
        product.plantNm = detail.plantNm
        product.releaseQuantity = parseDouble(detail.releaseQuantity)
        product.releaseAmount = parseDouble(detail.releaseAmount)
    }

    internal fun determineDeliveryStatus(detail: SapClientOrderRequest.ItemDetail): String {
        val defaultReason = detail.defaultReason?.takeIf { it.isNotBlank() }
        val lineItemStatus = detail.lineItemStatus?.takeIf { it.isNotBlank() }
        val scheduleTime = normalizeTime(detail.shippingScheduleTime)
        val completeTime = normalizeTime(detail.shippingCompleteTime)

        // Sequential evaluation (if/if/if/if) - last match wins
        var status = "대기" // default

        // Condition 1: default_reason empty AND line_item_status empty AND shipping_schedule_time empty
        if (defaultReason == null && lineItemStatus == null && scheduleTime == null) {
            status = "대기"
        }
        // Condition 2: shipping_schedule_time present AND shipping_complete_time empty
        if (scheduleTime != null && completeTime == null) {
            status = "배송중"
        }
        // Condition 3: shipping_complete_time present
        if (completeTime != null) {
            status = "배송완료"
        }
        // Condition 4: default_reason present AND shipping_schedule_time empty
        if (defaultReason != null && scheduleTime == null) {
            status = "결품"
        }

        return status
    }

    private fun normalizeTime(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (value.trim() == "000000") return null
        return value
    }

    private fun parseDouble(value: String?): Double {
        if (value.isNullOrBlank()) return 0.0
        return value.toDouble()
    }
}
