package com.otoki.powersales.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.dto.request.LeaderDisplayScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.LeaderDisplayScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.LeaderDisplayScheduleResponse
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.enums.SecondWorkType
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.schedule.exception.LeaderDisplayScheduleNotFoundException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderAccountException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotTeamMemberException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeInactiveException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.schedule.exception.ScheduleValidationException
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.schedule.service.internal.LastMonthRevenueLookup
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 조장 진열 일정(마스터) 변경 서비스 — 레거시 `scheduleChange.jsp`(진열) 동등.
 *
 * **설계 토대**: 진열 조회는 레거시·신규 모두 **기간 마스터(`DisplayWorkSchedule`)** 기반이라, 진열의
 * 거래처/근무유형 변경·추가·삭제는 **마스터를 편집**해야 조회 화면에 반영된다 (TMS 직접 편집은 반영 안 됨).
 *
 * **권한**: 레거시는 권한 보유 계정만 진열 변경 화면 진입 → 신규에서도 **조장을 authorized 로 간주**하여
 * admin 의 확정편집 가드(`EditDisableForDisplayMaster`)·연결TMS 삭제 가드(`deleteCheck`)를 적용하지 않는다.
 * 단 팀(`cost_center_code`) 스코프와 거래처 소속은 검증한다.
 *
 * 검증/자동채움은 admin 단건 CRUD([AdminScheduleService.createSchedule]/`updateSchedule`/`deleteSchedule`)가
 * 쓰는 하위 조각([ScheduleUploadValidator.validateSingle], [LastMonthRevenueLookup], owner resolve)을 재사용한다.
 */
