package com.otoki.powersales.common.service

import com.otoki.powersales.common.dto.response.HomeResponse
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.notice.repository.NoticeRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.safetycheck.service.SafetyCheckService
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.productexpiration.repository.ProductExpirationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 홈 화면 비즈니스 로직 Service
 */
@Service
@Transactional(readOnly = true)
class HomeService(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val noticeRepository: NoticeRepository,
    private val accountRepository: AccountRepository,
    private val safetyCheckService: SafetyCheckService,
    private val productExpirationRepository: ProductExpirationRepository
) {

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /**
         * 정렬 우선순위: 출근완료(0) → 행사(1) → 진열(2).
         *
         * SF 레거시(`FullCalendarComponentController`) 는 이름순 정렬만 적용하므로 4단계 정렬은 신규 도입 정책.
         * 기존 "임시배정" 분기는 `WorkingCategory2.TEMPORARY` 옵션을 SF picklist 정합으로 제거하면서 함께 제거
         * (sf-align-teammemberschedule #762 — SF 레거시 동등 분기 부재 확인 완료).
         */
        private fun sortPriority(teamMemberSchedule: TeamMemberSchedule): Int {
            return when {
                teamMemberSchedule.attendanceLog != null -> 0
                teamMemberSchedule.workingCategory1 != WorkingCategory1.DISPLAY -> 1
                else -> 2
            }
        }
    }

    /**
     * 홈 화면 데이터 통합 조회
     *
     * 역할별 분기:
     * - USER(여사원): 본인 스케줄, 안전점검 확인
     * - LEADER(조장): 팀 전체 스케줄, 안전점검 항상 false
     */
    fun getHomeData(userId: Long): HomeResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val today = LocalDate.now()

        // 역할별 TMS 조회 (행사 일정 집계 + 진열 출근여부 조회용)
        val (teamMemberSchedules, employeeMap) = fetchSchedulesByRole(employee, today)

        // 행사 일정: TMS 중 workingCategory1 = 행사 만 집계.
        // 레거시 `selectHomeSchedulePromote` 의 `workingcategory1__c = '행사'` 하드 필터 정합 —
        // 레거시는 진열 TMS 행을 홈 일정으로 카운트하지 않는다 (진열은 아래 확정 마스터에서만).
        val eventSchedules = teamMemberSchedules.filter { it.workingCategory1 == WorkingCategory1.EVENT }

        // 진열 출근여부 조회용: TMS 중 workingCategory1 = 진열 → (employeeId, accountId) 매핑.
        // 레거시 `selectHomeScheduleDisplay` 의 dtc2 LEFT JOIN(workingcategory1='진열') 정합 —
        // 진열 일정 존재 판정은 확정 마스터로, 출근여부(commutelogid)는 진열 TMS 에서 읽는다.
        val displayTmsByKey = teamMemberSchedules
            .filter { it.workingCategory1 == WorkingCategory1.DISPLAY }
            .mapNotNull { tms ->
                val empId = tms.employee?.id
                val accId = tms.account?.id
                if (empId != null && accId != null) Pair(empId, accId) to tms else null
            }
            .toMap()

        // 진열 일정: 확정 진열마스터 (레거시 `selectHomeScheduleDisplay` 의 `confirmed__c is true` 정합)
        val displayWorkSchedules = fetchDisplaySchedulesByRole(employee, employeeMap, today)

        // 스케줄 → 거래처명 매핑 (batch fetch)
        val accountMap = fetchAccountMap(eventSchedules, displayWorkSchedules)

        // 행사 TMS 정렬 + 중복 제거 + DTO 변환
        val eventInfos = eventSchedules
            .sortedBy { sortPriority(it) }
            .distinctBy { it.id }
            .map { tms -> toTeamMemberScheduleInfo(tms, employeeMap, accountMap) }

        // 확정 진열마스터 → DTO 변환 (출근여부는 매칭되는 진열 TMS 에서 읽음, 진열 → 항상 마지막 정렬)
        val displayInfos = displayWorkSchedules.map { dws ->
            toDisplayWorkScheduleInfo(dws, employeeMap, accountMap, displayTmsByKey)
        }

        val todaySchedules = eventInfos + displayInfos

        // 출근/근태 영역 노출 대상 여부 (레거시 home.jsp: appauthority ∈ {여사원, 조장} 만 노출)
        // 지점장 / AccountViewAll / null(미매핑) 은 출근 영역 비노출 — 모바일이 이 플래그로 카드 자체를 숨긴다.
        val attendanceApplicable =
            employee.role == AppAuthority.WOMAN || employee.role == AppAuthority.LEADER

        // 출근 현황 집계
        val attendanceSummary = HomeResponse.AttendanceSummaryInfo(
            totalCount = todaySchedules.size,
            registeredCount = todaySchedules.count { it.isCommuteRegistered }
        )

        // 안전점검 필요 여부 (조장은 항상 false)
        val safetyCheckRequired = if (employee.role == AppAuthority.WOMAN) {
            val todayStatus = safetyCheckService.getTodayStatus(userId)
            !todayStatus.completed
        } else {
            false
        }

        val expiryCount = productExpirationRepository.countByEmployeeIdAndAlarmDate(employee.id, today)

        val expiryAlert = HomeResponse.ExpiryAlertInfo(
            branchName = employee.orgName ?: "",
            employeeName = employee.name,
            employeeCode = employee.employeeCode,
            expiryCount = expiryCount.toInt()
        )

        // 최근 공지사항 조회 (최신 5건)
        val notices = noticeRepository
            .findRecentNotices(branch = employee.orgName ?: "")
            .map { notice ->
                HomeResponse.NoticeInfo(
                    id = notice.id,
                    title = notice.name ?: "",
                    category = notice.category?.apiCode ?: "",
                    categoryName = notice.category?.homeDisplayName ?: "",
                    createdAt = notice.createdAt ?: LocalDateTime.MIN
                )
            }

        return HomeResponse(
            todaySchedules = todaySchedules,
            attendanceSummary = attendanceSummary,
            attendanceApplicable = attendanceApplicable,
            safetyCheckRequired = safetyCheckRequired,
            expiryAlert = expiryAlert,
            notices = notices,
            currentDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * 역할별 스케줄 조회
     * @return Pair(스케줄 목록, employeeId→Employee 매핑)
     */
    private fun fetchSchedulesByRole(employee: Employee, today: LocalDate): Pair<List<TeamMemberSchedule>, Map<Long, Employee>> {
        return when (employee.role) {
            AppAuthority.LEADER -> {
                val teamEmployees = employeeRepository.findByOrgName(employee.orgName ?: "")
                val employeeIds = teamEmployees.map { it.id }
                val teamMemberSchedules = if (teamEmployees.isNotEmpty()) {
                    teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(today, teamEmployees)
                } else {
                    emptyList()
                }
                val employeeMap = teamEmployees.associateBy { it.id }
                Pair(teamMemberSchedules, employeeMap)
            }
            else -> {
                val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)
                val employeeMap = mapOf(employee.id to employee)
                Pair(teamMemberSchedules, employeeMap)
            }
        }
    }

    /**
     * 역할별 확정 진열마스터 조회
     */
    private fun fetchDisplaySchedulesByRole(
        employee: Employee,
        employeeMap: Map<Long, Employee>,
        today: LocalDate
    ): List<DisplayWorkSchedule> {
        val employeeIds = employeeMap.keys.toList()
        if (employeeIds.isEmpty()) return emptyList()
        return displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(employeeIds, today)
    }

    /**
     * 스케줄의 accountId → Account 이름 매핑 (batch fetch)
     */
    private fun fetchAccountMap(
        teamMemberSchedules: List<TeamMemberSchedule>,
        displayWorkSchedules: List<DisplayWorkSchedule> = emptyList()
    ): Map<Long, String> {
        val accountIds = (
            teamMemberSchedules.mapNotNull { it.account?.id } +
            displayWorkSchedules.mapNotNull { it.account?.id }
        ).distinct()
        if (accountIds.isEmpty()) return emptyMap()
        return accountRepository.findByIdIn(accountIds)
            .associate { it.id to (it.name ?: "") }
    }

    /**
     * TeamMemberSchedule entity → TeamMemberScheduleInfo DTO 변환
     */
    private fun toTeamMemberScheduleInfo(
        teamMemberSchedule: TeamMemberSchedule,
        employeeMap: Map<Long, Employee>,
        accountMap: Map<Long, String>
    ): HomeResponse.TeamMemberScheduleInfo {
        val matchedEmployee = teamMemberSchedule.employee?.id?.let { employeeMap[it] }
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = teamMemberSchedule.id,
            employeeName = matchedEmployee?.name ?: "",
            employeeCode = matchedEmployee?.employeeCode ?: "",
            accountName = teamMemberSchedule.account?.id?.let { accountMap[it] },
            accountId = teamMemberSchedule.account?.id,
            workCategory = teamMemberSchedule.workingCategory1?.displayName ?: "",
            workType = teamMemberSchedule.workingType?.displayName,
            isCommuteRegistered = teamMemberSchedule.attendanceLog != null,
            commuteRegisteredAt = teamMemberSchedule.commuteReportDatetime
        )
    }

    /**
     * DisplayWorkSchedule entity → TeamMemberScheduleInfo DTO 변환
     * 레거시 동작: 확정 진열마스터가 홈화면 스케줄에 포함되어 출근 등록 가능.
     * 출근여부는 매칭되는 진열 TMS([displayTmsByKey])에서 읽는다
     * (레거시 `selectHomeScheduleDisplay` 의 dtc2 LEFT JOIN 정합).
     */
    private fun toDisplayWorkScheduleInfo(
        displayWorkSchedule: DisplayWorkSchedule,
        employeeMap: Map<Long, Employee>,
        accountMap: Map<Long, String>,
        displayTmsByKey: Map<Pair<Long, Long>, TeamMemberSchedule>
    ): HomeResponse.TeamMemberScheduleInfo {
        val matchedEmployee = displayWorkSchedule.employee?.id?.let { employeeMap[it] }
        val empId = displayWorkSchedule.employee?.id
        val accId = displayWorkSchedule.account?.id
        val matchedTms = if (empId != null && accId != null) displayTmsByKey[Pair(empId, accId)] else null
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = 0,
            displayWorkScheduleId = displayWorkSchedule.id,
            employeeName = matchedEmployee?.name ?: "",
            employeeCode = matchedEmployee?.employeeCode ?: "",
            accountName = displayWorkSchedule.account?.id?.let { accountMap[it] },
            accountId = displayWorkSchedule.account?.id,
            workCategory = displayWorkSchedule.typeOfWork1?.displayName ?: "진열",
            workType = displayWorkSchedule.typeOfWork3?.displayName,
            isCommuteRegistered = matchedTms?.attendanceLog != null,
            commuteRegisteredAt = matchedTms?.commuteReportDatetime
        )
    }
}
