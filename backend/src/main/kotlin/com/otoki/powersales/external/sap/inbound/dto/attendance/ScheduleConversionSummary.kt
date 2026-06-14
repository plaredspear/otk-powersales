package com.otoki.powersales.external.sap.inbound.dto.attendance

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * `attend_info` → `team_member_schedule` 변환 처리 카운트. (Spec #553)
 *
 * SAP 호환 보존을 위해 응답에서 `converted_schedule_count` 등 snake_case 키로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ScheduleConversionSummary(
    val convertedScheduleCount: Int,
    val deletedScheduleCount: Int,
    val skippedEmployeeNotFound: Int,
    val skippedJobFilter: Int,
    val skippedAttendTypeFilter: Int,
    val skippedIdempotent: Int,
    // 휴직/퇴직 사원 연차 스케줄 차단 건수 (SF TeamMemberScheduleTriggerHandler addError 동등).
    val skippedRetiredOrLeave: Int = 0
) {
    operator fun plus(other: ScheduleConversionSummary): ScheduleConversionSummary =
        ScheduleConversionSummary(
            convertedScheduleCount = convertedScheduleCount + other.convertedScheduleCount,
            deletedScheduleCount = deletedScheduleCount + other.deletedScheduleCount,
            skippedEmployeeNotFound = skippedEmployeeNotFound + other.skippedEmployeeNotFound,
            skippedJobFilter = skippedJobFilter + other.skippedJobFilter,
            skippedAttendTypeFilter = skippedAttendTypeFilter + other.skippedAttendTypeFilter,
            skippedIdempotent = skippedIdempotent + other.skippedIdempotent,
            skippedRetiredOrLeave = skippedRetiredOrLeave + other.skippedRetiredOrLeave
        )

    fun toReason(): String =
        "converted=$convertedScheduleCount deleted=$deletedScheduleCount " +
            "skipped_employee_not_found=$skippedEmployeeNotFound " +
            "skipped_job_filter=$skippedJobFilter " +
            "skipped_attend_type=$skippedAttendTypeFilter " +
            "skipped_idempotent=$skippedIdempotent " +
            "skipped_retired_or_leave=$skippedRetiredOrLeave"

    companion object {
        val ZERO: ScheduleConversionSummary = ScheduleConversionSummary(0, 0, 0, 0, 0, 0, 0)
    }
}
