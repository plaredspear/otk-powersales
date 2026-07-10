package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderCalendarDay
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyEmployeeItem
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusSummary
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyWorkerItem
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 여사원 일별/월간 근무 현황 계산 코어 — 조장 일별현황([LeaderScheduleService]) 과
 * AccountViewAll 대리출근([ProxyAttendanceService]) 이 공유하는 조회 로직.
 *
 * 대상 인원(`teamEmployeeIds`)만 파라미터로 받으므로, 인원을 정하는 스코프 기준
 * (조장 = 본인 costCenterCode, 대리출근 = 선택 지점 costCenterCode)은 호출부 책임이다.
 *
 * 레거시 `employee/mngDaily.jsp` + `EmployeeController.mgnDaily`(일별현황) /
 * `mgnSchedule.jsp` + `calSchedule`(월간 캘린더) 결과 재현 규칙:
 * - 진열: `DisplayWorkSchedule`(확정·기간유효) 중 **안전점검 제출자**만 포함. 거래처별 출근 =
 *   동일 (여사원,거래처,일자) 진열 team_member_schedule 행의 attendanceLog 존재.
 * - 행사: team_member_schedule `WORK && cat1=EVENT`.
 * - 연차: team_member_schedule `ANNUAL_LEAVE`.
 * - 요약 총원/출근: 진열·행사 distinct 여사원 수 (mergedList 여사원 단위 카운터).
 * - 정렬: 진열=출근완료→임시(미출근)→정규(미출근), 행사=출근완료→미출근, 그 안은 이름·거래처명순.
 */
