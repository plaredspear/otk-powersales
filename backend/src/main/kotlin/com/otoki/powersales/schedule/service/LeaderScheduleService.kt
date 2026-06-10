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
import com.otoki.powersales.employee.enums.EmploymentStatus
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.LeaderScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.schedule.dto.response.LeaderDailyEmployeeItem
import com.otoki.powersales.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.schedule.dto.response.LeaderDailyStatusSummary
import com.otoki.powersales.schedule.dto.response.LeaderDailyWorkerItem
import com.otoki.powersales.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.schedule.dto.response.LeaderScheduleUpdateResponse
import com.otoki.powersales.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.LeaderScheduleAccountRequiredException
import com.otoki.powersales.schedule.exception.LeaderScheduleAlreadyAttendedException
import com.otoki.powersales.schedule.exception.LeaderScheduleDisplayMasterLinkedException
import com.otoki.powersales.schedule.exception.LeaderScheduleInvalidWorkCategory2Exception
import com.otoki.powersales.schedule.exception.LeaderScheduleInvalidWorkingTypeException
import com.otoki.powersales.schedule.exception.LeaderScheduleMissingWorkCategory3Exception
import com.otoki.powersales.schedule.exception.LeaderScheduleNotDisplayScheduleException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotFoundException
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
    private val scheduleConflictValidator: ScheduleConflictValidator,
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver
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
        // owner 는 대상 직원의 소속 조장 User (= 대리 등록 조장 registrant) — 레거시
        // TeamMemberScheduleTriggerHandler.insertOwner 동등.
        val ownerUser = teamMemberScheduleOwnerResolver.resolveOwner(targetEmployee)
        val newSchedule = TeamMemberSchedule(
            employee = targetEmployee,
            account = account,
            workingDate = workingDate,
            workingType = WORKING_TYPE_WORK,
            workingCategory1 = WorkingCategory1.fromDisplayNameOrNull(request.workingCategory1),
            workingCategory2 = WORKING_CATEGORY2_DEDICATED,
            workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(request.workingCategory3),
            proxyRegisteredBy = registrant.id,
            ownerUser = ownerUser
        )

        // step 11: 저장 + 응답
        val saved = teamMemberScheduleRepository.save(newSchedule)
        return LeaderScheduleCreateResponse.from(saved)
    }

    /**
     * 조장 진열 일정 수정 (P7 — 레거시 `changeProc` InterfaceType=M 동등, 거래처 변경만).
     *
     * 가드: 본인 팀원의 진열 일정 + 진열마스터 미연결 + 출근 미등록. 새 거래처는 본인 담당 거래처.
     * 충돌 검증은 자기 자신을 제외하고 기존 [ScheduleConflictValidator] 규칙을 재사용한다(근무유형3 불변).
     *
     * **신규 차이(MFEIS)**: 레거시는 트리거로 N/M/D 시 `MonthlyFemaleEmployeeIntegrationSchedule` 를
     * 재집계했으나, 신규는 #554 create 와 동일하게 batch 재집계 모델을 따라 호출하지 않는다.
     * 본 수정은 거래처만 바꾸고 월/직원/근무유형 집계 키는 불변이라 MFEIS 영향이 없다.
     */
    @Transactional
    fun updateTeamMemberSchedule(
        registrantId: Long,
        scheduleId: Long,
        request: LeaderScheduleUpdateRequest
    ): LeaderScheduleUpdateResponse {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { LeaderScheduleNotFoundException() }
        requireEditableDisplaySchedule(schedule, registrant)

        val accountId = request.accountId ?: throw LeaderScheduleAccountRequiredException()
        val account = accountRepository.findById(accountId)
            .orElseThrow { LeaderScheduleNotLeaderAccountException() }
        if (!isLeaderAccount(account, registrant)) {
            throw LeaderScheduleNotLeaderAccountException()
        }

        // 충돌 검증 — 근무유형3 은 변경하지 않으므로 기존 값 유지, 자기 자신 제외.
        val employeeId = schedule.employee?.id ?: throw LeaderScheduleNotTeamMemberException()
        val workingDate = schedule.workingDate ?: throw LeaderScheduleNotFoundException()
        scheduleConflictValidator.validateConflicts(
            employeeId = employeeId,
            workingDate = workingDate,
            workingType = WORKING_TYPE_WORK,
            accountId = account.id,
            workingCategory3 = schedule.workingCategory3,
            excludeScheduleId = schedule.id
        )

        // 영속 entity — dirty checking 으로 commit 시 자동 UPDATE (명시 save 불요).
        schedule.account = account
        return LeaderScheduleUpdateResponse.from(schedule)
    }

    /**
     * 조장 진열 일정 삭제 (P7 — 레거시 `changeProc` InterfaceType=D 동등).
     *
     * 가드: 본인 팀원의 진열 일정 + 진열마스터 미연결 + 출근 미등록.
     *
     * **신규 차이(MFEIS)**: 레거시 트리거의 `MonthlyFemaleEmployeeIntegrationSchedule` 재집계는
     * 신규 batch 모델로 대체 — 삭제분은 차기 batch 까지 MFEIS 집계에 지연 반영될 수 있다(허용).
     */
    @Transactional
    fun deleteTeamMemberSchedule(registrantId: Long, scheduleId: Long) {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { LeaderScheduleNotFoundException() }
        requireEditableDisplaySchedule(schedule, registrant)

        teamMemberScheduleRepository.delete(schedule)
    }

    /**
     * 진열 일정 수정/삭제 공통 가드.
     * - 본인 팀원(cost_center_code 일치) 일정
     * - 진열 일정(`WORK && cat1 != EVENT`) — 행사/연차는 불가 (행사는 admin promotion 도메인 소유)
     * - 진열마스터 미연결 (레거시 `checkDisplayMaster`)
     * - 출근 미등록 (레거시 `deleteblock`)
     */
    private fun requireEditableDisplaySchedule(schedule: TeamMemberSchedule, registrant: Employee) {
        val employee = schedule.employee ?: throw LeaderScheduleNotTeamMemberException()
        if (employee.costCenterCode != registrant.costCenterCode) {
            throw LeaderScheduleNotTeamMemberException()
        }
        val isDisplay = schedule.workingType == WorkingType.WORK &&
            schedule.workingCategory1 != WorkingCategory1.EVENT
        if (!isDisplay) throw LeaderScheduleNotDisplayScheduleException()
        if (schedule.displayWorkSchedule != null) throw LeaderScheduleDisplayMasterLinkedException()
        if (schedule.attendanceLog != null) throw LeaderScheduleAlreadyAttendedException()
    }

    fun getTeamMembers(registrantId: Long): List<LeaderTeamMemberListResponse> {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val costCenterCode = registrant.costCenterCode
            ?: return emptyList()

        // 레거시 employeeMapper.xml `empSearch` 동작 보존:
        // 여사원 식별 = 조장·지점장 제외(역필터), 퇴직자 제외(휴직은 포함),
        // 이름(한글) 가나다순 정렬.
        return employeeRepository
            .findByCostCenterCodeAndRoleNotIn(
                costCenterCode,
                listOf(AppAuthority.LEADER, AppAuthority.BRANCH_MANAGER)
            )
            .filter { it.status != EmploymentStatus.RESIGNED.code }
            .sortedBy { it.name }
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
            // 레거시 teamleaderAccList 정합: 거래처명 + 거래처코드(externalKey) LIKE 검색
            accounts.filter {
                (it.name?.lowercase()?.contains(needle) == true) ||
                    (it.externalKey?.lowercase()?.contains(needle) == true)
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
