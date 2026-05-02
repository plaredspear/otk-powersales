package com.otoki.powersales.admin.dto.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/**
 * 행사 단위 일정 일괄 삭제 요청 (Spec #571 P1-B).
 */
data class PromotionScheduleBulkDeleteRequest(
    @field:NotEmpty(message = "schedule_ids는 1~500건이어야 합니다")
    @field:Size(min = 1, max = 500, message = "schedule_ids는 1~500건이어야 합니다")
    val scheduleIds: List<Long>
)
