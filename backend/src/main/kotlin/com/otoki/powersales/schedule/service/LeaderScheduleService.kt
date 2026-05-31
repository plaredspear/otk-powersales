package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.schedule.dto.response.LeaderDailyEmployeeItem
import com.otoki.powersales.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.schedule.dto.response.LeaderDailyStatusSummary
import com.otoki.powersales.schedule.dto.response.LeaderDailyWorkerItem
import com.otoki.powersales.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.LeaderScheduleAccountRequiredException
import com.otoki.powersales.schedule.exception.LeaderScheduleInvalidWorkCategory2Exception
import com.otoki.powersales.schedule.exception.LeaderScheduleInvalidWorkingTypeException
import com.otoki.powersales.schedule.exception.LeaderScheduleMissingWorkCategory3Exception
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderAccountException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotTeamMemberException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeInactiveException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 조장 대리 일정 등록 서비스 (Spec #554 P1-B).
 *
 * - 조장이 본인 팀원의 근무 일정을 단건 대리 등록 (POST)
 * - 본인 팀원 목록 조회 (GET)
 * - 본인 거래처 목록 조회 (GET)
 *
 * 거래처 매핑은 레거시 `accountMapper.xml:239-256` (`teamleaderAccList`) 의 간접 매핑 그대로:
 * `account.branch_code = employee.cost_center_code` AND `account.account_group IN ('1000','1010')`.
 */
@Service
@Transactional(readOnly = true)
class LeaderScheduleService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val scheduleConflictValidator: ScheduleConflictValidator
) {

    companion object {
        private const val EMPLOYEE_STATUS_ON_LEAVE = "휴직"
        private const val EMPLOYEE_STATUS_RETIRED = "퇴직"
        private val WORKING_TYPE_WORK = WorkingType.WORK
        private val WORKING_CATEGORY2_DEDICATED = WorkingCategory2.DEDICATED
        private val WORKING_CATEGORY3_ALLOWED = setOf("고정", "격고", "순회")
        private val LEADER_ACCOUNT_GROUPS = listOf("1000", "1010")

        /** 일별 현황 정렬 — 출근 완료자 우선(레거시 mngDaily 병합 순서 동등), 그 다음 이름순. */
        private val WORKER_ORDER: Comparator<LeaderDailyWorkerItem> =
            compareByDescending<LeaderDailyWorkerItem> { it.attended }.thenBy { it.employeeName }
    }

    @Transactional
    fun createTeamMemberSchedule(
        registrantId: Long,
        request: LeaderScheduleCreateRequest
    ): LeaderScheduleCreateResponse {
        // step 1: 등록자 매핑
        val registrant = findRegistrant(registrantId)

        // step 2: 조장 권한 검증
        requireLeader(registrant)

        // step 3: 요청 필드 정합성 검증
        validateRequestFields(request)

        // step 4: 대상 직원 조회
        val targetEmployeeId = request.targetEmployeeId
            ?: throw LeaderScheduleTargetEmployeeNotFoundException()
        val targetEmployee = employeeRepository.findById(targetEmployeeId)
            .orElseThrow { LeaderScheduleTargetEmployeeNotFoundException() }

        // step 5: 팀원 검증
        if (targetEmployee.costCenterCode != registrant.costCenterCode) {
            throw LeaderScheduleNotTeamMemberException()
        }

        // step 6: 대상 직원 상태 검증
        if (targetEmployee.status == EMPLOYEE_STATUS_ON_LEAVE || targetEmployee.status == EMPLOYEE_STATUS_RETIRED) {
            throw LeaderScheduleTargetEmployeeInactiveException()
        }

        // step 7: 거래처 검증
        val accountId = request.accountId ?: throw LeaderScheduleAccountRequiredException()
        val account = accountRepository.findById(accountId)
            .orElseThrow { LeaderScheduleNotLeaderAccountException() }
        if (!isLeaderAccount(account, registrant)) {
            throw LeaderScheduleNotLeaderAccountException()
        }

        // step 8 & 9: 충돌 규칙 검증 (validator 가 기존 일정 조회 + 7개 규칙 평가)
        val workingDate = LocalDate.parse(request.workingDate)
        scheduleConflictValidator.validateConflicts(
            employeeId = targetEmployee.id,
            workingDate = workingDate,
            workingType = WORKING_TYPE_WORK,
            accountId = account.id,
            workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(request.workingCategory3)
        )

        // step 10: 신규 엔티티 생성
        val newSchedule = TeamMemberSchedule(
            employee = targetEmployee,
            account = account,
            workingDate = workingDate,
            workingType = WORKING_TYPE_WORK,
            workingCategory1 = WorkingCategory1.fromDisplayNameOrNull(request.workingCategory1),
            workingCategory2 = WORKING_CATEGORY2_DEDICATED,
            workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(request.workingCategory3),
            proxyRegisteredBy = registrant.id
        )

        // step 11: 저장 + 응답
        val saved = teamMemberScheduleRepository.save(newSchedule)
        return LeaderScheduleCreateResponse.from(saved)
    }

    fun getTeamMembers(registrantId: Long): List<LeaderTeamMemberListResponse> {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val costCenterCode = registrant.costCenterCode
            ?: return emptyList()

        return employeeRepository
            .findByCostCenterCodeAndRole(costCenterCode, AppAuthority.WOMAN)
            .sortedBy { it.employeeCode }
            .map { LeaderTeamMemberListResponse.from(it) }
    }

    /**
     * 조장 여사원 일별 현황 조회 (레거시 `employee/mngDaily.jsp` — 조회 전용).
     *
     * **레거시 매핑**: SF/JSP `employee/mngDaily.jsp` + `EmployeeController.mgnDaily`.
     * 조장이 특정 날짜의 팀 여사원 진열/행사/연차 근무 현황 + 거래처별 출근 등록 현황을 조회.
     * 레거시의 "일정변경"(mutation) 은 본 작업 범위 외 — P7 / spec #679 로 분리.
     *
     * **동작**: 본인 팀(cost_center_code) 여사원의 [date] 일정을 fetchJoin 단건 조회 후
     * 진열/행사 근무자 + 연차자로 분류하고 출근 등록 여부(attendance_log FK)를 함께 반환.
     *
     * **분류 정합**: 진열/행사/연차 분류·출근 판정은 [FemaleEmployeeScheduleQueryService.aggregateSummary]
     * 와 동일 기준 — 진열=`WORK && cat1 != EVENT`(cat1=null 포함), 행사=`WORK && cat1 == EVENT`,
     * 연차=`ANNUAL_LEAVE`, 출근=`attendanceLog != null`.
     */
    fun getDailyStatus(registrantId: Long, date: LocalDate): LeaderDailyStatusResponse {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val costCenterCode = registrant.costCenterCode
            ?: return emptyDailyStatus(date)

        val teamMembers = employeeRepository.findByCostCenterCodeAndRole(costCenterCode, AppAuthority.WOMAN)
        if (teamMembers.isEmpty()) return emptyDailyStatus(date)

        val employeeIds = teamMembers.mapNotNull { it.id.takeIf { v -> v != 0L } }
        val schedules = teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, employeeIds)

        // 진열/행사/연차 분류 — aggregateSummary 정합 (진열은 cat1 != EVENT 로 null 포함).
        val displaySchedules = schedules.filter {
            it.workingType == WorkingType.WORK && it.workingCategory1 != WorkingCategory1.EVENT
        }
        val eventSchedules = schedules.filter {
            it.workingType == WorkingType.WORK && it.workingCategory1 == WorkingCategory1.EVENT
        }
        val annualLeaveSchedules = schedules.filter { it.workingType == WorkingType.ANNUAL_LEAVE }

        val displayWorkers = displaySchedules.map { it.toWorkerItem() }.sortedWith(WORKER_ORDER)
        val eventWorkers = eventSchedules.map { it.toWorkerItem() }.sortedWith(WORKER_ORDER)
        val annualLeaveWorkers = annualLeaveSchedules
            .mapNotNull { it.employee }
            .distinctBy { it.id }
            .sortedBy { it.name }
            .map { LeaderDailyEmployeeItem(employeeId = it.id, employeeName = it.name, employeeCode = it.employeeCode) }

        val summary = LeaderDailyStatusSummary(
            displayTotal = displaySchedules.size,
            displayAttended = displaySchedules.count { it.attendanceLog != null },
            eventTotal = eventSchedules.size,
            eventAttended = eventSchedules.count { it.attendanceLog != null },
            annualLeaveCount = annualLeaveWorkers.size,
        )

        return LeaderDailyStatusResponse(
            date = date.toString(),
            summary = summary,
            displayWorkers = displayWorkers,
            eventWorkers = eventWorkers,
            annualLeaveWorkers = annualLeaveWorkers,
        )
    }

    fun getAccounts(registrantId: Long, keyword: String?): List<LeaderAccountListResponse> {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val branchCode = registrant.costCenterCode
            ?: return emptyList()

        val accounts = accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
            branchCode = branchCode,
            accountGroups = LEADER_ACCOUNT_GROUPS,
            isDeleted = true
        )

        val trimmedKeyword = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val filtered = if (trimmedKeyword == null) {
            accounts
        } else {
            val needle = trimmedKeyword.lowercase()
            accounts.filter {
                (it.name?.lowercase()?.contains(needle) == true) ||
                    (it.address1?.lowercase()?.contains(needle) == true)
            }
        }

        return filtered
            .sortedBy { it.name ?: "" }
            .map { LeaderAccountListResponse.from(it) }
    }

    private fun findRegistrant(registrantId: Long): Employee =
        employeeRepository.findById(registrantId)
            .orElseThrow { EmployeeNotFoundException() }

    private fun requireLeader(employee: Employee) {
        if (employee.role != AppAuthority.LEADER) {
            throw LeaderScheduleNotLeaderException()
        }
    }

    private fun validateRequestFields(request: LeaderScheduleCreateRequest) {
        if (request.workingType != WORKING_TYPE_WORK.displayName) {
            throw LeaderScheduleInvalidWorkingTypeException()
        }
        if (request.workingCategory2 != WORKING_CATEGORY2_DEDICATED.displayName) {
            throw LeaderScheduleInvalidWorkCategory2Exception()
        }
        if (request.workingCategory3 !in WORKING_CATEGORY3_ALLOWED) {
            throw LeaderScheduleMissingWorkCategory3Exception()
        }
        if (request.accountId == null) {
            throw LeaderScheduleAccountRequiredException()
        }
    }

    private fun isLeaderAccount(account: Account, registrant: Employee): Boolean {
        if (account.branchCode != registrant.costCenterCode) return false
        if (account.accountGroup !in LEADER_ACCOUNT_GROUPS) return false
        return true
    }

    private fun emptyDailyStatus(date: LocalDate): LeaderDailyStatusResponse =
        LeaderDailyStatusResponse(
            date = date.toString(),
            summary = LeaderDailyStatusSummary(0, 0, 0, 0, 0),
            displayWorkers = emptyList(),
            eventWorkers = emptyList(),
            annualLeaveWorkers = emptyList(),
        )

    private fun TeamMemberSchedule.toWorkerItem(): LeaderDailyWorkerItem =
        LeaderDailyWorkerItem(
            scheduleId = id,
            employeeId = employee?.id,
            employeeName = employee?.name.orEmpty(),
            employeeCode = employee?.employeeCode.orEmpty(),
            accountName = account?.name.orEmpty(),
            accountCode = account?.externalKey.orEmpty(),
            workingCategory1 = workingCategory1?.displayName,
            workingCategory2 = workingCategory2?.displayName,
            workingCategory3 = workingCategory3?.displayName,
            attended = attendanceLog != null,
        )
}
