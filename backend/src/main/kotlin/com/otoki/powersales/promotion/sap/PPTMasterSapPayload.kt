package com.otoki.powersales.promotion.sap

/**
 * 전문행사조 마스터 SAP 송신 wrapper (Spec #765 §6.2).
 *
 * 레거시 `IF_REST_SAP_PPTMToSAP.cls:157-159` 의
 * `wrapper = { 'REQUEST' => PPTMMapList }` 와 1:1 정합. JSON 직렬화 시
 * `{ "REQUEST": [ row1, row2, ... ] }` 형태로 SAP REST Adapter (`/SD03300`) 로 송신된다.
 */
@Suppress("ConstructorParameterNaming", "PropertyName")
data class PPTMasterSapPayload(
    val REQUEST: List<PPTMasterSapPayloadRow>
)
