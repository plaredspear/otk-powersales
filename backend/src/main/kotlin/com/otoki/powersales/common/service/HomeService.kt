package com.otoki.powersales.common.service

import com.otoki.powersales.common.dto.response.HomeResponse
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.auth.entity.UserRole
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

        /** 정렬 우선순위: 출근완료(0) → 임시배정(1) → 행사(2) → 진열(3) */
        private fun sortPriority(teamMemberSchedule: TeamMemberSchedule): Int {
            return when {
                teamMemberSchedule.commuteLogId != null -> 0
                teamMemberSchedule.workingCategory2?.contains("임시") == true -> 1
                teamMemberSchedule.workingCategory1 != "진열" -> 2
                else -> 3
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

        // 역할별 스케줄 조회
        val (teamMemberSchedules, employeeMap) = fetchSchedulesByRole(employee, today)

        // 확정 진열마스터 조회 (TMS에 없는 거래처만 추가)
        val displayWorkSchedules = fetchDisplaySchedulesByRole(employee, employeeMap, today)
        val tmsAccountKeys = teamMemberSchedules.mapNotNull { tms ->
            val empId = tms.employee?.id
            val accId = tms.account?.id
            if (empId != null && accId != null) Pair(empId, accId) else null
        }.toSet()
        val additionalDisplaySchedules = displayWorkSchedules.filter { dws ->
            val empId = dws.employee?.id
            val accId = dws.account?.id
            empId != null && accId != null && Pair(empId, accId) !in tmsAccountKeys
        }

        // 스케줄 → 거래처명 매핑 (batch fetch)
        val accountMap = fetchAccountMap(teamMemberSchedules, additionalDisplaySchedules)

        // TMS 정렬 + 중복 제거 + DTO 변환
        val tmsInfos = teamMemberSchedules
            .sortedBy { sortPriority(it) }
            .distinctBy { it.id }
            .map { tms -> toTeamMemberScheduleInfo(tms, employeeMap, accountMap) }

        // DWS → DTO 변환 (진열, 미출근 → 항상 마지막 정렬)
        val dwsInfos = additionalDisplaySchedules.map { dws ->
            toDisplayWorkScheduleInfo(dws, employeeMap, accountMap)
        }

        val todaySchedules = tmsInfos + dwsInfos

        // 출근 현황 집계
        val attendanceSummary = HomeResponse.AttendanceSummaryInfo(
            totalCount = todaySchedules.size,
            registeredCount = todaySchedules.count { it.isCommuteRegistered }
        )

        // 안전점검 필요 여부 (조장은 항상 false)
        val safetyCheckRequired = if (employee.role == UserRole.WOMAN) {
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
                    categoryName = notice.category?.displayName ?: "",
                    createdAt = notice.createdAt ?: LocalDateTime.MIN
                )
            }

        return HomeResponse(
            todaySchedules = todaySchedules,
            attendanceSummary = attendanceSummary,
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
            UserRole.LEADER -> {
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
    ): Map<Int, String> {
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
        accountMap: Map<Int, String>
    ): HomeResponse.TeamMemberScheduleInfo {
        val matchedEmployee = teamMemberSchedule.employee?.id?.let { employeeMap[it] }
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = teamMemberSchedule.id,
            employeeName = matchedEmployee?.name ?: "",
            employeeCode = matchedEmployee?.employeeCode ?: "",
            accountName = teamMemberSchedule.account?.id?.let { accountMap[it] },
            accountId = teamMemberSchedule.account?.id,
            workCategory = teamMemberSchedule.workingCategory1 ?: "",
            workType = teamMemberSchedule.workingType,
            isCommuteRegistered = teamMemberSchedule.commuteLogId != null,
            commuteRegisteredAt = teamMemberSchedule.commuteReportDatetime
        )
    }

    /**
     * DisplayWorkSchedule entity → TeamMemberScheduleInfo DTO 변환
     * 레거시 동작: 확정 진열마스터가 홈화면 스케줄에 포함되어 출근 등록 가능
     */
    private fun toDisplayWorkScheduleInfo(
        displayWorkSchedule: DisplayWorkSchedule,
        employeeMap: Map<Long, Employee>,
        accountMap: Map<Int, String>
    ): HomeResponse.TeamMemberScheduleInfo {
        val matchedEmployee = displayWorkSchedule.employee?.id?.let { employeeMap[it] }
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = 0,
            displayWorkScheduleId = displayWorkSchedule.id,
            employeeName = matchedEmployee?.name ?: "",
            employeeCode = matchedEmployee?.employeeCode ?: "",
            accountName = displayWorkSchedule.account?.id?.let { accountMap[it] },
            accountId = displayWorkSchedule.account?.id,
            workCategory = displayWorkSchedule.typeOfWork1 ?: "진열",
            workType = displayWorkSchedule.typeOfWork3,
            isCommuteRegistered = false,
            commuteRegisteredAt = null
        )
    }
}
