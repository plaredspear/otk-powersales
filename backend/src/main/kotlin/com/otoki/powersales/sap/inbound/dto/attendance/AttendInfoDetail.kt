package com.otoki.powersales.sap.inbound.dto.attendance

import com.fasterxml.jackson.annotation.JsonIgnore
import com.otoki.powersales.sap.inbound.dto.SapInboundChunkedResult
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 출근 정보 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #562, #553)
 *
 * 청크 단위 처리이므로 `chunks` 결과를 함께 보고한다 (#560 패턴 동일).
 *
 * `scheduleConversion` (Spec #553): `attend_info` INSERT 후 연차류 코드를 `team_member_schedule`
 * 일정으로 변환한 결과 카운트. 변환 호출이 단 한 번도 발생하지 않은 경우(예: 모든 청크 commit 실패)
 * `null` 가능, 변환은 호출되었으나 실제 변환 0건이면 모든 카운트 0 인 객체.
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 *
 * Spec #639: [SapInboundChunkedResult] 구현 — `chunkCount` 는 `chunks.size` 에서 파생되며
 * 외부 응답 JSON 에는 포함되지 않는다 (`@get:JsonIgnore` — 본 필드는 [com.otoki.powersales.sap.auth.audit.SapInboundAuditAspect]
 * 의 audit reason 추출 전용).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AttendInfoDetail(
    override val successCount: Int,
    override val failureCount: Int,
    val failures: List<FailureItem>,
    val chunks: List<ChunkResult>,
    val scheduleConversion: ScheduleConversionSummary? = null
) : SapInboundChunkedResult {
    @get:JsonIgnore
    override val chunkCount: Int get() = chunks.size
}
