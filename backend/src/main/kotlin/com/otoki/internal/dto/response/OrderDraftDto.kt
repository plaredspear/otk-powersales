package com.otoki.internal.dto.response

import com.otoki.internal.entity.OrderDraft
import com.otoki.internal.entity.OrderDraftItem
import java.time.format.DateTimeFormatter

/**
 * 임시저장 주문서 응답 DTO
 */
data class OrderDraftResponse(
    val clientId: Long,
    val clientName: String,
    val deliveryDate: String,
    val items: List<DraftItemResponse>,
    val totalAmount: Long,
    val savedAt: String
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
        private val DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(draft: OrderDraft): OrderDraftResponse {
            return OrderDraftResponse(
                clientId = draft.store.id,
                clientName = draft.store.storeName,
                deliveryDate = draft.deliveryDate.format(DATE_FORMATTER),
                items = draft.items.map { DraftItemResponse.from(it) },
                totalAmount = draft.totalAmount,
                savedAt = draft.updatedAt.format(DATETIME_FORMATTER)
            )
        }
    }
}

/**
 * 임시저장 제품 항목 응답 DTO
 */
data class DraftItemResponse(
    val productCode: String,
    val productName: String,
    val boxQuantity: Int,
    val pieceQuantity: Int,
    val unitPrice: Long,
    val amount: Long,
    val piecesPerBox: Int,
    val minOrderUnit: Int,
    val supplyQuantity: Int,
    val dcQuantity: Int
) {
    companion object {
        fun from(item: OrderDraftItem): DraftItemResponse {
            return DraftItemResponse(
                productCode = item.productCode,
                productName = item.productName,
                boxQuantity = item.boxQuantity,
                pieceQuantity = item.pieceQuantity,
                unitPrice = item.unitPrice,
                amount = item.amount,
                piecesPerBox = item.piecesPerBox,
                minOrderUnit = item.minOrderUnit,
                supplyQuantity = item.supplyQuantity,
                dcQuantity = item.dcQuantity
            )
        }
    }
}

/**
 * 임시저장 성공 응답 DTO
 */
data class DraftSavedResponse(
    val savedAt: String
)
