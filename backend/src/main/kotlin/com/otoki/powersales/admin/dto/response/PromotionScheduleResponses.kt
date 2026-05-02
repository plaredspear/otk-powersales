package com.otoki.powersales.admin.dto.response

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamType
import java.time.LocalDate

/**
 * 행사 단위 일정 목록 조회 응답 (Spec #571 P1-B).
 */
data class PromotionScheduleListResponse(
    val promotionId: Long,
    val promotionName: String?,
    val schedulePeriod: SchedulePeriod,
    val members: List<PromotionScheduleMember>,
    val totalMemberCount: Int,
    val totalScheduleCount: Int
)

data class SchedulePeriod(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class PromotionScheduleMember(
    val promotionEmployeeId: Long,
    val employeeId: Long,
    val employeeNumber: String,
    val employeeName: String,
    val professionalPromotionTeam: ProfessionalPromotionTeamType?,
    val schedules: List<PromotionScheduleItem>
)

data class PromotionScheduleItem(
    val scheduleId: Long,
    val workingDate: LocalDate,
    val accountId: Int,
    val accountCode: String?,
    val accountName: String,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?
)

/**
 * 일괄 변경 응답.
 */
data class PromotionScheduleBulkUpdateResponse(
    val updatedCount: Int,
    val scheduleIds: List<Long>
)

/**
 * 일괄 삭제 응답.
 */
data class PromotionScheduleBulkDeleteResponse(
    val deletedCount: Int
)
