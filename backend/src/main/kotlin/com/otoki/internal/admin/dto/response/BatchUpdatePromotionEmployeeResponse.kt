package com.otoki.internal.admin.dto.response

data class BatchUpdatePromotionEmployeeResponse(
    val updatedCount: Int,
    val items: List<PromotionEmployeeListResponse>
)

data class BatchItemError(
    val itemIndex: Int,
    val employeeCode: String?,
    val errorCode: String,
    val message: String
)
