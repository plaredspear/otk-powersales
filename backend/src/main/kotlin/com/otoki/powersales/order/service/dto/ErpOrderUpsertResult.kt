package com.otoki.powersales.order.service.dto

/**
 * ERP 주문 UPSERT 도메인 결과 (헤더+라인 분리 카운트).
 *
 * 부분 실패 시멘틱:
 * - Account 매칭 실패 / 필수 필드 누락 → 행 단위 [failures] (saveAll 미포함, 트랜잭션 롤백 없음).
 * - 라인 ConstraintViolation 등 saveAll 도중 throw → 단일 트랜잭션 전체 롤백 (헤더+라인 모두 0).
 *
 * 어댑터 응답은 헤더 카운트 ([headerSuccessCount]) 만 사용 (현재 외부 컨트랙트 보존, 라인은 별도 카운트하지 않음).
 */
data class ErpOrderUpsertResult(
    val headerSuccessCount: Int,
    val lineSuccessCount: Int,
    val failures: List<ErpOrderUpsertFailedRow>
)
