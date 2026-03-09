package com.otoki.internal.admin.dto.response

import com.otoki.internal.promotion.entity.PromotionType

data class PromotionTypeResponse(
    val id: Long,
    val name: String,
    val displayOrder: Int,
    val isActive: Boolean
) {
    companion object {
        fun from(entity: PromotionType): PromotionTypeResponse = PromotionTypeResponse(
            id = entity.id,
            name = entity.name,
            displayOrder = entity.displayOrder,
            isActive = entity.isActive
        )
    }
}
