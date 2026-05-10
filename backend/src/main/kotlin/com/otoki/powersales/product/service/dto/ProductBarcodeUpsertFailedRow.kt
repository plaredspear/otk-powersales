package com.otoki.powersales.product.service.dto

/**
 * 제품 바코드 UPSERT 실패 행. 도메인 결과 [ProductBarcodeUpsertResult.failures] 의 원소.
 *
 * - [identifier] : 식별자 (현재 채택은 customKey 또는 ProductCode 값)
 * - [reason] : 실패 사유 (예: `"product_code not found: 999999"`, `"ProductCode 필수"`)
 */
data class ProductBarcodeUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
