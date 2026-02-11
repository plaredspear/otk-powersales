package com.otoki.internal.dto.response

import com.otoki.internal.entity.ClaimCategory

/**
 * 클레임 종류1 (하위 종류2 포함) 응답 DTO
 */
data class ClaimCategoryWithSubcategoriesResponse(
    val id: Long,
    val name: String,
    val subcategories: List<ClaimSubcategoryResponse>
) {
    companion object {
        fun from(
            category: ClaimCategory,
            subcategories: List<ClaimSubcategoryResponse>
        ): ClaimCategoryWithSubcategoriesResponse {
            return ClaimCategoryWithSubcategoriesResponse(
                id = category.id,
                name = category.name,
                subcategories = subcategories
            )
        }
    }
}
