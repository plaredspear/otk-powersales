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

    /**
     * 여사원 일정관리 "여사원" 탭 목록.
     *
     * SF 레거시 `TeamMemberListController.fetchTeamMembers()` 정합 + 적응형 지점 선택:
     * - 특수 사번 4명 (`19951029`/`20001013`/`20060052`/`20050308`) → CostCenterCode IN ('3233','3234','3235','3236','5691')
     * - 영업지원1·2팀 (cost_center_code = `4888`/`4889`) → 다중 지점 → `branchCode` 미지정 시 빈 리스트, 지정 시 그 cost center 한정
     * - SYSTEM_ADMIN → 다중 지점 → `branchCode` 동일 규칙
     * - 일반 → 본인 `cost_center_code` 단일 (branchCode 무시)
     *
     * 공통 SOQL 정합 필터: `role = WOMAN AND app_login_active = true AND is_deleted != true`, `ORDER BY name`.
     * SF 의 `TeamMemberListComponentHelper.js:49-52` (지점 1개면 자동, N개면 사용자 선택) 패턴 정합.
     */
    fun getMembers(employeeId: Long, branchCode: String? = null): List<TeamMemberDto> {
        val currentEmployee = findEmployeeById(employeeId)
        val role = currentEmployee.role
        val empCode = currentEmployee.employeeCode
        val ccCode = currentEmployee.costCenterCode

        val targetCostCenterCodes: List<String>? = when {
            empCode in SF_SPECIAL_EMPLOYEE_CODES ->
                listOf("3233", "3234", "3235", "3236", "5691")
            ccCode in SF_FULL_SCOPE_COST_CENTERS || role == UserRole.SYSTEM_ADMIN ->
                // 다중 지점 케이스: 지점 선택 필수. 미지정 시 빈 리스트 (SF 적응형 패턴)
                if (branchCode.isNullOrBlank()) return emptyList() else listOf(branchCode)
            ccCode.isNullOrBlank() -> return emptyList()
            else -> listOf(ccCode)
        }

        return employeeRepository.findActiveWomenByCostCenterCodes(targetCostCenterCodes)
            .map { TeamMemberDto.from(it) }
    }

    fun getAccounts(employeeId: Long, branchCode: String?): List<TeamScheduleAccountDto> {
        val effectiveBranchCode = branchCode ?: run {
            val currentEmployee = findEmployeeById(employeeId)
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
    fun getBranches(employeeId: Long): List<BranchResponse> {
        val currentEmployee = findEmployeeById(employeeId)
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

    /**
     * 일정 조회.
     *
     * SF 레거시 `FullCalendarComponentController.fetchAllShcedule()` 정합 — **XOR 분기**:
     * - `employeeIds` 가 있으면 여사원 IN 절 단일 사용 (SF `cls:60`)
     * - 없고 `accountIds` 가 있으면 거래처 IN 절 단일 사용 (SF `cls:71`)
     * - 둘 다 없으면 빈 결과
     *
     * 두 IN 절을 합쳐 OR 처럼 동작하지 않는다 — 운영 부하 worst case (전사 IN×IN) 회피.
     *
     * 기간: `from` ~ `to` 임의 범위. 캘린더 월 뷰는 그달 1일~말일을, 목록 뷰의 RangePicker 는 사용자 지정 기간을 넘긴다.
     */
    fun getSchedulesWithSummary(
        employeeId: Long,
        from: LocalDate,
        to: LocalDate,
        employeeIds: List<Long>?,
        accountIds: List<Int>?
    ): MonthlyScheduleWithSummaryDto {
        val hasEmployeeFilter = !employeeIds.isNullOrEmpty()
        val hasAccountFilter = !accountIds.isNullOrEmpty()

        if (!hasEmployeeFilter && !hasAccountFilter) {
            return MonthlyScheduleWithSummaryDto(schedules = emptyList(), dailySummary = emptyList())
        }

        // SF XOR: 여사원 우선, 없으면 거래처. 두 IN 절을 동시에 합치지 않는다.
        val rawSchedules: List<TeamMemberSchedule> = if (hasEmployeeFilter) {
            teamMemberScheduleRepository.findMonthlyByEmployeeIds(employeeIds!!, from, to)
        } else {
            teamMemberScheduleRepository.findMonthlyByAccountIds(accountIds!!, from, to)
        }

        val uniqueSchedules = rawSchedules.distinctBy { it.id }

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
    fun createSchedule(employeeId: Long, request: TeamScheduleCreateRequest): TeamScheduleCreateResultDto {
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

        val currentEmployee = findEmployeeById(employeeId)
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
    fun updateSchedule(employeeId: Long, scheduleId: Long, request: TeamScheduleUpdateRequest) {
        val currentEmployee = findEmployeeById(employeeId)
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
    fun deleteSchedule(employeeId: Long, scheduleId: Long) {
        val currentEmployee = findEmployeeById(employeeId)
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

    /**
     * 현재 로그인 사용자의 Employee 조회.
     *
     * SF 정합: SF 는 매번 `User.DKRetail__EmployeeNumber__c` → `DKRetail__Employee__c.DKRetail__EmpCode__c` 로
     * 사번 매칭하지만, 신규는 인증 시점에 `principal.employeeId` 로 압축해 두므로 `Employee.id` 직접 lookup.
     */
    private fun findEmployeeById(employeeId: Long): Employee {
        return adminEmployeeHolder.employee
            ?: employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw TeamScheduleEmployeeNotFoundException()
    }

    companion object {
        /** SF `TeamMemberListController.fetchTeamMembers` 특수 사번 하드코딩 분기 — 인천 cost center 광역 매핑 */
        private val SF_SPECIAL_EMPLOYEE_CODES = setOf("19951029", "20001013", "20060052", "20050308")

        /** SF `TeamMemberListController.fetchManagedAccList` 영업지원1·2팀 — 전사 노출 cost center */
        private val SF_FULL_SCOPE_COST_CENTERS = setOf("4888", "4889")
    }
}
