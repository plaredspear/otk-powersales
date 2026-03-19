package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.TeamScheduleCreateRequest
import com.otoki.internal.admin.dto.request.TeamScheduleUpdateRequest
import com.otoki.internal.admin.dto.response.*
import com.otoki.internal.admin.exception.*
import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AdminTeamScheduleService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository
) {

    fun getMembers(userId: Long): List<TeamMemberDto> {
        val currentUser = findUserById(userId)
        val costCenterCode = currentUser.costCenterCode ?: return emptyList()
        return userRepository.findByCostCenterCodeAndAppAuthority(costCenterCode, "여사원")
            .filter { it.isDeleted != true }
            .map { TeamMemberDto.from(it) }
    }

    fun getAccounts(userId: Long, branchCode: String?): List<TeamScheduleAccountDto> {
        val effectiveBranchCode = branchCode ?: run {
            val currentUser = findUserById(userId)
            currentUser.costCenterCode ?: return emptyList()
        }
        return accountRepository.findByBranchCodeAndAccountGroupIn(
            effectiveBranchCode, listOf("1010", "1000")
        )
            .filter { it.isDeleted != true }
            .map { TeamScheduleAccountDto.from(it) }
    }

    fun getBranches(): List<BranchResponse> {
        return userRepository.findDistinctBranches()
    }

    fun getMonthlySchedules(
        userId: Long,
        year: Int,
        month: Int,
        employeeIds: List<Long>?,
        accountIds: List<Int>?
    ): List<TeamScheduleDto> {
        val hasEmployeeFilter = !employeeIds.isNullOrEmpty()
        val hasAccountFilter = !accountIds.isNullOrEmpty()

        if (!hasEmployeeFilter && !hasAccountFilter) {
            return emptyList()
        }

        val yearMonth = YearMonth.of(year, month)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        val schedules = mutableSetOf<TeamMemberSchedule>()

        if (hasEmployeeFilter) {
            schedules.addAll(
                teamMemberScheduleRepository.findMonthlyByEmployeeIds(employeeIds!!, from, to)
            )
        }
        if (hasAccountFilter) {
            schedules.addAll(
                teamMemberScheduleRepository.findMonthlyByAccountIds(accountIds!!, from, to)
            )
        }

        val uniqueSchedules = schedules.distinctBy { it.id }
        val userMap = buildUserMap(uniqueSchedules)
        val accountMap = buildAccountMap(uniqueSchedules)

        return uniqueSchedules.map { TeamScheduleDto.from(it, userMap, accountMap) }
    }

    fun getDailySummary(
        userId: Long,
        year: Int,
        month: Int,
        employeeIds: List<Long>?,
        accountIds: List<Int>?
    ): List<DailySummaryDto> {
        val hasEmployeeFilter = !employeeIds.isNullOrEmpty()
        val hasAccountFilter = !accountIds.isNullOrEmpty()

        if (!hasEmployeeFilter && !hasAccountFilter) {
            return emptyList()
        }

        val yearMonth = YearMonth.of(year, month)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        val schedules = mutableSetOf<TeamMemberSchedule>()

        if (hasEmployeeFilter) {
            schedules.addAll(
                teamMemberScheduleRepository.findMonthlyByEmployeeIds(employeeIds!!, from, to)
            )
        }
        if (hasAccountFilter) {
            schedules.addAll(
                teamMemberScheduleRepository.findMonthlyByAccountIds(accountIds!!, from, to)
            )
        }

        val uniqueSchedules = schedules.distinctBy { it.id }

        return uniqueSchedules
            .groupBy { it.workingDate }
            .map { (date, daySchedules) ->
                val displaySchedules = daySchedules.filter { it.workingType == "근무" && it.workingCategory1 != "행사" }
                val promotionSchedules = daySchedules.filter { it.workingType == "근무" && it.workingCategory1 == "행사" }

                DailySummaryDto(
                    date = date?.toString() ?: "",
                    displayExpected = displaySchedules.size,
                    displayActual = displaySchedules.count { it.commuteLogId != null },
                    promotionExpected = promotionSchedules.size,
                    promotionActual = promotionSchedules.count { it.commuteLogId != null },
                    annualLeave = daySchedules.count { it.workingType == "연차" },
                    compensatoryLeave = daySchedules.count { it.workingType == "대휴" }
                )
            }
            .sortedBy { it.date }
    }

    @Transactional
    fun createSchedule(userId: Long, request: TeamScheduleCreateRequest): TeamScheduleCreateResultDto {
        val employee = userRepository.findByEmployeeNumber(request.employeeNumber)
            .orElseThrow { TeamScheduleEmployeeNotFoundException() }

        validateEmployeeStatus(employee)
        val workingDate = LocalDate.parse(request.workingDate, DateTimeFormatter.ISO_LOCAL_DATE)

        if (request.workingType == "근무" && request.workingCategory1 == "진열") {
            validateScheduleConflict(employee.id, workingDate, request.workingCategory3, null)
        }

        if (request.accountId != null) {
            accountRepository.findById(request.accountId)
                .orElseThrow { TeamScheduleAccountNotFoundException() }
        }

        val currentUser = findUserById(userId)
        val schedule = TeamMemberSchedule(
            employeeId = employee.id,
            workingDate = workingDate,
            workingType = request.workingType,
            workingCategory1 = request.workingCategory1,
            workingCategory2 = request.workingCategory2,
            workingCategory3 = request.workingCategory3,
            accountId = request.accountId,
            teamLeaderId = currentUser.id
        )
        val saved = teamMemberScheduleRepository.save(schedule)
        return TeamScheduleCreateResultDto(id = saved.id)
    }

    @Transactional
    fun updateSchedule(userId: Long, scheduleId: Long, request: TeamScheduleUpdateRequest) {
        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamScheduleNotFoundException() }

        val employee = schedule.employeeId?.let { userRepository.findById(it).orElse(null) }
        if (employee != null) {
            validateEmployeeStatus(employee)
        }

        val newWorkingDate = LocalDate.parse(request.workingDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val dateChanged = schedule.workingDate != newWorkingDate
        val category3Changed = schedule.workingCategory3 != request.workingCategory3

        if (request.workingType == "근무" && request.workingCategory1 == "진열" && (dateChanged || category3Changed)) {
            validateScheduleConflict(schedule.employeeId!!, newWorkingDate, request.workingCategory3, scheduleId)
        }

        if (request.accountId != null && request.accountId != schedule.accountId) {
            accountRepository.findById(request.accountId)
                .orElseThrow { TeamScheduleAccountNotFoundException() }
        }

        schedule.workingDate = newWorkingDate
        schedule.workingType = request.workingType
        schedule.workingCategory1 = request.workingCategory1
        schedule.workingCategory3 = request.workingCategory3
        schedule.accountId = request.accountId
    }

    @Transactional
    fun deleteSchedule(userId: Long, scheduleId: Long) {
        val currentUser = findUserById(userId)
        if (currentUser.appAuthority == "지점장") {
            throw TeamScheduleDeleteForbiddenException()
        }

        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamScheduleNotFoundException() }

        teamMemberScheduleRepository.delete(schedule)
    }

    // --- Private helpers ---

    private fun findUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { TeamScheduleEmployeeNotFoundException() }
    }

    private fun validateEmployeeStatus(employee: User) {
        when (employee.status) {
            "휴직" -> throw TeamScheduleEmployeeOnLeaveException()
            "퇴직" -> throw TeamScheduleEmployeeResignedException()
        }
    }

    private fun validateScheduleConflict(
        employeeId: Long,
        workingDate: LocalDate,
        workingCategory3: String?,
        excludeId: Long?
    ) {
        val existing = teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(employeeId, workingDate)
            .filter { it.id != excludeId }

        if (existing.isEmpty()) return

        when (workingCategory3) {
            "고정" -> {
                if (existing.any { it.workingCategory3 == "고정" }) {
                    throw TeamScheduleConflictException("해당 날짜에 고정 일정이 이미 존재합니다")
                }
                if (existing.isNotEmpty()) {
                    throw TeamScheduleConflictException("다른 유형의 일정이 있는 날짜에 고정을 추가할 수 없습니다")
                }
            }
            "격고" -> {
                if (existing.any { it.workingCategory3 == "고정" }) {
                    throw TeamScheduleConflictException("고정 일정이 있는 날짜에는 다른 유형을 추가할 수 없습니다")
                }
                if (existing.count { it.workingCategory3 == "격고" } >= 2) {
                    throw TeamScheduleConflictException("해당 날짜에 격고 일정이 이미 2건 존재합니다")
                }
            }
            "순회" -> {
                if (existing.any { it.workingCategory3 == "고정" }) {
                    throw TeamScheduleConflictException("고정 일정이 있는 날짜에는 다른 유형을 추가할 수 없습니다")
                }
            }
        }
    }

    private fun buildUserMap(schedules: List<TeamMemberSchedule>): Map<Long, User> {
        val employeeIds = schedules.mapNotNull { it.employeeId }.distinct()
        if (employeeIds.isEmpty()) return emptyMap()
        return userRepository.findAllById(employeeIds).associateBy { it.id }
    }

    private fun buildAccountMap(schedules: List<TeamMemberSchedule>): Map<Int, Account> {
        val accountIds = schedules.mapNotNull { it.accountId }.distinct()
        if (accountIds.isEmpty()) return emptyMap()
        return accountRepository.findByIdIn(accountIds).associateBy { it.id }
    }
}
