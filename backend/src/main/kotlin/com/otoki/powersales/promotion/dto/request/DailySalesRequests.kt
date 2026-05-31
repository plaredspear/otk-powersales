package com.otoki.powersales.promotion.dto.request

import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * 일매출 마감 입력 (multipart/form-data, @ModelAttribute).
 *
 * 사진은 컨트롤러에서 별도 [org.springframework.web.multipart.MultipartFile] 파라미터로 수신한다.
 * 모든 금액/수량 필드는 선택값이며, "대표상품(수량+단가) 또는 기타상품(수량+금액) 최소 하나" 규칙은 service 에서 검증.
 * camelCase 키로 송신한다 (예: primarySalesQuantity).
 */
data class DailySalesCloseRequest(
    @field:PositiveOrZero(message = "판매단가는 0 이상이어야 합니다")
    val basePrice: BigDecimal? = null,

    @field:PositiveOrZero(message = "대표상품 판매수량은 0 이상이어야 합니다")
    val primarySalesQuantity: BigDecimal? = null,

    @field:PositiveOrZero(message = "대표상품 판매단가는 0 이상이어야 합니다")
    val primarySalesPrice: BigDecimal? = null,

    @field:PositiveOrZero(message = "대표상품 판매금액은 0 이상이어야 합니다")
    val primaryProductAmount: BigDecimal? = null,

    @field:PositiveOrZero(message = "기타상품 판매수량은 0 이상이어야 합니다")
    val otherSalesQuantity: BigDecimal? = null,

    @field:PositiveOrZero(message = "기타상품 판매금액은 0 이상이어야 합니다")
    val otherSalesAmount: BigDecimal? = null,

    @field:Size(max = 50, message = "기타상품명은 최대 50자입니다")
    val description: String? = null,
)

/**
 * 일매출 임시저장 입력 (multipart/form-data, @ModelAttribute).
 *
 * 마감과 달리 "상품 최소 하나" 검증을 하지 않는다 (레거시 tempDailySalesProc 와 동일하게 부분 입력 허용).
 */
data class DailySalesDraftRequest(
    @field:PositiveOrZero(message = "판매단가는 0 이상이어야 합니다")
    val basePrice: BigDecimal? = null,

    @field:PositiveOrZero(message = "대표상품 판매수량은 0 이상이어야 합니다")
    val primarySalesQuantity: BigDecimal? = null,

    @field:PositiveOrZero(message = "대표상품 판매단가는 0 이상이어야 합니다")
    val primarySalesPrice: BigDecimal? = null,

    @field:PositiveOrZero(message = "대표상품 판매금액은 0 이상이어야 합니다")
    val primaryProductAmount: BigDecimal? = null,

    @field:PositiveOrZero(message = "기타상품 판매수량은 0 이상이어야 합니다")
    val otherSalesQuantity: BigDecimal? = null,

    @field:PositiveOrZero(message = "기타상품 판매금액은 0 이상이어야 합니다")
    val otherSalesAmount: BigDecimal? = null,

    @field:Size(max = 50, message = "기타상품명은 최대 50자입니다")
    val description: String? = null,
)
