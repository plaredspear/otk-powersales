package com.otoki.powersales.sap.inbound.dto.organize

import com.otoki.powersales.sap.inbound.dto.SapInboundUpsertResult
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 조직 마스터 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #556)
 *
 * 다른 SAP 인바운드 인터페이스와 동일한 모델 (`success_count` / `failure_count` / `failures[]`).
 * 조직마스터는 파괴적(DELETE 전체 → INSERT 전체) 인터페이스라 행 단위 부분 실패가 발생할 수 없어
 * 정상 처리 시 항상 `failureCount=0`, `failures=[]` 로 응답한다.
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OrganizeMasterDetail(
    override val successCount: Int,
    override val failureCount: Int,
    val failures: List<FailureItem>
) : SapInboundUpsertResult

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FailureItem(
    val orgCd: String?,
    val reason: String
)
