package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 관리자 웹 ERP주문 목록 응답 DTO (기준정보 > ERP주문 조회 화면).
 *
 * SAP 인바운드(#561)가 적재한 `erp_order` 를 web admin 에 조회 노출하는 read-only 응답.
 * 표준 페이징 봉투(`content/page/size/totalElements/totalPages`) — 거래처(`AccountListResponse`) 정합.
 */
data class AdminErpOrderListResponse(
    val content: List<AdminErpOrderListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

/**
 * ERP주문 목록 1행 — 헤더 요약 필드.
 *
 * 참조주문번호(`refSapOrderNumber`)는 목록에서 제외한다 — 상세(`AdminErpOrderDetailResponse`)에서만 노출.
 */
data class AdminErpOrderListItem(
    val id: Long,
    val sapOrderNumber: String,
    val sapAccountCode: String?,
    val sapAccountName: String?,
    val deliveryRequestDate: LocalDate?,
    val orderDate: LocalDate?,
    val employeeCode: String?,
    val employeeName: String?,
    val orderSalesAmount: BigDecimal?,
    val orderChannelNm: String?,
    val orderTypeNm: String?
) {
    companion object {
        fun from(order: ErpOrder): AdminErpOrderListItem = AdminErpOrderListItem(
            id = order.id,
            sapOrderNumber = order.sapOrderNumber,
            sapAccountCode = order.sapAccountCode,
            sapAccountName = order.sapAccountName,
            deliveryRequestDate = order.deliveryRequestDate,
            orderDate = order.orderDate,
            employeeCode = order.employeeCode,
            employeeName = order.employeeName,
            orderSalesAmount = order.orderSalesAmount,
            orderChannelNm = order.orderChannelNm,
            orderTypeNm = order.orderTypeNm
        )
    }
}

/**
 * 관리자 웹 ERP주문 상세 응답 DTO — 헤더 전체 필드 + 주문상품 라인 목록.
 */
data class AdminErpOrderDetailResponse(
    val id: Long,
    val sapOrderNumber: String,
    val refSapOrderNumber: String?,
    val sapAccountCode: String?,
    val sapAccountName: String?,
    val accountId: Long?,
    val accountName: String?,
    val deliveryRequestDate: LocalDate?,
    val orderDate: LocalDate?,
    val employeeCode: String?,
    val employeeName: String?,
    val orderSalesAmount: BigDecimal?,
    val orderChannel: String?,
    val orderChannelNm: String?,
    val orderType: String?,
    val orderTypeNm: String?,
    val isDeleted: Boolean?,
    val products: List<AdminErpOrderProductItem>
) {
    companion object {
        fun from(order: ErpOrder, products: List<ErpOrderProduct>): AdminErpOrderDetailResponse =
            AdminErpOrderDetailResponse(
                id = order.id,
                sapOrderNumber = order.sapOrderNumber,
                refSapOrderNumber = order.refSapOrderNumber,
                sapAccountCode = order.sapAccountCode,
                sapAccountName = order.sapAccountName,
                accountId = order.account?.id,
                accountName = order.account?.name,
                deliveryRequestDate = order.deliveryRequestDate,
                orderDate = order.orderDate,
                employeeCode = order.employeeCode,
                employeeName = order.employeeName,
                orderSalesAmount = order.orderSalesAmount,
                orderChannel = order.orderChannel,
                orderChannelNm = order.orderChannelNm,
                orderType = order.orderType,
                orderTypeNm = order.orderTypeNm,
                isDeleted = order.isDeleted,
                products = products.map { AdminErpOrderProductItem.from(it) }
            )
    }
}

/**
 * ERP주문 상세의 주문상품 라인 1건.
 */
data class AdminErpOrderProductItem(
    val id: Long,
    val lineNumber: String?,
    val productCode: String?,
    val productName: String?,
    val orderQuantity: BigDecimal?,
    val unit: String?,
    val confirmQuantity: BigDecimal?,
    val confirmUnit: String?,
    val shippingQuantity: BigDecimal?,
    val orderSalesLineAmount: BigDecimal?,
    val lineItemStatus: String?,
    val deliveryStatus: String?,
    val plantNm: String?
) {
    companion object {
        fun from(product: ErpOrderProduct): AdminErpOrderProductItem = AdminErpOrderProductItem(
            id = product.id,
            lineNumber = product.lineNumber,
            productCode = product.productCode,
            productName = product.productName,
            orderQuantity = product.orderQuantity,
            unit = product.unit,
            confirmQuantity = product.confirmQuantity,
            confirmUnit = product.confirmUnit,
            shippingQuantity = product.shippingQuantity,
            orderSalesLineAmount = product.orderSalesLineAmount,
            lineItemStatus = product.lineItemStatus,
            deliveryStatus = product.deliveryStatus,
            plantNm = product.plantNm
        )
    }
}
