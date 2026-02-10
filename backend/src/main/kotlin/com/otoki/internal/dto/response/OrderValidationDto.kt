package com.otoki.internal.dto.response

/**
 * 주문서 유효성 검증 결과 응답 DTO
 */
data class ValidationResultResponse(
    val isValid: Boolean,
    val invalidItems: List<InvalidItemResponse>
)

/**
 * 유효성 실패한 제품 상세 정보
 */
data class InvalidItemResponse(
    val productCode: String,
    val productName: String,
    val boxQuantity: Int,
    val pieceQuantity: Int,
    val piecesPerBox: Int,
    val minOrderUnit: Int,
    val supplyQuantity: Int,
    val dcQuantity: Int,
    val validationErrors: List<String>
)