@Service
@Transactional(readOnly = true)
class LeaderDisplayScheduleService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val uploadValidator: ScheduleUploadValidator,
    private val lastMonthRevenueLookup: LastMonthRevenueLookup,
    private val userRepository: UserRepository,
) {

    companion object {
        private const val EMPLOYEE_STATUS_ON_LEAVE = "휴직"
        private const val EMPLOYEE_STATUS_RETIRED = "퇴직"
        private val LEADER_ACCOUNT_GROUPS = listOf("1000", "1010")
    }

    fun getDisplaySchedule(registrantId: Long, displayWorkScheduleId: Long): LeaderDisplayScheduleResponse {
        val registrant = findLeader(registrantId)
        val schedule = findOwnedSchedule(displayWorkScheduleId, registrant)
        return LeaderDisplayScheduleResponse.from(schedule)
    }

    @Transactional
    fun createDisplaySchedule(
        registrantId: Long,
        request: LeaderDisplayScheduleCreateRequest
    ): LeaderDisplayScheduleResponse {
        val registrant = findLeader(registrantId)

        val targetEmployeeId = request.targetEmployeeId
            ?: throw LeaderScheduleTargetEmployeeNotFoundException()
        val targetEmployee = employeeRepository.findById(targetEmployeeId)
            .orElseThrow { LeaderScheduleTargetEmployeeNotFoundException() }
        if (targetEmployee.costCenterCode != registrant.costCenterCode) {
            throw LeaderScheduleNotTeamMemberException()
        }
        if (targetEmployee.status == EMPLOYEE_STATUS_ON_LEAVE || targetEmployee.status == EMPLOYEE_STATUS_RETIRED) {
            throw LeaderScheduleTargetEmployeeInactiveException()
        }

        val account = resolveLeaderAccount(request.accountId, registrant)

        val validatedRow = validate(
            employee = targetEmployee,
            account = account,
            typeOfWork3 = request.typeOfWork3.orEmpty(),
            typeOfWork4 = request.typeOfWork4.orEmpty(),
            typeOfWork5 = request.typeOfWork5.orEmpty(),
            startDate = request.startDate!!,
            endDate = request.endDate,
            excludeScheduleId = null,
        )

        val entity = DisplayWorkSchedule(
            employee = targetEmployee,
            account = account,
            typeOfWork1 = TypeOfWork1.DISPLAY,
            typeOfWork3 = TypeOfWork3.fromDisplayNameOrNull(validatedRow.typeOfWork3),
            typeOfWork4 = SecondWorkType.fromDisplayNameOrNull(validatedRow.typeOfWork4),
            typeOfWork5 = TypeOfWork5.fromDisplayNameOrNull(validatedRow.typeOfWork5),
            startDate = validatedRow.startDate,
            endDate = validatedRow.endDate,
            // 조장 authorized — 즉시 daily-status 노출 위해 확정 상태로 생성.
            confirmed = true,
            costCenterCode = validatedRow.costCenterCode,
            lastMonthRevenue = lastMonthRevenueLookup.forAccount(account),
            ownerUser = resolveOwnerUser(validatedRow.costCenterCode),
        )

        val saved = displayWorkScheduleRepository.save(entity)
        return LeaderDisplayScheduleResponse.from(saved)
    }

    @Transactional
    fun updateDisplaySchedule(
        registrantId: Long,
        displayWorkScheduleId: Long,
        request: LeaderDisplayScheduleUpdateRequest
    ): LeaderDisplayScheduleResponse {
        val registrant = findLeader(registrantId)
        val schedule = findOwnedSchedule(displayWorkScheduleId, registrant)

        // 진열 변경은 담당 여사원을 바꾸지 않는다 (레거시 scheduleChange 진열 정합).
        val targetEmployee = schedule.employee
            ?: throw LeaderDisplayScheduleNotFoundException()
        val account = resolveLeaderAccount(request.accountId, registrant)

        // 확정편집 가드 미적용 (조장 authorized). 정합 검증만 수행 (자기 자신 중복 제외).
        val validatedRow = validate(
            employee = targetEmployee,
            account = account,
            typeOfWork3 = request.typeOfWork3.orEmpty(),
            typeOfWork4 = request.typeOfWork4.orEmpty(),
            typeOfWork5 = request.typeOfWork5.orEmpty(),
            startDate = request.startDate!!,
            endDate = request.endDate,
            excludeScheduleId = displayWorkScheduleId,
        )

        schedule.account = account
        schedule.typeOfWork1 = TypeOfWork1.DISPLAY
        schedule.typeOfWork3 = TypeOfWork3.fromDisplayNameOrNull(validatedRow.typeOfWork3)
        schedule.typeOfWork4 = SecondWorkType.fromDisplayNameOrNull(validatedRow.typeOfWork4)
        schedule.typeOfWork5 = TypeOfWork5.fromDisplayNameOrNull(validatedRow.typeOfWork5)
        schedule.startDate = validatedRow.startDate
        schedule.endDate = validatedRow.endDate
        schedule.costCenterCode = validatedRow.costCenterCode
        schedule.lastMonthRevenue = lastMonthRevenueLookup.forAccount(account)

        return LeaderDisplayScheduleResponse.from(schedule)
    }

    @Transactional
    fun deleteDisplaySchedule(registrantId: Long, displayWorkScheduleId: Long) {
        val registrant = findLeader(registrantId)
        val schedule = findOwnedSchedule(displayWorkScheduleId, registrant)

        // 연결된 진열 TMS 의 FK SetNull (SF deleteConstraint=SetNull 동등) — 출근으로 생성된 TMS 보존.
        // 연결TMS 삭제 가드는 조장 authorized 라 미적용.
        val linked = teamMemberScheduleRepository.findByDisplayWorkSchedule(schedule)
        if (linked.isNotEmpty()) {
            linked.forEach { it.displayWorkSchedule = null }
            teamMemberScheduleRepository.saveAll(linked)
        }

        schedule.isDeleted = true
    }

    // --- private helpers ---

    private fun findLeader(registrantId: Long): Employee {
        val registrant = employeeRepository.findById(registrantId)
            .orElseThrow { EmployeeNotFoundException() }
        if (registrant.role != AppAuthority.LEADER) {
            throw LeaderScheduleNotLeaderException()
        }
        return registrant
    }

    /** 마스터 로드 + 조장 팀(cost_center_code) 스코프 검증. */
    private fun findOwnedSchedule(displayWorkScheduleId: Long, registrant: Employee): DisplayWorkSchedule {
        val schedule = displayWorkScheduleRepository.findById(displayWorkScheduleId)
            .filter { it.isDeleted != true }
            .orElseThrow { LeaderDisplayScheduleNotFoundException() }
        val ownerCostCenter = schedule.costCenterCode ?: schedule.employee?.costCenterCode
        if (ownerCostCenter != registrant.costCenterCode) {
            throw LeaderScheduleNotTeamMemberException()
        }
        return schedule
    }

    private fun resolveLeaderAccount(accountId: Long?, registrant: Employee): Account {
        val id = accountId ?: throw LeaderScheduleNotLeaderAccountException()
        val account = accountRepository.findById(id)
            .orElseThrow { LeaderScheduleNotLeaderAccountException() }
        if (account.branchCode != registrant.costCenterCode || account.accountGroup !in LEADER_ACCOUNT_GROUPS) {
            throw LeaderScheduleNotLeaderAccountException()
        }
        return account
    }

    /** admin 단건 CRUD 와 동일한 검증기 재사용 — 기간/중복/근무유형 (레거시 parity). */
    private fun validate(
        employee: Employee,
        account: Account,
        typeOfWork3: String,
        typeOfWork4: String,
        typeOfWork5: String,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate?,
        excludeScheduleId: Long?,
    ): ScheduleUploadValidator.ValidatedRow {
        val existing = displayWorkScheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(employee.id))
        val result = uploadValidator.validateSingle(
            employeeCode = employee.employeeCode.orEmpty(),
            accountCode = account.externalKey.orEmpty(),
            typeOfWork3 = typeOfWork3,
            typeOfWork4 = typeOfWork4,
            typeOfWork5 = typeOfWork5,
            startDate = startDate,
            endDate = endDate,
            employee = employee,
            account = account,
            existingSchedules = existing,
            excludeScheduleId = excludeScheduleId,
        )
        return result.validatedRow
            ?: throw ScheduleValidationException(result.messages.joinToString("; "))
    }

    /** 자동채움: 소속 조장 User (admin createSchedule 동등). */
    private fun resolveOwnerUser(costCenterCode: String?): User? {
        if (costCenterCode.isNullOrBlank()) return null
        return employeeRepository
            .findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf(costCenterCode), AppAuthority.LEADER)
            .firstOrNull()
            ?.employeeCode
            ?.let { code -> userRepository.findByEmployeeCodeIn(listOf(code)).firstOrNull() }
    }
}
