package com.otoki.powersales.domain.activity.order.service.dto

/**
 * ERP 주문 UPSERT 도메인 입력 커맨드 (헤더 + 라인 다단).
 *
 * - 헤더 UPSERT 키: [sapOrderNumber] (= [com.otoki.powersales.domain.activity.order.entity.ErpOrder.sapOrderNumber])
 * - 라인 UPSERT 키 / 도메인 동작은 [ErpOrderLineCommand] 참조.
 */
data class ErpOrderUpsertCommand(
    val sapOrderNumber: String?,
    val sapAccountCode: String?,
    val sapAccountName: String?,
    val deliveryRequestDate: String?,
    val orderDate: String?,
    val employeeCode: String?,
    val employeeName: String?,
    val orderSalesAmount: String?,
    val orderChannel: String?,
    val orderChannelNm: String?,
    val orderType: String?,
    val orderTypeNm: String?,
    val lines: List<ErpOrderLineCommand>
)
