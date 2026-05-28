package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.sap.AttendanceSapPayloadRow
import java.time.LocalDate
import java.time.LocalDateTime

interface TeamMemberScheduleRepositoryCustom {

    /**
     * 출근 등록 시점의 attendance_log id-FK 채움 (Spec #789 — sfid 비즈니스 로직 사용 금지 정책 정합).
     * 운영에서는 HC sync 가 sfid 컬럼을 채우고 본 메서드는 application 레이어 backlink 보강 책임 (dev mock 측 동등 시뮬레이션 포함).
     */
    fun updateAttendanceLog(id: Long, attendanceLogId: Long)

    fun updateSafetyCheckData(
        id: Long,
        equipment1: String?,
        equipment2: String?,
        equipment3: String?,
        equipment4: String?,
        equipment5: String?,
        equipment6: String?,
        equipment7: String?,
        equipment8: String?,
        equipment9: String?,
        yesChkCnt: Double?,
        noChkCnt: Double?,
        startTime: LocalDateTime?,
        completeTime: LocalDateTime?,
        precaution: String?,
        precautionChk: Double?,
        traversalFlag: String?
    )

    fun findByEmployeeIdAndWorkingDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    fun findMonthlyByEmployeeIds(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
        promotionTeams: List<String>? = null
    ): List<TeamMemberSchedule>

    fun findMonthlyByAccountIds(
        accountIds: List<Int>,
        from: LocalDate,
        to: LocalDate,
        promotionTeams: List<String>? = null
    ): List<TeamMemberSchedule>

    fun findActiveByEmployeeIdAndDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    fun deleteAnnualLeaveByEmployeeIdAndDateRange(employeeId: Long, from: LocalDate, to: LocalDate): Long

    fun deleteFutureWorkSchedulesByEmployeeId(employeeId: Long, fromDate: LocalDate): Long

    fun findAnnualLeaveByDateRange(from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findAnnualLeaveByDateRangeAndEmployeeIds(from: LocalDate, to: LocalDate, employeeIds: List<Long>): List<TeamMemberSchedule>

    fun findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId: Long, fromDate: LocalDate, toDate: LocalDate): List<Int>

    fun findIntegrationScheduleRecords(employeeIds: List<Long>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findWorkSchedulesByEmployeeAndAccountAndMonth(employeeId: Long, accountId: Int, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun countWorkSchedulesByEmployeeAndDateAndWorkingType(employeeId: Long, workingDate: LocalDate): Int

    /**
     * 일반 출근(REGULAR) SAP daily batch 용 페이지 조회.
     * `team_member_schedule` ⋈ `attendance_log` (attendance_log_id 단방향 FK JOIN, Spec #789) + employee + account.
     * 필터: attendanceType=REGULAR, workingType='근무', workingDate ∈ {today, yesterday}.
     */
    fun findRegularAttendancesForSapPaged(
        today: LocalDate,
        yesterday: LocalDate,
        limit: Int,
        offset: Int
    ): List<AttendanceSapPayloadRow>
}
