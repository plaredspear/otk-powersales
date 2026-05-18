package com.otoki.powersales.sf.inbound.dto.sales

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * SF 월 매출 이력 인바운드 요청 DTO (Spec #775).
 *
 * SAP 측 [com.otoki.powersales.sap.inbound.dto.sales.MonthlySalesHistoryRequest] 와 동일 wire format.
 * 동일 도메인 service [com.otoki.powersales.sales.service.MonthlySalesHistoryUpsertService] 를 호출하기
 * 위해 같은 필드 형식 유지 — adapter 만 분리.
 */
data class SfMonthlySalesHistoryRequest(
    @field:NotNull(message = "reqItemList 필수")
    @JsonProperty("reqItemList")
    val reqItemList: List<SfMonthlySalesHistoryRequestItem>?
)
