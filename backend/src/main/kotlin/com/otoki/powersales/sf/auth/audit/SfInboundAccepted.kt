package com.otoki.powersales.sf.auth.audit

/**
 * SF 인바운드 어댑터 메서드의 `REQUEST_ACCEPTED` audit 자동 기록 마커.
 *
 * 부착된 메서드는 [SfInboundAuditAspect] 가 around advice 로 감싸 다음을 수행한다:
 * 1. 메서드 진입 직전 — [countArgName] 으로 지정된 인자(`Collection`) 의 size 를 received count 로 추출
 * 2. 메서드 호출 후 정상 return — 결과가 [com.otoki.powersales.sf.inbound.dto.SfInboundUpsertResult]
 *    (또는 chunked 변형) 이면 `successCount` / `failureCount` (/ `chunkCount`) 를 추출,
 *    [reasonTemplate] 의 placeholder 를 치환하여 `REQUEST_ACCEPTED` audit 1회 기록
 * 3. 메서드 throw — `success=0 failure=<received>` audit 기록 후 예외 재전파
 *
 * `reasonTemplate` placeholder:
 * - `{success}` → 결과의 `successCount` (throw 시 0)
 * - `{failure}` → 결과의 `failureCount` (throw 시 `received`)
 * - `{chunks}` → 결과가 chunked 인 경우 `chunkCount`
 *
 * SAP 측 동등 어노테이션 [com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted] 와는 다른 marker —
 * Aspect / Audit 시스템이 외부 시스템별 격리되어 있다 (Spec #774).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SfInboundAccepted(
    val countArgName: String = "items",
    val reasonTemplate: String = "success={success} failure={failure}"
)
