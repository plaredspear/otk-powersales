package com.otoki.powersales.admin.dto.response

/**
 * Admin SAP outbound 테스트 응답 DTO 묶음.
 */

/**
 * Preview 응답 — 실제 SAP 송신 없이 payload 만 빌드.
 *
 * @property interfaceId 매핑된 SAP REST Adapter interfaceId
 * @property endpointPath `/<interfaceId>` (호출 시 base-url 뒤에 붙는 경로)
 * @property payload SAP 송신 직전 페이로드 (JSON 직렬화 가능 구조)
 * @property summary 빌드 결과 요약 (예: `"3 rows"`, `"empty payload"`)
 */
data class SapOutboundTestPreviewResponse(
    val interfaceId: String,
    val endpointPath: String,
    val payload: Any?,
    val summary: String,
)

/**
 * Send 응답 — 실제 SAP 송신 결과.
 *
 * Sender 별 결과 형태가 다르므로 공통 부 + sender 별 raw result 로 구성.
 *
 * @property interfaceId SAP interfaceId
 * @property success 송신 성공 여부 (sender 가 throw 하지 않고 true 반환했는지)
 * @property message 사람이 읽을 수 있는 결과 메시지
 * @property sapOutboundLogId BATCH 류 sender 가 적재한 [sap_outbound_log.id] (없으면 null)
 * @property sapOutboxId OUTBOX 류 sender 가 적재한 [sap_outbox.id] (없으면 null)
 * @property result sender 별 반환값 (LoanInquirySapResult / SapOrderRequestDetailLine[] 등)
 */
data class SapOutboundTestSendResponse(
    val interfaceId: String,
    val success: Boolean,
    val message: String,
    val sapOutboundLogId: Long? = null,
    val sapOutboxId: Long? = null,
    val result: Any? = null,
)
