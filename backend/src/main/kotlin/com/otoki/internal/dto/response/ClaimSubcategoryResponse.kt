package com.otoki.internal.dto.response

import com.otoki.internal.entity.ClaimSubcategory

/**
 * 클레임 종류2 응답 DTO
 */
data class ClaimSubcategoryResponse(
    val id: Long,
    val name: String
) {
    companion object {
        fun from(subcategory: ClaimSubcategory): ClaimSubcategoryResponse {
            return ClaimSubcategoryResponse(
                id = subcategory.id,
                name = subcategory.name
            )
        }
    }
}
