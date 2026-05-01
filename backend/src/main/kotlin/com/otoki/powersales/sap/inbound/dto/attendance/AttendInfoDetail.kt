package com.otoki.powersales.sap.inbound.dto.attendance

import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem

/**
 * SAP 출근 정보 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #562, #553)
 *
 * 청크 단위 처리이므로 `chunks` 결과를 함께 보고한다 (#560 패턴 동일).
 *
 * `scheduleConversion` (Spec #553): `attend_info` INSERT 후 연차류 코드를 `team_member_schedule`
 * 일정으로 변환한 결과 카운트. 변환 호출이 단 한 번도 발생하지 않은 경우(예: 모든 청크 commit 실패)
 * `null` 가능, 변환은 호출되었으나 실제 변환 0건이면 모든 카운트 0 인 객체.
 */
data class AttendInfoDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>,
    val chunks: List<ChunkResult>,
    val scheduleConversion: ScheduleConversionSummary? = null
)
