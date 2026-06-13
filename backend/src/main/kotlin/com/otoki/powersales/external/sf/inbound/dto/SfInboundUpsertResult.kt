package com.otoki.powersales.external.sf.inbound.dto

/**
 * SF 인바운드 어댑터의 도메인 적재 결과 응답 DTO 가 구현하는 공통 인터페이스.
 *
 * [com.otoki.powersales.external.sf.auth.audit.SfInboundAuditAspect] 가 메서드 반환값에서
 * `successCount` / `failureCount` 를 타입 안전하게 추출하여 `REQUEST_ACCEPTED` audit 의
 * `reason` 필드를 구성하는 데 사용한다.
 *
 * 청크 처리 변형은 [SfInboundChunkedResult] 참조.
 */
interface SfInboundUpsertResult {
    val successCount: Int
    val failureCount: Int
}
