package com.otoki.powersales.domain.foundation.product.service.dto

/**
 * 제품 바코드 마스터 UPSERT 도메인 입력 커맨드.
 *
 * - UPSERT 키: `productCode + productUnit + productSequence` 단순 연결 (= [com.otoki.powersales.domain.foundation.product.entity.ProductBarcode.customKey])
 */
data class ProductBarcodeUpsertCommand(
    val productCode: String?,
    val productName: String?,
    val productUnit: String?,
    val productSequence: String?,
    val productBarcode: String?
)
