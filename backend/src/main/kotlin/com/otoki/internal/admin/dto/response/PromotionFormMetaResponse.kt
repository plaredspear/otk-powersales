package com.otoki.internal.admin.dto.response

data class PromotionFormMetaResponse(
    val promotionTypes: List<PromotionTypeOption>,
    val standLocations: List<StandLocationOption>
)

data class PromotionTypeOption(
    val id: Long,
    val name: String
)

data class StandLocationOption(
    val value: String,
    val name: String
)
