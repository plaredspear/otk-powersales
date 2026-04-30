package com.otoki.powersales.sap.inbound.dto.product

/**
 * SAP 제품 / 바코드 / 시스템 코드 마스터 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #559)
 *
 * 세 엔드포인트 모두 동일 구조이지만 [FailureItem.identifier] 의 의미는 다르다.
 * - product: identifier = ProductCode
 * - product-barcode: identifier = customKey
 * - system-code: identifier = externalKey
 */
data class ProductMasterDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>
)

data class FailureItem(
    val identifier: String?,
    val reason: String
)
