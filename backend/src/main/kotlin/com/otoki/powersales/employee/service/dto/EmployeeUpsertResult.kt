package com.otoki.powersales.employee.service.dto

/**
 * 직원 마스터 UPSERT 도메인 결과.
 *
 * 부분 실패 시멘틱 — 행 단위 검증 실패는 트랜잭션 롤백 없이 [failures] 누적 후 성공 행만 saveAll.
 *
 * - [protectedManualCodes] : Spec #579 — origin=MANUAL 인 기존 직원은 SAP 인바운드 갱신 대상에서 제외.
 *   응답·카운트에는 영향 없으며, 어댑터에서 별도 audit (`MANUAL_ORIGIN_PROTECTED`) 으로 기록한다.
 */
data class EmployeeUpsertResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<EmployeeUpsertFailedRow>,
    val protectedManualCodes: List<String>
)
