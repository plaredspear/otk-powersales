package com.otoki.powersales.domain.activity.schedule.sap

import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import java.time.LocalDate

/**
 * 여사원일정 근무(REGULAR) SAP 송신 페이로드 빌드용 row projection.
 *
 * 레거시 `Batch_TeamMemberSchedule.cls:43-62` SOQL 결과 셋과 동등.
 * 식별자(EmployeeCode/SAPAccountCode/WorkingCategory1~3)는 schedule 측에서 직접 뽑고,
 * WorkingCategory4(=secondWorkType)만 commute_log(attendance_log) 경유다 — 레거시 SOQL 의 출처 매핑과 일치.
 * `team_member_schedule` + `employee`(schedule.employee_sfid) + `account`(schedule.account_sfid)
 *   + `attendance_log` LEFT JOIN(어제 보정 row 의 secondWorkType 용) 결과.
 */
data class TeamMemberScheduleSapPayloadRow(
    val scheduleId: Long,
    val workingDate: LocalDate,
    val employeeCode: String,
    val accountExternalKey: String?,
    val workingCategory1: WorkingCategory1?,
    val workingCategory2: WorkingCategory2?,
    val workingCategory3: WorkingCategory3?,
    val secondWorkType: SecondWorkType?
)
