package com.otoki.internal.common.service

import com.otoki.internal.common.dto.response.HomeResponse
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.safetycheck.service.SafetyCheckService
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.productexpiration.repository.ProductExpirationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 홈 화면 비즈니스 로직 Service
 */
@Service
@Transactional(readOnly = true)
class HomeService(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val noticeRepository: NoticeRepository,
    private val accountRepository: AccountRepository,
    private val safetyCheckService: SafetyCheckService,
    private val productExpirationRepository: ProductExpirationRepository
) {

    companion object {
        private const val NOTICE_DAYS = 7L
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

        // 스케줄 → 거래처명 매핑 (batch fetch)
        val accountMap = fetchAccountMap(teamMemberSchedules)

        // 정렬 + 중복 제거 + DTO 변환
        val todaySchedules = teamMemberSchedules
            .sortedBy { sortPriority(it) }
            .distinctBy { it.id }
            .map { teamMemberSchedule -> toTeamMemberScheduleInfo(teamMemberSchedule, employeeMap, accountMap) }

        // 출근 현황 집계
        val attendanceSummary = HomeResponse.AttendanceSummaryInfo(
            totalCount = todaySchedules.size,
            registeredCount = todaySchedules.count { it.isCommuteRegistered }
        )

        // 안전점검 필요 여부 (조장은 항상 false)
        val safetyCheckRequired = when (employee.role) {
            UserRole.USER -> {
                val todayStatus = safetyCheckService.getTodayStatus(userId)
                !todayStatus.completed
            }
            else -> false
        }

        val expiryCount = productExpirationRepository.countByEmployeeIdAndAlarmDate(employee.id, today)

        val expiryAlert = HomeResponse.ExpiryAlertInfo(
            branchName = employee.orgName ?: "",
            employeeName = employee.name,
            employeeCode = employee.employeeCode,
            expiryCount = expiryCount.toInt()
        )

        // 최근 1주일 공지사항 조회
        val since = LocalDateTime.of(today.minusDays(NOTICE_DAYS), LocalTime.MIN)
        val notices = noticeRepository
            .findRecentNotices(branch = employee.orgName ?: "", since = since)
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
                val teamMemberSchedules = if (employeeIds.isNotEmpty()) {
                    teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(today, employeeIds)
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
     * 스케줄의 accountId → Account 이름 매핑 (batch fetch)
     */
    private fun fetchAccountMap(teamMemberSchedules: List<TeamMemberSchedule>): Map<Int, String> {
        val accountIds = teamMemberSchedules.mapNotNull { it.accountId }.distinct()
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
        val matchedEmployee = teamMemberSchedule.employeeId?.let { employeeMap[it] }
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = teamMemberSchedule.id,
            employeeName = matchedEmployee?.name ?: "",
            employeeCode = matchedEmployee?.employeeCode ?: "",
            accountName = teamMemberSchedule.accountId?.let { accountMap[it] },
            accountSfid = teamMemberSchedule.accountId?.toString(),
            workCategory = teamMemberSchedule.workingCategory1 ?: "",
            workType = teamMemberSchedule.workingType,
            isCommuteRegistered = teamMemberSchedule.commuteLogId != null,
            commuteRegisteredAt = teamMemberSchedule.commuteReportDatetime
        )
    }
}
