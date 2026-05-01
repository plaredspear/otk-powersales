package com.otoki.powersales.sap.inbound.dto.attendance

/**
 * `attend_info` → `team_member_schedule` 변환 처리 카운트. (Spec #553)
 *
 * Jackson SNAKE_CASE 정책에 의해 응답에서는 `converted_schedule_count` 등 snake_case 키로 직렬화된다.
 */
data class ScheduleConversionSummary(
    val convertedScheduleCount: Int,
    val deletedScheduleCount: Int,
    val skippedEmployeeNotFound: Int,
    val skippedJobFilter: Int,
    val skippedAttendTypeFilter: Int,
    val skippedIdempotent: Int
) {
    operator fun plus(other: ScheduleConversionSummary): ScheduleConversionSummary =
        ScheduleConversionSummary(
            convertedScheduleCount = convertedScheduleCount + other.convertedScheduleCount,
            deletedScheduleCount = deletedScheduleCount + other.deletedScheduleCount,
            skippedEmployeeNotFound = skippedEmployeeNotFound + other.skippedEmployeeNotFound,
            skippedJobFilter = skippedJobFilter + other.skippedJobFilter,
            skippedAttendTypeFilter = skippedAttendTypeFilter + other.skippedAttendTypeFilter,
            skippedIdempotent = skippedIdempotent + other.skippedIdempotent
        )

    fun toReason(): String =
        "converted=$convertedScheduleCount deleted=$deletedScheduleCount " +
            "skipped_employee_not_found=$skippedEmployeeNotFound " +
            "skipped_job_filter=$skippedJobFilter " +
            "skipped_attend_type=$skippedAttendTypeFilter " +
            "skipped_idempotent=$skippedIdempotent"

    companion object {
        val ZERO: ScheduleConversionSummary = ScheduleConversionSummary(0, 0, 0, 0, 0, 0)
    }
}
