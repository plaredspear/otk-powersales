package com.otoki.powersales.schedule.sap

import com.otoki.powersales.schedule.enums.SecondWorkType
import java.time.LocalDate

/**
 * 일반 출근(REGULAR) SAP 송신 페이로드 빌드용 row projection.
 *
 * 레거시 `Batch_TeamMemberSchedule.cls:43-62` SOQL 결과 셋과 동등.
 * `attendance_log` + `team_member_schedule` + `employee` + `account` join 결과.
 */
data class AttendanceSapPayloadRow(
    val attendanceLogId: Long,
    val workingDate: LocalDate,
    val employeeCode: String,
    val accountExternalKey: String?,
    val workingCategory1: String?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val secondWorkType: SecondWorkType?
)
