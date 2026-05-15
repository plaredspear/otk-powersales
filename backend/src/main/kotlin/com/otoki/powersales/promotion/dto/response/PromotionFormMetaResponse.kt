package com.otoki.powersales.promotion.dto.response

data class PromotionFormMetaResponse(
    val promotionTypes: List<PromotionTypeOption>,
    val standLocations: List<StandLocationOption>
)

data class PromotionTypeOption(
    val value: String,
    val name: String
)

data class StandLocationOption(
    val value: String,
    val name: String
)
