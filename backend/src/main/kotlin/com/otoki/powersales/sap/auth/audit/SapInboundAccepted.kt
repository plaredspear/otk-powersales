package com.otoki.powersales.sap.auth.audit

/**
 * SAP 인바운드 어댑터 메서드의 `REQUEST_ACCEPTED` audit 자동 기록 마커. (Spec #639)
 *
 * 부착된 메서드는 [SapInboundAuditAspect] 가 around advice 로 감싸 다음을 수행한다:
 * 1. 메서드 진입 직전 — [countArgName] 으로 지정된 인자(`Collection`) 의 size 를 received count 로 추출
 * 2. 메서드 호출 후 정상 return — 결과가 [com.otoki.powersales.sap.inbound.dto.SapInboundUpsertResult]
 *    (또는 chunked 변형) 이면 `successCount` / `failureCount` (/ `chunkCount`) 를 추출,
 *    [reasonTemplate] 의 placeholder 를 치환하여 `REQUEST_ACCEPTED` audit 1회 기록
 * 3. 메서드 throw — `success=0 failure=<received>` audit 기록 후 예외 재전파
 *
 * `reasonTemplate` placeholder:
 * - `{success}` → 결과의 `successCount` (throw 시 0)
 * - `{failure}` → 결과의 `failureCount` (throw 시 `received`)
 * - `{chunks}` → 결과가 [com.otoki.powersales.sap.inbound.dto.SapInboundChunkedResult] 인 경우 `chunkCount`
 *
 * 사용 예:
 * ```kotlin
 * @SapInboundAccepted("items")
 * fun upsert(items: List<X>): YDetail { ... }
 *
 * @SapInboundAccepted("items", reasonTemplate = "success={success} failure={failure} chunks={chunks}")
 * fun upsertChunked(items: List<X>): ZDetail { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SapInboundAccepted(
    val countArgName: String = "items",
    val reasonTemplate: String = "success={success} failure={failure}"
)
