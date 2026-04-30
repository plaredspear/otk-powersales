package com.otoki.powersales.sap.inbound.dto.product

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 제품 바코드 마스터 행 DTO. (Spec #559)
 *
 * UPSERT 키 customKey 는 (ProductCode + ProductUnit + ProductSequence) 단순 연결.
 */
data class BarcodeMasterRequestItem(
    @JsonProperty("ProductCode") val productCode: String? = null,
    @JsonProperty("ProductName") val productName: String? = null,
    @JsonProperty("ProductUnit") val productUnit: String? = null,
    @JsonProperty("ProductSequence") val productSequence: String? = null,
    @JsonProperty("ProductBarcode") val productBarcode: String? = null
)
