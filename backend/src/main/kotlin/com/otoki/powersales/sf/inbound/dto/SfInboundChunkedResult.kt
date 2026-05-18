package com.otoki.powersales.sf.inbound.dto

/**
 * 청크 분할 처리한 SF 인바운드 어댑터의 결과 응답 DTO 가 구현하는 인터페이스.
 *
 * [com.otoki.powersales.sf.auth.audit.SfInboundAuditAspect] 가 `chunkCount` 를 추출하여
 * `REQUEST_ACCEPTED` audit 의 `reason` 에 `chunks={N}` placeholder 를 치환한다.
 */
interface SfInboundChunkedResult : SfInboundUpsertResult {
    val chunkCount: Int
}
