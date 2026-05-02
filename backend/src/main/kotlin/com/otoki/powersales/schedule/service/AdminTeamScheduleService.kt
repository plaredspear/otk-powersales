package com.otoki.powersales.schedule.service

import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AdminTeamScheduleService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val adminEmployeeHolder: AdminEmployeeHolder,
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService,
    private val teamScheduleValidator: TeamScheduleValidator
) {

    fun getMembers(userId: Long): List<TeamMemberDto> {
        val currentEmployee = findEmployeeById(userId)
        val costCenterCode = currentEmployee.costCenterCode ?: return emptyList()
        return employeeRepository.findWithEmployeeInfoByCostCenterCodeAndAppAuthority(costCenterCode, "여사원")
            .filter { it.isDeleted != true }
            .map { TeamMemberDto.from(it) }
    }

    fun getAccounts(userId: Long, branchCode: String?): List<TeamScheduleAccountDto> {
        val effectiveBranchCode = branchCode ?: run {
            val currentEmployee = findEmployeeById(userId)
            currentEmployee.costCenterCode ?: return emptyList()
        }
        return accountRepository.findByBranchCodeAndAccountGroupIn(
            effectiveBranchCode, listOf("1010", "1000")
        )
            .filter { it.isDeleted != true }
            .map { TeamScheduleAccountDto.from(it) }
    }

    fun getBranches(): List<BranchResponse> {
        return employeeRepository.findDistinctBranches()
    }

    fun getMonthlySchedulesWithSummary(
        userId: Long,
        year: Int,
        month: Int,
        employeeIds: List<Long>?,
        accountIds: List<Int>?
    ): MonthlyScheduleWithSummaryDto {
        val hasEmployeeFilter = !employeeIds.isNullOrEmpty()
        val hasAccountFilter = !accountIds.isNullOrEmpty()

        if (!hasEmployeeFilter && !hasAccountFilter) {
            return MonthlyScheduleWithSummaryDto(schedules = emptyList(), dailySummary = emptyList())
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

        val scheduleDtos = uniqueSchedules.map { TeamScheduleDto.from(it) }

        val dailySummaryDtos = uniqueSchedules
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

        return MonthlyScheduleWithSummaryDto(schedules = scheduleDtos, dailySummary = dailySummaryDtos)
    }

    @Transactional
    fun createSchedule(userId: Long, request: TeamScheduleCreateRequest): TeamScheduleCreateResultDto {
        val employee = employeeRepository.findByEmployeeCode(request.employeeCode)
            .orElseThrow { TeamScheduleEmployeeNotFoundException() }

        teamScheduleValidator.validateEmployeeStatus(employee)

        if (request.workingType == "근무" && request.accountId == null) {
            throw TeamScheduleAccountRequiredException()
        }

        val workingDate = LocalDate.parse(request.workingDate, DateTimeFormatter.ISO_LOCAL_DATE)

        if (request.workingType == "근무" && request.workingCategory1 == "진열") {
            teamScheduleValidator.validateScheduleConflict(employee.id, workingDate, request.workingCategory3, null)
        }

        val account = if (request.accountId != null) {
            accountRepository.findById(request.accountId)
                .orElseThrow { TeamScheduleAccountNotFoundException() }
        } else null

        val currentEmployee = findEmployeeById(userId)
        val schedule = TeamMemberSchedule(
            employee = employee,
            workingDate = workingDate,
            workingType = request.workingType,
            workingCategory1 = request.workingCategory1,
            workingCategory2 = request.workingCategory2,
            workingCategory3 = request.workingCategory3,
            account = account,
            teamLeader = currentEmployee
        )
        val saved = teamMemberScheduleRepository.save(schedule)

        if (request.workingType == "근무" && account != null) {
            adminMonthlyIntegrationService.refreshIntegration(
                employee.id, account.id, YearMonth.from(workingDate)
            )
        }

        return TeamScheduleCreateResultDto(id = saved.id)
    }

    @Transactional
    fun updateSchedule(userId: Long, scheduleId: Long, request: TeamScheduleUpdateRequest) {
        val currentEmployee = findEmployeeById(userId)
        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamScheduleNotFoundException() }

        teamScheduleValidator.validateDisplayMasterLink(currentEmployee, schedule)

        val employee = schedule.employee
        if (employee != null) {
            teamScheduleValidator.validateEmployeeStatus(employee)
        }

        if (request.workingType == "근무" && request.accountId == null) {
            throw TeamScheduleAccountRequiredException()
        }

        val oldWorkingDate = schedule.workingDate
        val oldAccountId = schedule.account?.id
        val oldEmployeeId = schedule.employee?.id

        val newWorkingDate = LocalDate.parse(request.workingDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val dateChanged = schedule.workingDate != newWorkingDate
        val category3Changed = schedule.workingCategory3 != request.workingCategory3

        if (dateChanged && schedule.workingDate?.isBefore(LocalDate.now()) == true && schedule.workingCategory1 != "행사") {
            throw TeamSchedulePastDateChangeException()
        }

        if (request.workingType == "근무" && request.workingCategory1 == "진열" && (dateChanged || category3Changed)) {
            teamScheduleValidator.validateScheduleConflict(schedule.employee!!.id, newWorkingDate, request.workingCategory3, scheduleId)
        }

        val newAccount = if (request.accountId != null && request.accountId != schedule.account?.id) {
            accountRepository.findById(request.accountId)
                .orElseThrow { TeamScheduleAccountNotFoundException() }
        } else if (request.accountId == null) {
            null
        } else {
            schedule.account
        }

        schedule.workingDate = newWorkingDate
        schedule.workingType = request.workingType
        schedule.workingCategory1 = request.workingCategory1
        schedule.workingCategory3 = request.workingCategory3
        schedule.account = newAccount

        if (oldEmployeeId != null) {
            val refreshTargets = mutableSetOf<Triple<Long, Int, YearMonth>>()

            if (oldWorkingDate != null && oldAccountId != null) {
                refreshTargets.add(Triple(oldEmployeeId, oldAccountId, YearMonth.from(oldWorkingDate)))
            }
            if (request.workingType == "근무" && newAccount != null) {
                refreshTargets.add(Triple(oldEmployeeId, newAccount.id, YearMonth.from(newWorkingDate)))
            }

            for ((empId, accId, ym) in refreshTargets) {
                adminMonthlyIntegrationService.refreshIntegration(empId, accId, ym)
            }
        }
    }

    @Transactional
    fun deleteSchedule(userId: Long, scheduleId: Long) {
        val currentEmployee = findEmployeeById(userId)
        if (currentEmployee.appAuthority == "지점장") {
            throw TeamScheduleDeleteForbiddenException()
        }

        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamScheduleNotFoundException() }

        if (currentEmployee.appAuthority != "시스템관리자" && schedule.commuteLogId != null) {
            throw TeamScheduleWorkReportDeleteException()
        }

        teamScheduleValidator.validateDisplayMasterLink(currentEmployee, schedule)

        val employeeId = schedule.employee?.id
        val accountId = schedule.account?.id
        val workingDate = schedule.workingDate

        teamMemberScheduleRepository.delete(schedule)

        if (employeeId != null && accountId != null && workingDate != null && schedule.workingType == "근무") {
            adminMonthlyIntegrationService.refreshIntegration(employeeId, accountId, YearMonth.from(workingDate))
        }
    }

    // --- Private helpers ---

    private fun findEmployeeById(userId: Long): Employee {
        return adminEmployeeHolder.employee
            ?: employeeRepository.findWithEmployeeInfoById(userId)
            ?: throw TeamScheduleEmployeeNotFoundException()
    }

}
