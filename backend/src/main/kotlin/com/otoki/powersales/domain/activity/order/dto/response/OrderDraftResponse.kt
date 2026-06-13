package com.otoki.powersales.domain.activity.order.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 임시저장 등록 응답 — `POST /api/v1/mobile/orders/draft` (Spec #596).
 */
data class OrderDraftSaveResponse(
    val draftId: Long,
    val savedAt: LocalDateTime,
)

/**
 * 주문 임시저장 조회 응답 — `GET /api/v1/mobile/orders/draft` (Spec #596).
 *
 * 거래처명/external_key/`totalAmount` 헤더 + 라인 List. 납기일 미반환 (Q8 — 레거시 정합).
 */
data class OrderDraftDetailResponse(
    val draftId: Long,
    val accountId: Long,
    val accountName: String,
    val accountExternalKey: String?,
    val totalAmount: Long,
    val savedAt: LocalDateTime,
    val lines: List<OrderDraftLineResponse>,
)

data class OrderDraftLineResponse(
    val lineNumber: Int,
    val productCode: String,
    val productName: String?,
    val unit: String,
    val quantity: BigDecimal,
    val quantityPieces: Int?,
    val quantityBoxes: BigDecimal?,
    val unitPrice: BigDecimal?,
    val amount: BigDecimal?,
)
