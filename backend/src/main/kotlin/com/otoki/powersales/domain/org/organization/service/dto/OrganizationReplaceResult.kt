package com.otoki.powersales.domain.org.organization.service.dto

/**
 * 조직 마스터 REPLACE_ALL 도메인 결과.
 *
 * 파괴적 패턴 — 전체 성공 / 전체 실패. 행 단위 `failures[]` 시멘틱 없음.
 * 적재 도중 ConstraintViolation 발생 시 `@Transactional` 롤백으로 기존 데이터 복원, 도메인이 throw → 어댑터에서 catch.
 */
data class OrganizationReplaceResult(
    val replacedCount: Int
)
