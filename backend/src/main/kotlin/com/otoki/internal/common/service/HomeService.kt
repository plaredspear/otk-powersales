package com.otoki.internal.common.service

import com.otoki.internal.common.dto.response.HomeResponse
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.safetycheck.service.SafetyCheckService
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.shelflife.repository.ShelfLifeRepository
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
    private val userRepository: UserRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val noticeRepository: NoticeRepository,
    private val accountRepository: AccountRepository,
    private val safetyCheckService: SafetyCheckService,
    private val shelfLifeRepository: ShelfLifeRepository
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
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val today = LocalDate.now()

        // 역할별 스케줄 조회
        val (teamMemberSchedules, userMap) = fetchSchedulesByRole(user, today)

        // 스케줄 → 거래처명 매핑 (batch fetch)
        val accountMap = fetchAccountMap(teamMemberSchedules)

        // 정렬 + 중복 제거 + DTO 변환
        val todaySchedules = teamMemberSchedules
            .sortedBy { sortPriority(it) }
            .distinctBy { it.sfid }
            .map { teamMemberSchedule -> toTeamMemberScheduleInfo(teamMemberSchedule, userMap, accountMap) }

        // 출근 현황 집계
        val attendanceSummary = HomeResponse.AttendanceSummaryInfo(
            totalCount = todaySchedules.size,
            registeredCount = todaySchedules.count { it.isCommuteRegistered }
        )

        // 안전점검 필요 여부 (조장은 항상 false)
        val safetyCheckRequired = when (user.role) {
            UserRole.USER -> {
                val todayStatus = safetyCheckService.getTodayStatus(userId)
                !todayStatus.completed
            }
            else -> false
        }

        val expiryCount = user.sfid?.let { sfid ->
            shelfLifeRepository.countByEmployeeIdAndAlarmDate(sfid, today)
        } ?: 0L

        val expiryAlert = HomeResponse.ExpiryAlertInfo(
            branchName = user.orgName ?: "",
            employeeName = user.name,
            employeeId = user.employeeId,
            expiryCount = expiryCount.toInt()
        )

        // 최근 1주일 공지사항 조회
        val since = LocalDateTime.of(today.minusDays(NOTICE_DAYS), LocalTime.MIN)
        val notices = noticeRepository
            .findRecentNotices(branch = user.orgName ?: "", since = since)
            .map { notice ->
                HomeResponse.NoticeInfo(
                    id = notice.id,
                    title = notice.name ?: "",
                    category = notice.category?.apiCode ?: "",
                    categoryName = notice.category?.displayName ?: "",
                    createdAt = notice.createdDate ?: LocalDateTime.MIN
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
     * @return Pair(스케줄 목록, employeeId→User 매핑)
     */
    private fun fetchSchedulesByRole(user: User, today: LocalDate): Pair<List<TeamMemberSchedule>, Map<String, User>> {
        return when (user.role) {
            UserRole.LEADER -> {
                val teamUsers = userRepository.findByOrgName(user.orgName ?: "")
                val employeeIds = teamUsers.map { it.employeeId }
                val teamMemberSchedules = if (employeeIds.isNotEmpty()) {
                    teamMemberScheduleRepository.findByWorkingDateAndEmployeeIdIn(today, employeeIds)
                } else {
                    emptyList()
                }
                val userMap = teamUsers.associateBy { it.employeeId }
                Pair(teamMemberSchedules, userMap)
            }
            else -> {
                val employeeId = user.employeeId
                val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employeeId, today)
                val userMap = mapOf(employeeId to user)
                Pair(teamMemberSchedules, userMap)
            }
        }
    }

    /**
     * 스케줄의 accountId → Account 이름 매핑 (batch fetch)
     */
    private fun fetchAccountMap(teamMemberSchedules: List<TeamMemberSchedule>): Map<String, String> {
        val accountSfids = teamMemberSchedules.mapNotNull { it.accountId }.distinct()
        if (accountSfids.isEmpty()) return emptyMap()
        return accountRepository.findBySfidIn(accountSfids)
            .associate { (it.sfid ?: "") to (it.name ?: "") }
    }

    /**
     * TeamMemberSchedule entity → TeamMemberScheduleInfo DTO 변환
     */
    private fun toTeamMemberScheduleInfo(
        teamMemberSchedule: TeamMemberSchedule,
        userMap: Map<String, User>,
        accountMap: Map<String, String>
    ): HomeResponse.TeamMemberScheduleInfo {
        val employeeId = teamMemberSchedule.employeeId ?: ""
        val matchedUser = userMap[employeeId]
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = teamMemberSchedule.sfid ?: "",
            employeeName = matchedUser?.name ?: "",
            employeeId = employeeId,
            storeName = teamMemberSchedule.accountId?.let { accountMap[it] },
            storeSfid = teamMemberSchedule.accountId,
            workCategory = teamMemberSchedule.workingCategory1 ?: "",
            workType = teamMemberSchedule.workingType,
            isCommuteRegistered = teamMemberSchedule.commuteLogId != null,
            commuteRegisteredAt = teamMemberSchedule.commuteReportDatetime
        )
    }
}
