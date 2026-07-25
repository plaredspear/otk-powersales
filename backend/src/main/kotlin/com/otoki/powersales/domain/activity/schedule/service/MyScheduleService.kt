package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.DailyScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.DisplayWorkScheduleItemDto
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.ReportProgressDto
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkDayDto
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
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
        // 날짜 라벨용 workingType. 연차/대휴가 섞인 날은 그 날 거래처 집계를 비우므로(isLeave),
        // 라벨도 연차/대휴를 우선 노출해야 "근무" 라벨 + 거래처 0건 이라는 모순이 생기지 않는다.
        val workingTypeByDate = schedulesByDate.mapValues { (_, schedules) ->
            val leave = schedules.firstOrNull {
                it.workingType == WorkingType.ANNUAL_LEAVE || it.workingType == WorkingType.ALT_HOLIDAY
            }
            (leave ?: schedules.firstOrNull())?.workingType?.displayName
        }

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
            val dayMasters = if (!isLeave) masters.filter { it.overlapsDate(currentDate) } else emptyList()
            val displayAccountIds = dayMasters.mapNotNull { it.account?.id }.toSet()

            // 행사 거래처: 안전점검 게이트 없이 EVENT team_member_schedule (레거시 selectHomeSchedulePromote 정합).
            val eventAccountIds = if (!isLeave) {
                daySchedules
                    .filter { it.workingCategory1 == WorkingCategory1.EVENT }
                    .mapNotNull { it.account?.id }
                    .toSet()
            } else {
                emptySet()
            }

            // 보고완료(sum): 그날 attendanceLog 존재하는 거래처 (진열/행사 공통, 레거시 commutelogid 유무).
            val attendedAccountIds = daySchedules
                .filter { it.attendanceLog != null }
                .mapNotNull { it.account?.id }
                .toSet()

            // 출처 선택은 일간 상세(getDailySchedule)와 동일한 레거시 승자독식 규칙을 따른다
            // (레거시 calSchedule 도 myDaily 와 같은 4단 우선순위를 쓰므로 월간 셀 카운트와
            //  일간 상세 건수가 어긋나지 않아야 한다).
            //   ① 출근등록 완료 → 진열 ∪ 행사 합집합 ② 진열 임시 → 진열 ③ 행사 → 행사 ④ 진열
            val hasRegistered = (displayAccountIds + eventAccountIds).any { it in attendedAccountIds }
            val hasTemporaryDisplay = dayMasters.any { it.typeOfWork5 == TypeOfWork5.TEMPORARY }

            val accountIds = when {
                hasRegistered -> displayAccountIds + eventAccountIds
                hasTemporaryDisplay -> displayAccountIds
                eventAccountIds.isNotEmpty() -> eventAccountIds
                else -> displayAccountIds
            }

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

        // 대휴/연차인 경우 거래처 목록 없이 반환.
        // 판정은 월간(getMonthlySchedule)과 동일하게 `any` 로 한다 — 조회에 정렬이 없어
        // `firstOrNull()` 은 DB 반환 순서에 의존하며, 같은 날 [근무, 연차] 2건이 섞이면
        // 월간 셀은 "근무 없음"인데 그 날을 탭하면 거래처가 나오는 불일치가 생긴다.
        val leaveSchedule = memberSchedules.firstOrNull {
            it.workingType == WorkingType.ALT_HOLIDAY || it.workingType == WorkingType.ANNUAL_LEAVE
        }
        if (leaveSchedule != null) {
            return DailyScheduleResponse(
                date = date.toString(),
                dayOfWeek = dayOfWeek,
                memberName = employee.name,
                employeeCode = employee.employeeCode,
                workingType = leaveSchedule.workingType?.displayName,
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

        // 진열 ∪ 행사 출처 선택 (레거시 MyPageController.myDaily 정합).
        //
        // 레거시는 사원×날짜 단위로 출처를 "배타적 승자독식"으로 하나 고른 뒤, 선택된 행의
        // workingcategory1 에 따라 진열/행사 중 한쪽 거래처 목록만 조회한다. 우선순위는
        //   ① 출근등록 완료 → ② 진열 임시(typeOfWork5=임시) → ③ 행사 → ④ 진열
        // 즉 출근등록도 없고 진열이 임시도 아니면 행사가 진열을 밀어낸다. 상시 진열이 걸린
        // 거래처에 행사가 배치되는 정상 운영 케이스에서 행사가 반드시 노출되어야 하기 때문이다.
        //
        // 과거 구현은 거래처 id 기준 dedup + 진열 우선이라, 상시 진열과 같은 거래처의 행사가
        // 영구 미노출됐다(진열이 items.first() 승자). 레거시와 정반대라 교정한다.
        val hasRegistered = displayAccountItems.any { it.isRegistered } || eventAccountItems.any { it.isRegistered }
        val hasTemporaryDisplay = schedules.any { it.typeOfWork5 == TypeOfWork5.TEMPORARY }

        val accountItems = when {
            // ① 출근등록 완료행이 있으면 진열·행사를 함께 노출하고 거래처 기준으로만 합친다.
            //
            //    신규 차이 (의도적): 레거시 MyPageController:180-188 은 출근등록 행이 있을 때
            //    `disAccList` 를 할당하지 않아(:178 빈 리스트 유지), 출근등록 행과 (근무유형1, 근무유형2)
            //    가 일치하지 않는 미등록 거래처를 버린다. 이는 `else` 분기에만 할당하는 비대칭으로
            //    의도로 보기 어렵고, 결과가 "한 거래처에 출근등록하면 근무유형이 다른 나머지 거래처가
            //    화면에서 사라짐" 이라 여사원이 그날 방문할 다른 매장을 놓치는 정보 손실이 된다.
            //    따라서 잔여 거래처를 누적 노출한다 (레거시 버그 미재현).
            hasRegistered -> (displayAccountItems + eventAccountItems)
                .groupBy { it.accountId }
                .flatMap { (accountId, items) ->
                    if (accountId == 0L) items
                    else listOf(items.firstOrNull { it.isRegistered } ?: items.first())
                }
            // ② 진열 임시가 있으면 진열 우선 (행사 배제).
            hasTemporaryDisplay -> displayAccountItems
            // ③ 행사가 있으면 행사 우선 (진열 배제).
            eventAccountItems.isNotEmpty() -> eventAccountItems
            // ④ 나머지는 진열.
            else -> displayAccountItems
        }

        // 보고 진행 상황 계산
        val completed = accountItems.count { it.isRegistered }
        val total = accountItems.size
        // 화면 상단 "N / M 보고 완료 (workType)" 라벨. 진열 마스터를 무조건 먼저 보면 위 우선순위에서
        // 행사가 선택된 날에도 "진열"로 표기되므로, 실제 선택된 목록의 workType1 을 쓴다.
        // ① 순위(출근등록)에서는 진열·행사가 섞이는데 삽입 순서상 진열이 앞이라, 출근등록한 행을
        // 먼저 보고 그 근무유형을 라벨로 쓴다.
        val labelSource = accountItems.firstOrNull { it.isRegistered } ?: accountItems.firstOrNull()
        val workType = labelSource?.workType1?.takeIf { it.isNotBlank() }
            ?: schedules.firstOrNull()?.typeOfWork1?.displayName
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
            // 레거시 myDaily 정합: 거래처명 오름차순 정렬 (MyPageController:193 personmergedList.sort,
            // name null 은 "" 취급 — 신규 accountName 은 이미 "" fallback).
            accounts = accountItems.sortedBy { it.accountName }
        )
    }

    /** 진열 마스터 기간(startDate~endDate, endDate NULL=무기한)이 특정 날짜를 포함하는지. */
    private fun DisplayWorkSchedule.overlapsDate(date: LocalDate): Boolean {
        val start = startDate ?: return false
        return !date.isBefore(start) && (endDate == null || !date.isAfter(endDate))
    }
}
