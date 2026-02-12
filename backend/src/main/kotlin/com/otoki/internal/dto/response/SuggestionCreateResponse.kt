package com.otoki.internal.dto.response

import com.otoki.internal.entity.Suggestion
import com.otoki.internal.entity.SuggestionCategory

/**
 * 제안 등록 응답 DTO
 */
data class SuggestionCreateResponse(
    val id: Long,
    val category: String,
    val categoryName: String,
    val productCode: String?,
    val productName: String?,
    val title: String,
    val createdAt: String
) {
    companion object {
        /**
         * Entity로부터 Response DTO 생성
         */
        fun from(suggestion: Suggestion): SuggestionCreateResponse {
            return SuggestionCreateResponse(
                id = suggestion.id,
                category = suggestion.category.name,
                categoryName = getCategoryName(suggestion.category),
                productCode = suggestion.productCode,
                productName = suggestion.productName,
                title = suggestion.title,
                createdAt = suggestion.createdAt.toString()
            )
        }

        /**
         * 분류 코드를 한글명으로 변환
         */
        private fun getCategoryName(category: SuggestionCategory): String {
            return when (category) {
                SuggestionCategory.NEW_PRODUCT -> "신제품 제안"
                SuggestionCategory.EXISTING_PRODUCT -> "기존제품 상품가치향상"
            }
        }
    }
}
