package com.otoki.powersales.sap.inbound.dto.product

import com.otoki.powersales.sap.inbound.dto.SapInboundUpsertResult
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 제품 / 바코드 / 시스템 코드 마스터 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #559)
 *
 * 세 엔드포인트 모두 동일 구조이지만 [FailureItem.identifier] 의 의미는 다르다.
 * - product: identifier = ProductCode
 * - product-barcode: identifier = customKey
 * - system-code: identifier = externalKey
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ProductMasterDetail(
    override val successCount: Int,
    override val failureCount: Int,
    val failures: List<FailureItem>
) : SapInboundUpsertResult

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FailureItem(
    val identifier: String?,
    val reason: String
)