@Component
@Transactional(readOnly = true)
class TeamDailyStatusCalculator(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository,
) {

    companion object {
        /** 진열 cat2 '임시' 표시값 (레거시 tempList `workingcategory2__c.contains("임시")` 정합). */
        private val DISPLAY_CATEGORY2_TEMPORARY = TypeOfWork5.TEMPORARY.displayName
    }

    /**
     * 대상 인원의 특정 날짜 일별 현황(진열/행사/연차 + 요약)을 계산해 응답 DTO 로 반환.
     * 대상 인원이 비었으면 [emptyDailyStatus].
     */
    fun computeDailyStatus(teamEmployeeIds: List<Long>, date: LocalDate): LeaderDailyStatusResponse {
        if (teamEmployeeIds.isEmpty()) return emptyDailyStatus(date)

        val workers = computeDailyWorkers(teamEmployeeIds, date)
        val schedules = workers.schedules
        val rawDisplay = workers.display
        val rawEvent = workers.event

        // ── 정렬 (레거시 mergedList 버킷 순서, 여사원 단위) ──
        // 진열: 출근완료 → 임시(미출근) → 정규(미출근), 그 안은 이름·거래처명순.
        val displayAttendedEmps = rawDisplay.filter { it.attended }.mapNotNull { it.employeeId }.toSet()
        val displayTempEmps = rawDisplay
            .filter {
                it.employeeId != null && it.employeeId !in displayAttendedEmps &&
                    it.workingCategory2 == DISPLAY_CATEGORY2_TEMPORARY
            }
            .mapNotNull { it.employeeId }
            .toSet()
        val displayWorkers = rawDisplay.sortedWith(
            compareBy(
                { w -> when {
                    w.employeeId in displayAttendedEmps -> 0
                    w.employeeId in displayTempEmps -> 1
                    else -> 2
                } },
                { it.employeeName },
                { it.accountName },
            )
        )
        // 행사: 출근완료 → 미출근, 그 안은 이름·거래처명순.
        val eventAttendedEmps = rawEvent.filter { it.attended }.mapNotNull { it.employeeId }.toSet()
        val eventWorkers = rawEvent.sortedWith(
            compareBy(
                { w -> if (w.employeeId in eventAttendedEmps) 0 else 1 },
                { it.employeeName },
                { it.accountName },
            )
        )

        // ── 연차: team_member_schedule ANNUAL_LEAVE ──
        val annualLeaveWorkers = schedules
            .filter { it.workingType == WorkingType.ANNUAL_LEAVE }
            .mapNotNull { it.employee }
            .distinctBy { it.id }
            .sortedBy { it.name }
            .map { LeaderDailyEmployeeItem(employeeId = it.id, employeeName = it.name, employeeCode = it.employeeCode) }

        // ── 요약: 진열·행사 distinct 여사원 수 ──
        val summary = LeaderDailyStatusSummary(
            displayTotal = rawDisplay.mapNotNull { it.employeeId }.toSet().size,
            displayAttended = rawDisplay.filter { it.attended }.mapNotNull { it.employeeId }.toSet().size,
            eventTotal = rawEvent.mapNotNull { it.employeeId }.toSet().size,
            eventAttended = rawEvent.filter { it.attended }.mapNotNull { it.employeeId }.toSet().size,
            annualLeaveCount = annualLeaveWorkers.size,
        )

        return LeaderDailyStatusResponse(
            date = date.toString(),
            summary = summary,
            displayWorkers = displayWorkers,
            eventWorkers = eventWorkers,
            annualLeaveWorkers = annualLeaveWorkers,
        )
    }

    /**
     * 월간 캘린더 1일 항목 계산 — total=(진열[안전점검]+행사) 건수, attended=출근완료 건수.
     * total=0 이면 null (레거시 cnt>0 만 표시).
     */
    fun computeCalendarDay(teamEmployeeIds: List<Long>, date: LocalDate): LeaderCalendarDay? {
        val workers = computeDailyWorkers(teamEmployeeIds, date)
        val total = workers.display.size + workers.event.size
        if (total == 0) return null
        val attended = workers.display.count { it.attended } + workers.event.count { it.attended }
        return LeaderCalendarDay(date = date.toString(), total = total, attended = attended)
    }

    /**
     * 일별 진열/행사 워커 집계 — 일별 현황·월간 캘린더 공유.
     * - 진열: `DisplayWorkSchedule`(확정·기간유효) 중 안전점검 제출자만, 거래처별 출근은 진열
     *   team_member_schedule 행의 attendanceLog 존재로 판정.
     * - 행사: team_member_schedule cat1=EVENT.
     */
    private fun computeDailyWorkers(teamEmployeeIds: List<Long>, date: LocalDate): DailyWorkers {
        val schedules = teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, teamEmployeeIds)

        val safetyEmployeeIds = safetyCheckSubmissionRepository
            .findByEmployeeIdInAndWorkingDate(teamEmployeeIds, date)
            .mapNotNull { it.employeeId }
            .toSet()
        val displayAttendedKeys = schedules
            .asSequence()
            .filter { it.workingCategory1 == WorkingCategory1.DISPLAY && it.attendanceLog != null }
            .mapNotNull { s -> pairOrNull(s.employee?.id, s.account?.id) }
            .toSet()
        val display = displayWorkScheduleRepository
            .findConfirmedValidByEmployeeIdsAndDate(teamEmployeeIds, date)
            .filter { it.employee?.id in safetyEmployeeIds }
            .map { it.toDisplayWorkerItem(displayAttendedKeys) }

        val event = schedules
            .filter { it.workingCategory1 == WorkingCategory1.EVENT }
            .map { it.toWorkerItem() }

        return DailyWorkers(schedules = schedules, display = display, event = event)
    }

    private data class DailyWorkers(
        val schedules: List<TeamMemberSchedule>,
        val display: List<LeaderDailyWorkerItem>,
        val event: List<LeaderDailyWorkerItem>
    )

    fun emptyDailyStatus(date: LocalDate): LeaderDailyStatusResponse =
        LeaderDailyStatusResponse(
            date = date.toString(),
            summary = LeaderDailyStatusSummary(0, 0, 0, 0, 0),
            displayWorkers = emptyList(),
            eventWorkers = emptyList(),
            annualLeaveWorkers = emptyList(),
        )

    private fun TeamMemberSchedule.toWorkerItem(): LeaderDailyWorkerItem =
        LeaderDailyWorkerItem(
            scheduleId = id,
            displayWorkScheduleId = null,
            employeeId = employee?.id,
            employeeName = employee?.name.orEmpty(),
            employeeCode = employee?.employeeCode.orEmpty(),
            accountName = account?.name.orEmpty(),
            accountCode = account?.externalKey.orEmpty(),
            workingCategory1 = workingCategory1?.displayName,
            workingCategory2 = workingCategory2?.displayName,
            workingCategory3 = workingCategory3?.displayName,
            attended = attendanceLog != null,
        )

    /**
     * 진열 마스터 → 일별 현황 워커 항목 (레거시 `selectDisplayAccList` 컬럼 매핑).
     * 진열은 마스터 출처(조회 전용)라 편집용 schedule id 가 없어 scheduleId = 0.
     */
    private fun DisplayWorkSchedule.toDisplayWorkerItem(
        attendedKeys: Set<Pair<Long, Long>>
    ): LeaderDailyWorkerItem {
        val empId = employee?.id
        val accId = account?.id
        return LeaderDailyWorkerItem(
            scheduleId = 0L,
            displayWorkScheduleId = id,
            employeeId = empId,
            employeeName = employee?.name.orEmpty(),
            employeeCode = employee?.employeeCode.orEmpty(),
            accountName = account?.name.orEmpty(),
            accountCode = account?.externalKey.orEmpty(),
            workingCategory1 = typeOfWork1?.displayName,
            workingCategory2 = typeOfWork5?.displayName,
            workingCategory3 = typeOfWork3?.displayName,
            attended = empId != null && accId != null && (empId to accId) in attendedKeys,
        )
    }

    private fun pairOrNull(a: Long?, b: Long?): Pair<Long, Long>? =
        if (a != null && b != null) a to b else null
}
