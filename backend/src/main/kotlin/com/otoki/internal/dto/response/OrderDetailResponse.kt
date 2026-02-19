/*
package com.otoki.internal.dto.response

import com.otoki.internal.entity.Order
import com.otoki.internal.entity.OrderItem
import com.otoki.internal.entity.OrderProcessingRecord
import com.otoki.internal.entity.OrderRejection

/ **
 * 주문 상세 응답 DTO
 * /
data class OrderDetailResponse(
    val id: Long,
    val orderRequestNumber: String,
    val clientId: Long,
    val clientName: String,
    val clientDeadlineTime: String?,
    val orderDate: String,
    val deliveryDate: String,
    val totalAmount: Long,
    val totalApprovedAmount: Long,
    val approvalStatus: String,
    val isClosed: Boolean,
    val orderedItemCount: Int,
    val orderedItems: List<OrderedItemResponse>,
    val orderProcessingStatus: OrderProcessingStatusResponse?,
    val rejectedItems: List<RejectedItemResponse>?
) {
    companion object {
        fun from(
            order: Order,
            items: List<OrderItem>,
            isClosed: Boolean,
            processingRecords: List<OrderProcessingRecord>,
            rejections: List<OrderRejection>
        ): OrderDetailResponse {
            val orderedItems = items.map { OrderedItemResponse.from(it) }

            val processingStatus = if (isClosed && processingRecords.isNotEmpty()) {
                OrderProcessingStatusResponse.from(processingRecords)
            } else {
                null
            }

            val rejectedItems = if (isClosed && rejections.isNotEmpty()) {
                rejections.map { RejectedItemResponse.from(it) }
            } else {
                null
            }

            return OrderDetailResponse(
                id = order.id,
                orderRequestNumber = order.orderRequestNumber,
                clientId = order.store.id,
                clientName = order.store.storeName,
                clientDeadlineTime = order.clientDeadlineTime,
                orderDate = order.orderDate.toString(),
                deliveryDate = order.deliveryDate.toString(),
                totalAmount = order.totalAmount,
                totalApprovedAmount = order.totalApprovedAmount,
                approvalStatus = order.approvalStatus.name,
                isClosed = isClosed,
                orderedItemCount = items.size,
                orderedItems = orderedItems,
                orderProcessingStatus = processingStatus,
                rejectedItems = rejectedItems
            )
        }
    }
}

/ **
 * 주문 제품 항목 응답 DTO
 * /
data class OrderedItemResponse(
    val productCode: String,
    val productName: String,
    val totalQuantityBoxes: Double,
    val totalQuantityPieces: Int,
    val isCancelled: Boolean
) {
    companion object {
        fun from(item: OrderItem): OrderedItemResponse {
            return OrderedItemResponse(
                productCode = item.productCode,
                productName = item.productName,
                totalQuantityBoxes = item.quantityBoxes,
                totalQuantityPieces = item.quantityPieces,
                isCancelled = item.isCancelled
            )
        }
    }
}

/ **
 * 주문 처리 현황 응답 DTO
 * /
data class OrderProcessingStatusResponse(
    val sapOrderNumber: String,
    val items: List<ProcessingItemResponse>
) {
    companion object {
        fun from(records: List<OrderProcessingRecord>): OrderProcessingStatusResponse {
            return OrderProcessingStatusResponse(
                sapOrderNumber = records.first().sapOrderNumber,
                items = records.map { ProcessingItemResponse.from(it) }
            )
        }
    }
}

/ **
 * 처리 항목 응답 DTO
 * /
data class ProcessingItemResponse(
    val productCode: String,
    val productName: String,
    val deliveredQuantity: String,
    val deliveryStatus: String
) {
    companion object {
        fun from(record: OrderProcessingRecord): ProcessingItemResponse {
            return ProcessingItemResponse(
                productCode = record.productCode,
                productName = record.productName,
                deliveredQuantity = record.deliveredQuantity,
                deliveryStatus = record.deliveryStatus.name
            )
        }
    }
}

/ **
 * 반려 제품 응답 DTO
 * /
data class RejectedItemResponse(
    val productCode: String,
    val productName: String,
    val orderQuantityBoxes: Int,
    val rejectionReason: String
) {
    companion object {
        fun from(rejection: OrderRejection): RejectedItemResponse {
            return RejectedItemResponse(
                productCode = rejection.productCode,
                productName = rejection.productName,
                orderQuantityBoxes = rejection.orderQuantityBoxes,
                rejectionReason = rejection.rejectionReason
            )
        }
    }
}
*/
