package com.otoki.internal.dto.response

import com.otoki.internal.entity.Event
import com.otoki.internal.entity.EventProduct

/**
 * 행사 상세 응답 DTO
 */
data class EventDetailResponse(
    val event: EventInfo,
    val salesInfo: SalesInfo,
    val products: ProductsInfo,
    val isTodayRegistered: Boolean,
    val canRegisterToday: Boolean
) {

    /**
     * 행사 기본 정보
     */
    data class EventInfo(
        val eventId: String,
        val eventType: String,
        val eventName: String,
        val startDate: String,
        val endDate: String,
        val customerId: String,
        val customerName: String,
        val assigneeId: String
    ) {
        companion object {
            fun from(event: Event, customerName: String): EventInfo {
                return EventInfo(
                    eventId = event.eventId,
                    eventType = event.eventType,
                    eventName = event.eventName,
                    startDate = event.startDate.toString(),
                    endDate = event.endDate.toString(),
                    customerId = event.customerId,
                    customerName = customerName,
                    assigneeId = event.assigneeId
                )
            }
        }
    }

    /**
     * 매출 정보
     */
    data class SalesInfo(
        val targetAmount: Long,
        val achievedAmount: Long,
        val achievementRate: Double,
        val progressRate: Double
    )

    /**
     * 제품 정보
     */
    data class ProductsInfo(
        val mainProduct: ProductInfo?,
        val subProducts: List<ProductInfo>
    ) {
        companion object {
            fun from(products: List<EventProduct>): ProductsInfo {
                val mainProduct = products.firstOrNull { it.isMainProduct }
                    ?.let { ProductInfo.from(it) }
                val subProducts = products.filter { !it.isMainProduct }
                    .map { ProductInfo.from(it) }

                return ProductsInfo(
                    mainProduct = mainProduct,
                    subProducts = subProducts
                )
            }
        }
    }

    /**
     * 제품 정보
     */
    data class ProductInfo(
        val productCode: String,
        val productName: String,
        val isMainProduct: Boolean
    ) {
        companion object {
            fun from(eventProduct: EventProduct): ProductInfo {
                return ProductInfo(
                    productCode = eventProduct.productCode,
                    productName = eventProduct.productName,
                    isMainProduct = eventProduct.isMainProduct
                )
            }
        }
    }
}
