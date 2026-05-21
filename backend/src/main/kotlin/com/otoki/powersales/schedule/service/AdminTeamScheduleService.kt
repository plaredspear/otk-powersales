package com.otoki.powersales.schedule.service

import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
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
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService,
    private val teamScheduleValidator: TeamScheduleValidator
) {

    /**
     * 여사원 일정관리 "여사원" 탭 목록.
     *
     * SF 레거시 `TeamMemberListController.fetchTeamMembers()` 정합 — branchCode 무관, 권한 기준 일괄 조회:
     * - 특수 사번 4명 (`19951029`/`20001013`/`20060052`/`20050308`) → CostCenterCode IN ('3233','3234','3235','3236','5691','5694')
     * - 그 외 → 본인 `cost_center_code` 단일
     *
     * 공통 SOQL 정합 필터: `role = WOMAN AND app_login_active = true AND is_deleted != true`, `ORDER BY name`.
     * 여사원 모드는 SF `TeamMemberListComponent.cmp:76-115` 의 else 블록 — 지점 드롭다운이 없고 권한 범위 여사원을
     * 즉시 일괄 표시하는 동작. 다중 지점 사용자(SYSTEM_ADMIN / 영업지원1·2팀)도 본인 costCenterCode 단일 조회.
     *
     * @param principal 인증된 web admin 사용자. employeeCode / costCenterCode snapshot 만 사용 —
     *                  Employee 엔티티 재조회 없이 데이터 스코프 분기를 수행.
     */
    fun getMembers(principal: WebUserPrincipal): List<TeamMemberDto> {
        val empCode = principal.employeeCode
        val ccCode = principal.costCenterCode

        val targetCostCenterCodes: List<String> = when {
            empCode in SF_SPECIAL_EMPLOYEE_CODES ->
                SF_SPECIAL_EMPLOYEE_COST_CENTERS
            ccCode.isNullOrBlank() -> return emptyList()
            else -> listOf(ccCode)
        }

        return employeeRepository.findActiveWomenByCostCenterCodes(targetCostCenterCodes)
            .map { TeamMemberDto.from(it) }
    }

    fun getAccounts(principal: WebUserPrincipal, branchCode: String?): List<TeamScheduleAccountDto> {
        val effectiveBranchCode = branchCode ?: run {
            principal.costCenterCode ?: return emptyList()
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
    fun getBranches(principal: WebUserPrincipal): List<BranchResponse> {
        val role = principal.role

        return when {
            role == UserRoleEnum.SYSTEM_ADMIN -> organizationRepository.findAllTeamScheduleBranches()
            role != null && UserRoleEnum.ALL_BRANCHES.contains(role) ->
                organizationRepository.findTeamScheduleBranches(hrCode = null, allBranches = true)
            else ->
                organizationRepository.findTeamScheduleBranches(
                    hrCode = principal.costCenterCode,
                    allBranches = false
                )
        }
    }

    /**
     * 여사원 일정관리 "전문행사조" 필터 옵션. team_member_schedule.professional_promotion_team distinct.
     * SF `ProfessionalPromotionTeamMaster__c.ProfessionalPromotionTeam__c` picklist 5값
     * (라면세일조 / 프레시세일조_냉동 / 프레시세일조_냉장 / 프레시세일조_만두 / 카레행사조) 기준이나,
     * 운영 적재 실데이터를 출처로 — 신규 조 picklist 확장 시 코드 변경 없이 자동 반영.
     */
    fun getProfessionalPromotionTeams(): List<String> {
        return teamMemberScheduleRepository.findDistinctProfessionalPromotionTeams()
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
        from: LocalDate,
        to: LocalDate,
        employeeIds: List<Long>?,
        accountIds: List<Int>?,
        promotionTeams: List<String>? = null
    ): MonthlyScheduleWithSummaryDto {
        if (from.isAfter(to)) throw TeamScheduleInvalidRangeException()
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw TeamScheduleRangeTooWideException()
        }

        val hasEmployeeFilter = !employeeIds.isNullOrEmpty()
        val hasAccountFilter = !accountIds.isNullOrEmpty()

        if (!hasEmployeeFilter && !hasAccountFilter) {
            return MonthlyScheduleWithSummaryDto(schedules = emptyList(), dailySummary = emptyList())
        }

        val effectivePromotionTeams = promotionTeams?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

        // SF XOR: 여사원 우선, 없으면 거래처. 두 IN 절을 동시에 합치지 않는다.
        // 전문행사조(`professional_promotion_team`) 가 지정되면 AND 로 추가 — 데이터 행에 메타 컬럼으로 적재된 조 분류 기준 정밀 필터.
        val rawSchedules: List<TeamMemberSchedule> = if (hasEmployeeFilter) {
            teamMemberScheduleRepository.findMonthlyByEmployeeIds(employeeIds!!, from, to, effectivePromotionTeams)
        } else {
            teamMemberScheduleRepository.findMonthlyByAccountIds(accountIds!!, from, to, effectivePromotionTeams)
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
    fun createSchedule(principal: WebUserPrincipal, request: TeamScheduleCreateRequest): TeamScheduleCreateResultDto {
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

        val teamLeader = employeeRepository.findById(principal.requireEmployeeId())
            .orElseThrow { TeamScheduleEmployeeNotFoundException() }

        val schedule = TeamMemberSchedule(
            employee = employee,
            workingDate = workingDate,
            workingType = request.workingType,
            workingCategory1 = request.workingCategory1,
            workingCategory2 = request.workingCategory2,
            workingCategory3 = request.workingCategory3,
            account = account,
            teamLeader = teamLeader
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
    fun updateSchedule(principal: WebUserPrincipal, scheduleId: Long, request: TeamScheduleUpdateRequest) {
        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamScheduleNotFoundException() }

        teamScheduleValidator.validateDisplayMasterLink(principal.role, schedule)

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
    fun deleteSchedule(principal: WebUserPrincipal, scheduleId: Long) {
        if (principal.role == UserRoleEnum.BRANCH_MANAGER) {
            throw TeamScheduleDeleteForbiddenException()
        }

        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamScheduleNotFoundException() }

        if (principal.role != UserRoleEnum.SYSTEM_ADMIN && schedule.commuteLogSfid != null) {
            throw TeamScheduleWorkReportDeleteException()
        }

        teamScheduleValidator.validateDisplayMasterLink(principal.role, schedule)

        val employeeId = schedule.employee?.id
        val accountId = schedule.account?.id
        val workingDate = schedule.workingDate

        teamMemberScheduleRepository.delete(schedule)

        if (employeeId != null && accountId != null && workingDate != null && schedule.workingType == WorkingType.WORK) {
            adminMonthlyIntegrationService.refreshIntegration(employeeId, accountId, YearMonth.from(workingDate))
        }
    }

    // --- Private helpers ---

    companion object {
        /** SF `TeamMemberListController.fetchTeamMembers` 특수 사번 하드코딩 분기 — 인천 cost center 광역 매핑 */
        private val SF_SPECIAL_EMPLOYEE_CODES = setOf("19951029", "20001013", "20060052", "20050308")

        /** SF `TeamMemberListController.fetchTeamMembers:18` 특수 사번 4명 매핑 cost center 6개 */
        private val SF_SPECIAL_EMPLOYEE_COST_CENTERS = listOf("3233", "3234", "3235", "3236", "5691", "5694")

        /** 기간 조회 상한 — 운영 부하 worst case 회피. ChronoUnit.DAYS.between(from, to) 가 이 값을 초과하면 거부 */
        private const val MAX_RANGE_DAYS = 91L
    }
}
