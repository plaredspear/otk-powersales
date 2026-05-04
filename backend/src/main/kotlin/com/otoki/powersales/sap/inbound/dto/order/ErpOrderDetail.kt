package com.otoki.powersales.sap.inbound.dto.order

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP ERP 주문 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #561)
 *
 * 카운트 단위는 헤더 1건 = 1 (라인은 별도 카운트하지 않는다).
 * `failures.identifier` 는 `sap_order_number`.
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ErpOrderDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<ErpOrderFailure>
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ErpOrderFailure(
    val sapOrderNumber: String?,
    val reason: String
)
