package com.otoki.powersales.promotion.dto.request

import jakarta.validation.constraints.DecimalMin
import java.math.BigDecimal

/**
 * 상세 POS품목 (DKRetail__PromotionProduct__c) 생성/수정 요청.
 *
 * SF 다이얼로그 동등 — productId / price 모두 nullable.
 * 행사아이디는 path variable 로 받고 본 body 에 포함하지 않는다.
 */
data class PromotionPosProductRequest(
    val productId: Long? = null,
    // SF DKRetail__Price__c — Number(18,0) 정수형
    @field:DecimalMin(value = "0", inclusive = true, message = "금액은 0 이상이어야 합니다")
    val price: BigDecimal? = null,
)
