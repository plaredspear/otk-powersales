package com.otoki.internal.dto.request

import jakarta.validation.constraints.Min
import org.springframework.web.multipart.MultipartFile

/**
 * 일매출 등록 요청 DTO
 * multipart/form-data로 전송됨
 */
data class DailySalesCreateRequest(
    // 대표제품 정보 (조건부 필수)
    @field:Min(value = 0, message = "대표제품 판매단가는 0 이상이어야 합니다")
    val mainProductPrice: Int? = null,

    @field:Min(value = 0, message = "대표제품 판매수량은 0 이상이어야 합니다")
    val mainProductQuantity: Int? = null,

    // 기타제품 정보 (조건부 필수)
    val subProductCode: String? = null,

    @field:Min(value = 0, message = "기타제품 판매수량은 0 이상이어야 합니다")
    val subProductQuantity: Int? = null,

    @field:Min(value = 0, message = "기타제품 총판매금액은 0 이상이어야 합니다")
    val subProductAmount: Int? = null,

    // 사진 파일 (등록 시 필수, 임시저장 시 선택)
    val photo: MultipartFile? = null
) {
    /**
     * 대표제품 정보가 입력되었는지 확인
     */
    fun hasMainProduct(): Boolean {
        return mainProductPrice != null && mainProductQuantity != null
    }

    /**
     * 기타제품 정보가 입력되었는지 확인
     */
    fun hasSubProduct(): Boolean {
        return subProductCode != null && subProductQuantity != null && subProductAmount != null
    }

    /**
     * 기타제품 정보가 부분적으로만 입력되었는지 확인
     */
    fun hasPartialSubProduct(): Boolean {
        val fields = listOfNotNull(subProductCode, subProductQuantity, subProductAmount)
        return fields.isNotEmpty() && fields.size < 3
    }

    /**
     * 대표제품 총금액 계산
     */
    fun calculateMainProductAmount(): Int? {
        return if (hasMainProduct()) {
            mainProductPrice!! * mainProductQuantity!!
        } else {
            null
        }
    }
}
