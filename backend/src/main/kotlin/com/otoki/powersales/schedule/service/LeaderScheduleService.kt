package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.schedule.dto.response.LeaderAccountListResponse
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
            .findByCostCenterCodeAndRole(costCenterCode, UserRole.WOMAN)
            .sortedBy { it.employeeCode }
            .map { LeaderTeamMemberListResponse.from(it) }
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
        if (employee.role != UserRole.LEADER) {
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
}
