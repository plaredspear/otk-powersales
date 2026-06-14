package com.otoki.powersales.domain.activity.promotion.dto.response

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
