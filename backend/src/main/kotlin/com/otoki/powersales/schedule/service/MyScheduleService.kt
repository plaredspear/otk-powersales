package com.otoki.powersales.schedule.service

import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.enums.WorkingType
// import com.otoki.powersales.schedule.repository.AttendanceRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
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

        // 해당 기간 내 일정이 있는 날짜 목록 조회
        val workDates = displayWorkScheduleRepository
            .findDistinctStartDatesByEmployeeIdAndDateBetween(employee.id, startDate, endDate)
            .toSet()

        // TeamMemberSchedule에서 날짜별 workingType 조회
        val memberSchedules = teamMemberScheduleRepository
            .findMonthlyByEmployeeIds(listOf(employee.id), startDate, endDate)
        val workingTypeByDate = memberSchedules
            .groupBy { it.workingDate }
            .mapValues { (_, schedules) -> schedules.firstOrNull()?.workingType?.displayName }

        // 연차/대휴 건수 카운트
        val annualLeaveCount = memberSchedules.count { it.workingType == WorkingType.ANNUAL_LEAVE }
        val substituteHolidayCount = memberSchedules.count { it.workingType == WorkingType.ALT_HOLIDAY }

        // 해당 월의 모든 날짜에 대해 근무 여부 판정
        val workDays = mutableListOf<WorkDayDto>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            workDays.add(
                WorkDayDto(
                    date = currentDate.toString(),
                    hasWork = workDates.contains(currentDate),
                    workingType = workingTypeByDate[currentDate]
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

        // 해당 날짜의 거래처 일정 목록 조회
        val schedules = displayWorkScheduleRepository.findByEmployeeAndStartDate(employee.id, date)

        // 거래처 목록 매핑 (등록 여부 - Attendance 비활성화로 항상 false)
        // Note: V1 리매핑으로 storeId(Long)→account(String sfid) 변경, DTO는 Long 유지 → 0L 대체
        val accountItems = schedules.map { schedule ->
            DisplayWorkScheduleItemDto(
                accountId = 0L,  // V1: account(String sfid)로 변경됨, DTO Long 타입 유지
                accountName = "",  // V1에서 accountName 삭제됨
                workType1 = schedule.typeOfWork1?.displayName ?: "",
                workType2 = "",
                workType3 = "",
                isRegistered = false  // Phase2: attendance 비활성화
            )
        }

        // 보고 진행 상황 계산
        val completed = accountItems.count { it.isRegistered }
        val total = accountItems.size
        val workType = schedules.firstOrNull()?.typeOfWork1?.displayName ?: ""

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
}
