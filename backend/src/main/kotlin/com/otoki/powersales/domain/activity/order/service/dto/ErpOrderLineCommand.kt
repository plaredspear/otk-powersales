package com.otoki.powersales.domain.activity.order.service.dto

/**
 * ERP 주문 라인 도메인 커맨드. [ErpOrderUpsertCommand.lines] 의 원소.
 *
 * - UPSERT 키: `sapOrderNumber(선두 0 1자 제거) + lineNumber` (= [com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct.externalKey])
 * - `shippingVehicle` 은 키 구성에서 제외 — 차량 후속 갱신 호출에서 동일 라인을 UPDATE 해야 함.
 */
data class ErpOrderLineCommand(
    val sapOrderNumber: String?,
    val refSapOrderNumber: String?,
    val lineNumber: String?,
    val productCode: String?,
    val productName: String?,
    val orderQuantity: String?,
    val unit: String?,
    val confirmQuantityBox: String?,
    val confirmQuantity: String?,
    val confirmUnit: String?,
    val defaultReason: String?,
    val lineItemStatus: String?,
    val shippingDriverName: String?,
    val shippingVehicle: String?,
    val shippingDriverPhone: String?,
    val shippingScheduleTime: String?,
    val shippingCompleteTime: String?,
    val shippingQuantityBox: String?,
    val shippingQuantity: String?,
    val orderSalesLineAmount: String?,
    val shippingAmount: String?,
    val plant: String?,
    val plantNm: String?,
    val releaseQuantity: String?,
    val releaseAmount: String?
)
