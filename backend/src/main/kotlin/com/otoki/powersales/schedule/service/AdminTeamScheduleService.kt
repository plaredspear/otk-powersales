package com.otoki.powersales.schedule.service

import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingType
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
import com.otoki.powersales.organization.repository.OrganizationRepository
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
    private val organizationRepository: OrganizationRepository,
    private val adminEmployeeHolder: AdminEmployeeHolder,
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService,
    private val teamScheduleValidator: TeamScheduleValidator
) {

    fun getMembers(userId: Long): List<TeamMemberDto> {
        val currentEmployee = findEmployeeById(userId)
        val costCenterCode = currentEmployee.costCenterCode ?: return emptyList()
        return employeeRepository.findWithEmployeeInfoByCostCenterCodeAndRole(costCenterCode, UserRole.WOMAN)
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

    /**
     * 여사원 일정관리 "지점 선택" 드롭다운 옵션.
     *
     * SF 레거시 `ScheduleSearchByTeamMemberController.init()` → `CurrentUserBranchNameList.getOrgList()` 정합.
     * - SYSTEM_ADMIN: `Organization` 전체에서 distinct(Level5 우선)
     * - ALL_BRANCHES Role (영업지원/본부): SF `RT.Name in ('영업지원실','영업본부')` 분기 (CVS 미포함)
     * - 그 외 (LEADER, BRANCH_MANAGER, WOMAN 등): 본인 `costCenterCode` 기준 조직 트리 + Retail/제1/CVS사업부
     */
    fun getBranches(userId: Long): List<BranchResponse> {
        val currentEmployee = findEmployeeById(userId)
        val role = currentEmployee.role

        return when {
            role == UserRole.SYSTEM_ADMIN -> organizationRepository.findAllTeamScheduleBranches()
            role != null && UserRole.ALL_BRANCHES.contains(role) ->
                organizationRepository.findTeamScheduleBranches(hrCode = null, allBranches = true)
            else ->
                organizationRepository.findTeamScheduleBranches(
                    hrCode = currentEmployee.costCenterCode,
                    allBranches = false
                )
        }
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
                val displaySchedules = daySchedules.filter { it.workingType == WorkingType.WORK && it.workingCategory1 != WorkingCategory1.EVENT }
                val promotionSchedules = daySchedules.filter { it.workingType == WorkingType.WORK && it.workingCategory1 == WorkingCategory1.EVENT }

                DailySummaryDto(
                    date = date?.toString() ?: "",
                    displayExpected = displaySchedules.size,
                    displayActual = displaySchedules.count { it.commuteLogSfid != null },
                    promotionExpected = promotionSchedules.size,
                    promotionActual = promotionSchedules.count { it.commuteLogSfid != null },
                    annualLeave = daySchedules.count { it.workingType == WorkingType.ANNUAL_LEAVE },
                    compensatoryLeave = daySchedules.count { it.workingType == WorkingType.ALT_HOLIDAY }
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

        if (request.workingType == WorkingType.WORK && request.accountId == null) {
            throw TeamScheduleAccountRequiredException()
        }

        val workingDate = LocalDate.parse(request.workingDate, DateTimeFormatter.ISO_LOCAL_DATE)

        if (request.workingType == WorkingType.WORK && request.workingCategory1 == WorkingCategory1.DISPLAY) {
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

        if (request.workingType == WorkingType.WORK && account != null) {
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

        if (request.workingType == WorkingType.WORK && request.accountId == null) {
            throw TeamScheduleAccountRequiredException()
        }

        val oldWorkingDate = schedule.workingDate
        val oldAccountId = schedule.account?.id
        val oldEmployeeId = schedule.employee?.id

        val newWorkingDate = LocalDate.parse(request.workingDate, DateTimeFormatter.ISO_LOCAL_DATE)
        val dateChanged = schedule.workingDate != newWorkingDate
        val category3Changed = schedule.workingCategory3 != request.workingCategory3

        if (dateChanged && schedule.workingDate?.isBefore(LocalDate.now()) == true && schedule.workingCategory1 != WorkingCategory1.EVENT) {
            throw TeamSchedulePastDateChangeException()
        }

        if (request.workingType == WorkingType.WORK && request.workingCategory1 == WorkingCategory1.DISPLAY && (dateChanged || category3Changed)) {
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
            if (request.workingType == WorkingType.WORK && newAccount != null) {
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
        if (currentEmployee.role == UserRole.BRANCH_MANAGER) {
            throw TeamScheduleDeleteForbiddenException()
        }

        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamScheduleNotFoundException() }

        if (currentEmployee.role != UserRole.SYSTEM_ADMIN && schedule.commuteLogSfid != null) {
            throw TeamScheduleWorkReportDeleteException()
        }

        teamScheduleValidator.validateDisplayMasterLink(currentEmployee, schedule)

        val employeeId = schedule.employee?.id
        val accountId = schedule.account?.id
        val workingDate = schedule.workingDate

        teamMemberScheduleRepository.delete(schedule)

        if (employeeId != null && accountId != null && workingDate != null && schedule.workingType == WorkingType.WORK) {
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
