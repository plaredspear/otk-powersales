package com.otoki.internal.admin.dto.response

data class PromotionConfirmResponse(
    val promotionId: Long,
    val totalEmployees: Int,
    val upsertedSchedules: Int
)
