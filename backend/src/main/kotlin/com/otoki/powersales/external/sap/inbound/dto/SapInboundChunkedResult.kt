package com.otoki.powersales.external.sap.inbound.dto

/**
 * 청크 분할 처리한 SAP 인바운드 어댑터의 결과 응답 DTO 가 구현하는 인터페이스. (Spec #639)
 *
 * `SapInboundAuditAspect` 가 `chunkCount` 를 추출하여 `REQUEST_ACCEPTED` audit 의
 * `reason` 에 `chunks={N}` placeholder 를 치환한다.
 */
interface SapInboundChunkedResult : SapInboundUpsertResult {
    val chunkCount: Int
}
