package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.DailyScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.DisplayWorkScheduleItemDto
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.ReportProgressDto
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkDayDto
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
// import com.otoki.powersales.domain.activity.schedule.repository.AttendanceRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class MyScheduleService(
    private val employeeRepository: EmployeeRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
    // private val attendanceRepository: AttendanceRepository  // Phase2: PG 대응 테이블 없음
) {

    companion object {
        private val DAY_OF_WEEK_KR = mapOf(
            DayOfWeek.MONDAY to "월",
            DayOfWeek.TUESDAY to "화",
            DayOfWeek.WEDNESDAY to "수",
            DayOfWeek.THURSDAY to "목",
            DayOfWeek.FRIDAY to "금",
            DayOfWeek.SATURDAY to "토",
            DayOfWeek.SUNDAY to "일"
        )
    }

    /**
     * 월간 일정 조회
     * 특정 연/월의 근무일 여부를 날짜별로 반환
     */
    fun getMonthlySchedule(userId: Long, year: Int, month: Int): MonthlyScheduleResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        // YearMonth로 해당 월의 시작일/종료일 계산
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        // 진열 마스터 (확정·기간유효, 월과 기간 겹침). 마스터는 기간형이므로 그날이 마스터 기간에
        // 포함되면 진열 근무일로 본다(안전점검 제출 여부와 무관).
        //
        // 여사원 본인 앱 캘린더는 "내가 그날 가야 할 진열 계획"을 보여주는 사전 안내 화면이다.
        // 레거시 SF 모바일(FullCalendarComponentController)은 안전점검을 진열 표시의 전제로 걸지
        // 않는다 — 안전점검은 근무 당일 여사원이 남기는 하류 결과물이라 근무 전에는 존재하지 않으며,
        // 게이트로 걸면 예정 진열 근무가 캘린더에서 사라져 계획 안내 기능 자체를 잃는다.
        // (안전점검 제출자만 노출하는 comm_cnt>0 게이트는 조장이 조원 실적을 확인하는 화면
        //  [TeamDailyStatusCalculator]의 사후 실적 기준이며, 본인 계획 화면에는 적용하지 않는다.)
        val masters = displayWorkScheduleRepository
            .findConfirmedValidByEmployeeIdAndDateRange(employee.id, startDate, endDate)

        // TeamMemberSchedule: 행사 거래처·출근(attendanceLog)·workingType(연차/대휴) 소스. (account/attendanceLog fetch join)
        val memberSchedules = teamMemberScheduleRepository
            .findMonthlyByEmployeeIds(listOf(employee.id), startDate, endDate)
        val schedulesByDate = memberSchedules.groupBy { it.workingDate }
        val workingTypeByDate = memberSchedules
            .groupBy { it.workingDate }
            .mapValues { (_, schedules) -> schedules.firstOrNull()?.workingType?.displayName }

        // 연차/대휴 건수 카운트
        val annualLeaveCount = memberSchedules.count { it.workingType == WorkingType.ANNUAL_LEAVE }
        val substituteHolidayCount = memberSchedules.count { it.workingType == WorkingType.ALT_HOLIDAY }

        // 날짜별 근무 여부 + 보고완료/총건 산출 (레거시 calSchedule 셀 = sum/cnt, cnt>0 만 표시).
        val workDays = mutableListOf<WorkDayDto>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val daySchedules = schedulesByDate[currentDate].orEmpty()

            // 대휴/연차 날은 거래처 집계 없음 (레거시 mngDaily: workingType=연차/대휴 → accList 비움).
            val isLeave = daySchedules.any {
                it.workingType == WorkingType.ANNUAL_LEAVE || it.workingType == WorkingType.ALT_HOLIDAY
            }

            // 진열 거래처: 마스터 기간이 그날을 포함하면 노출 (안전점검 게이트 없음 — 여사원 본인 계획 화면).
            val displayAccountIds = if (!isLeave) {
                masters.filter { it.overlapsDate(currentDate) }.mapNotNull { it.account?.id }.toSet()
            } else {
                emptySet()
            }

            // 행사 거래처: 안전점검 게이트 없이 EVENT team_member_schedule (레거시 selectHomeSchedulePromote 정합).
            val eventAccountIds = if (!isLeave) {
                daySchedules
                    .filter { it.workingCategory1 == WorkingCategory1.EVENT }
                    .mapNotNull { it.account?.id }
                    .toSet()
            } else {
                emptySet()
            }

            val accountIds = displayAccountIds + eventAccountIds

            // 보고완료(sum): 그날 attendanceLog 존재하는 거래처 (진열/행사 공통, 레거시 commutelogid 유무).
            val attendedAccountIds = daySchedules
                .filter { it.attendanceLog != null }
                .mapNotNull { it.account?.id }
                .toSet()

            workDays.add(
                WorkDayDto(
                    date = currentDate.toString(),
                    hasWork = accountIds.isNotEmpty(),
                    workingType = workingTypeByDate[currentDate],
                    completedCount = accountIds.count { it in attendedAccountIds },
                    totalCount = accountIds.size
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return MonthlyScheduleResponse(
            year = year,
            month = month,
            workDays = workDays,
            annualLeaveCount = annualLeaveCount,
            substituteHolidayCount = substituteHolidayCount
        )
    }

    /**
     * 일간 일정 상세 조회
     * 특정 날짜의 거래처별 근무 정보 및 등록 현황 반환
     */
    fun getDailySchedule(userId: Long, date: LocalDate): DailyScheduleResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        // 요일 계산
        val dayOfWeek = DAY_OF_WEEK_KR[date.dayOfWeek] ?: ""

        // 해당 날짜의 TeamMemberSchedule에서 workingType 조회
        val memberSchedules = teamMemberScheduleRepository
            .findByEmployeeIdAndWorkingDate(employee.id, date)
        val workingType = memberSchedules.firstOrNull()?.workingType

        // 대휴/연차인 경우 거래처 목록 없이 반환
        if (workingType == WorkingType.ALT_HOLIDAY || workingType == WorkingType.ANNUAL_LEAVE) {
            return DailyScheduleResponse(
                date = date.toString(),
                dayOfWeek = dayOfWeek,
                memberName = employee.name,
                employeeCode = employee.employeeCode,
                workingType = workingType.displayName,
                reportProgress = ReportProgressDto(
                    completed = 0,
                    total = 0,
                    workType = ""
                ),
                accounts = emptyList()
            )
        }

        // 해당 날짜의 거래처 일정 목록 조회.
        // 진열 마스터는 기간형(startDate~endDate)이므로 시작일 단일 매칭이 아니라 기간 겹침으로 조회한다
        // (레거시 calSchedule/myDaily GENERATE_SERIES 정합, 홈/여사원 일별현황과 동일 기준).
        val schedules = displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(employee.id, date)

        // 출근 등록 완료된 거래처 id 집합
        // 레거시 myDaily.jsp: displayworkschedulemaster ⨝ teammemberschedule(같은 거래처·날짜)의
        //   commutelogid__c 유무로 등록 완료 판정. 신규는 TeamMemberSchedule.attendanceLog(백링크)로 대응.
        //   (attendance_log_id는 nullable LAZY FK → null/proxy 판별에 추가 쿼리 없음)
        val registeredAccountIds = memberSchedules
            .filter { it.attendanceLog != null }
            .mapNotNull { it.account?.id }
            .toSet()

        // 진열 거래처 매핑
        // 레거시 myDaily.jsp: 거래처명 | typeOfWork1 / typeOfWork5 / typeOfWork3
        //   workingcategory1 ← typeOfWork1(진열), workingcategory2 ← typeOfWork5(전담 등),
        //   workingcategory3 ← typeOfWork3(고정/격고/순회)
        val displayAccountItems = schedules.map { schedule ->
            val accountId = schedule.account?.id
            DisplayWorkScheduleItemDto(
                accountId = accountId ?: 0L,
                accountName = schedule.account?.name ?: "",
                workType1 = schedule.typeOfWork1?.displayName ?: "",
                workType2 = schedule.typeOfWork5?.displayName ?: "",
                workType3 = schedule.typeOfWork3?.displayName ?: "",
                isRegistered = accountId != null && accountId in registeredAccountIds
            )
        }

        // 행사 거래처 매핑 (레거시 selectAccList 행사 분기): EVENT TeamMemberSchedule 행에서 직접 소싱.
        // 진열 마스터에 없는 행사 전용일도 거래처가 표시되도록 한다.
        val eventAccountItems = memberSchedules
            .filter { it.workingCategory1 == WorkingCategory1.EVENT && it.account != null }
            .map { ms ->
                DisplayWorkScheduleItemDto(
                    accountId = ms.account?.id ?: 0L,
                    accountName = ms.account?.name ?: "",
                    workType1 = ms.workingCategory1?.displayName ?: "",
                    workType2 = ms.workingCategory2?.displayName ?: "",
                    workType3 = ms.workingCategory3?.displayName ?: "",
                    isRegistered = ms.attendanceLog != null
                )
            }

        // 진열 ∪ 행사 후 거래처 id 기준 중복 제거 (레거시 정합: 출근완료 항목 우선).
        // accountId = 0L(식별 불가)은 합치지 않고 그대로 둔다.
        val accountItems = (displayAccountItems + eventAccountItems)
            .groupBy { it.accountId }
            .flatMap { (accountId, items) ->
                if (accountId == 0L) items
                else listOf(items.firstOrNull { it.isRegistered } ?: items.first())
            }

        // 보고 진행 상황 계산
        val completed = accountItems.count { it.isRegistered }
        val total = accountItems.size
        val workType = schedules.firstOrNull()?.typeOfWork1?.displayName
            ?: eventAccountItems.firstOrNull()?.workType1
            ?: ""

        return DailyScheduleResponse(
            date = date.toString(),
            dayOfWeek = dayOfWeek,
            memberName = employee.name,
            employeeCode = employee.employeeCode,
            workingType = null,
            reportProgress = ReportProgressDto(
                completed = completed,
                total = total,
                workType = workType
            ),
            accounts = accountItems
        )
    }

    /** 진열 마스터 기간(startDate~endDate, endDate NULL=무기한)이 특정 날짜를 포함하는지. */
    private fun DisplayWorkSchedule.overlapsDate(date: LocalDate): Boolean {
        val start = startDate ?: return false
        return !date.isBefore(start) && (endDate == null || !date.isAfter(endDate))
    }
}
