package com.otoki.powersales.schedule.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 행사 단위 일정 일괄 변경 요청 (Spec #571 P1-B).
 */
data class PromotionScheduleBulkUpdateRequest(
    @field:NotEmpty(message = "items는 1~500건이어야 합니다")
    @field:Size(min = 1, max = 500, message = "items는 1~500건이어야 합니다")
    @field:Valid
    val items: List<PromotionScheduleBulkUpdateItem>
)

data class PromotionScheduleBulkUpdateItem(
    @field:NotNull(message = "schedule_id는 필수입니다")
    val scheduleId: Long?,

    @field:NotNull(message = "account_id는 필수입니다")
    val accountId: Int?,

    @field:NotNull(message = "working_date는 필수입니다")
    val workingDate: LocalDate?,

    @field:NotNull(message = "working_category1는 필수입니다")
    val workingCategory1: String?,

    @field:NotNull(message = "working_category3는 필수입니다")
    val workingCategory3: String?,

    val workingCategory4: String? = null
)
