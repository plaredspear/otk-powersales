package com.otoki.internal.common.service

import com.otoki.internal.common.dto.response.HomeResponse
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.safetycheck.service.SafetyCheckService
import com.otoki.internal.schedule.entity.Schedule
import com.otoki.internal.schedule.repository.ScheduleRepository
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
    private val scheduleRepository: ScheduleRepository,
    private val noticeRepository: NoticeRepository,
    private val accountRepository: AccountRepository,
    private val safetyCheckService: SafetyCheckService,
    private val shelfLifeRepository: ShelfLifeRepository
) {

    companion object {
        private const val NOTICE_DAYS = 7L
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /** 정렬 우선순위: 출근완료(0) → 임시배정(1) → 행사(2) → 진열(3) */
        private fun sortPriority(schedule: Schedule): Int {
            return when {
                schedule.commuteLogId != null -> 0
                schedule.workingCategory2?.contains("임시") == true -> 1
                schedule.workingCategory1 != "진열" -> 2
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
        val (schedules, userMap) = fetchSchedulesByRole(user, today)

        // 스케줄 → 거래처명 매핑 (batch fetch)
        val accountMap = fetchAccountMap(schedules)

        // 정렬 + 중복 제거 + DTO 변환
        val todaySchedules = schedules
            .sortedBy { sortPriority(it) }
            .distinctBy { it.sfid }
            .map { schedule -> toScheduleInfo(schedule, userMap, accountMap) }

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
                    type = notice.category?.apiCode ?: "",
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
     * @return Pair(스케줄 목록, sfid→User 매핑)
     */
    private fun fetchSchedulesByRole(user: User, today: LocalDate): Pair<List<Schedule>, Map<String, User>> {
        return when (user.role) {
            UserRole.LEADER -> {
                val teamUsers = userRepository.findByOrgName(user.orgName ?: "")
                val sfids = teamUsers.mapNotNull { it.sfid }
                val schedules = if (sfids.isNotEmpty()) {
                    scheduleRepository.findByWorkingDateAndEmployeeIdIn(today, sfids)
                } else {
                    emptyList()
                }
                val userMap = teamUsers.associateBy { it.sfid ?: "" }
                Pair(schedules, userMap)
            }
            else -> {
                val userSfid = user.sfid ?: ""
                val schedules = scheduleRepository.findByEmployeeIdAndWorkingDate(userSfid, today)
                val userMap = mapOf(userSfid to user)
                Pair(schedules, userMap)
            }
        }
    }

    /**
     * 스케줄의 accountId → Account 이름 매핑 (batch fetch)
     */
    private fun fetchAccountMap(schedules: List<Schedule>): Map<String, String> {
        val accountSfids = schedules.mapNotNull { it.accountId }.distinct()
        if (accountSfids.isEmpty()) return emptyMap()
        return accountRepository.findBySfidIn(accountSfids)
            .associate { (it.sfid ?: "") to (it.name ?: "") }
    }

    /**
     * Schedule entity → ScheduleInfo DTO 변환
     */
    private fun toScheduleInfo(
        schedule: Schedule,
        userMap: Map<String, User>,
        accountMap: Map<String, String>
    ): HomeResponse.ScheduleInfo {
        val employeeSfid = schedule.employeeId ?: ""
        val matchedUser = userMap[employeeSfid]
        return HomeResponse.ScheduleInfo(
            scheduleId = schedule.sfid ?: "",
            employeeName = matchedUser?.name ?: "",
            employeeSfid = employeeSfid,
            storeName = schedule.accountId?.let { accountMap[it] },
            storeSfid = schedule.accountId,
            workCategory = schedule.workingCategory1 ?: "",
            workType = schedule.workingType,
            isCommuteRegistered = schedule.commuteLogId != null,
            commuteRegisteredAt = schedule.commuteReportDatetime
        )
    }
}
