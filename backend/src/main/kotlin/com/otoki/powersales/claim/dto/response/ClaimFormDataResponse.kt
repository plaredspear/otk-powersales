package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2

/**
 * 클레임 등록 폼 초기화 데이터 응답 DTO.
 *
 * 클레임 종류1/2 (SF picklist 의존관계), 구매 방법, 요청사항을 enum 으로부터 구성한다.
 * mobile `ClaimFormData` 엔티티 형태와 정합:
 *   - categories[].id        = ClaimType1.value (A/B/C)
 *   - categories[].subcategories[].id = ClaimType2.value (AA~CF)
 *   - purchaseMethods[].code  = PurchaseMethod.sfValue (A/B/C)
 *   - requestTypes[].code     = RequestType.displayName (";" 구분 multipicklist value)
 */
data class ClaimFormDataResponse(
    val categories: List<CategoryInfo>,
    val purchaseMethods: List<CodeNameInfo>,
    val requestTypes: List<CodeNameInfo>
) {
    data class CategoryInfo(
        val id: String,
        val name: String,
        val subcategories: List<SubcategoryInfo>
    )

    data class SubcategoryInfo(
        val id: String,
        val name: String
    )

    data class CodeNameInfo(
        val code: String,
        val name: String
    )

    companion object {
        fun build(): ClaimFormDataResponse {
            val categories = ClaimType1.entries.map { type1 ->
                CategoryInfo(
                    id = type1.value,
                    name = type1.label,
                    subcategories = ClaimType2.entries
                        .filter { it.parent == type1 }
                        .map { SubcategoryInfo(id = it.value, name = it.label) }
                )
            }

            val purchaseMethods = PurchaseMethod.entries.map {
                CodeNameInfo(code = it.sfValue, name = it.displayName)
            }

            val requestTypes = RequestType.entries.map {
                CodeNameInfo(code = it.displayName, name = it.displayName)
            }

            return ClaimFormDataResponse(
                categories = categories,
                purchaseMethods = purchaseMethods,
                requestTypes = requestTypes
            )
        }
    }
}
