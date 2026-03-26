package com.otoki.internal.promotion.dto.response

data class BatchUpdatePromotionEmployeeResponse(
    val updatedCount: Int,
    val items: List<PromotionEmployeeListResponse>
)

data class BatchItemError(
    val itemIndex: Int,
    val employeeId: Long?,
    val errorCode: String,
    val message: String
)
