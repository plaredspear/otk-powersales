package com.otoki.powersales.domain.foundation.product.dto.response

/**
 * 모바일 제품추가 팝업의 중분류→소분류 드롭다운 응답.
 *
 * 레거시: `selectMiddleProduct`(중분류) + `chgSmall`/`selectSmallProduct`(소분류 동적 로드)를
 * 단일 트리 응답으로 통합한다. `subs` 는 해당 중분류의 소분류(category3) 목록.
 */
data class ProductCategoryGroup(
    /** 중분류 (category2). */
    val middle: String,
    /** 소분류 (category3) 목록. */
    val subs: List<String>
)
