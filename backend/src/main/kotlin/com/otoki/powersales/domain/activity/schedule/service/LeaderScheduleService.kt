package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmploymentStatus
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.domain.activity.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.service.PromotionSchedulesUpsertHelper
import com.otoki.powersales.domain.activity.schedule.dto.request.LeaderEventScheduleChangeRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderEventScheduleChangeResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderCalendarDay
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderMonthlyCalendarResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.exception.LeaderEventScheduleAttendedException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderEventScheduleClosedException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderEventScheduleNotEventException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderEventScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleAccountRequiredException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleInvalidWorkCategory2Exception
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleInvalidWorkingTypeException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleMissingWorkCategory3Exception
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotLeaderAccountException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotLeaderException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotTeamMemberException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeInactiveException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import kotlin.collections.contains

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
    private val teamMemberScheduleNameGenerator: TeamMemberScheduleNameGenerator,
    private val scheduleConflictValidator: ScheduleConflictValidator,
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionSchedulesUpsertHelper: PromotionSchedulesUpsertHelper,
    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper,
    private val teamDailyStatusCalculator: TeamDailyStatusCalculator,
    private val adminEmployeeCredentialService: AdminEmployeeCredentialService
) {

    companion object {
        private const val EMPLOYEE_STATUS_ON_LEAVE = "휴직"
        private const val EMPLOYEE_STATUS_RETIRED = "퇴직"
        private val WORKING_TYPE_WORK = WorkingType.WORK
        private val WORKING_CATEGORY2_DEDICATED = WorkingCategory2.DEDICATED
        private val WORKING_CATEGORY3_ALLOWED = setOf("고정", "격고", "순회")
        private val LEADER_ACCOUNT_GROUPS = listOf("1000", "1010")

        /** 레거시 PromotionEmployeeTriggerHandler.removeScheduleOnDelete 마감 삭제 차단 우회 사번. */
        private const val SPECIAL_BYPASS_EMPLOYEE_CODE = "00000009"
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
            name = teamMemberScheduleNameGenerator.next(),
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
     * 조장 행사 일정 변경 — 담당 여사원/투입일 재배정 (레거시 `scheduleChangePromo` updatePromtSchedule M 동등).
     *
     * 행사 일정은 `Promotion → PromotionEmployee → TeamMemberSchedule(1:1)` 파생 구조라 TMS 직접 수정이 아니라
     * **진실원본 PromotionEmployee 의 employee/scheduleDate 를 갱신 후 [PromotionSchedulesUpsertHelper.upsert]
     * 로 TMS 를 재파생**한다 (SF `PromotionToScheduleQuickActionController.upsertPromotionSchedules` 동등).
     *
     * 거래처/근무유형은 행사 마스터 파생이라 변경 대상이 아니다.
     * - 출근완료(`attendanceLog`) 일정은 변경 불가 (레거시 "등록하지 않은 항목만 수정").
     * - 행사조원 마감(`promoCloseByTm`) + 핵심필드 변경 시 조장(비admin)은 차단.
     * - 투입일 행사기간 범위/근무유형3 한도/중복/연차충돌 검증은 upsert 가 수행.
     */
    @Transactional
    fun changeEventAssignment(
        registrantId: Long,
        scheduleId: Long,
        request: LeaderEventScheduleChangeRequest
    ): LeaderEventScheduleChangeResponse {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val targetEmployeeId = request.targetEmployeeId
            ?: throw LeaderScheduleTargetEmployeeNotFoundException()
        val workingDate = request.workingDate
            ?: throw LeaderScheduleTargetEmployeeNotFoundException()

        val (schedule, pe) = resolveEventAssignment(scheduleId, registrant)

        // 출근완료 가드 (레거시 "등록하지 않은 항목만 수정")
        if (schedule.attendanceLog != null) throw LeaderEventScheduleAttendedException()

        // 신규 담당 여사원 검증 (조장 팀원)
        val targetEmployee = employeeRepository.findById(targetEmployeeId)
            .orElseThrow { LeaderScheduleTargetEmployeeNotFoundException() }
        if (targetEmployee.costCenterCode != registrant.costCenterCode) {
            throw LeaderScheduleNotTeamMemberException()
        }
        if (targetEmployee.status == EMPLOYEE_STATUS_ON_LEAVE || targetEmployee.status == EMPLOYEE_STATUS_RETIRED) {
            throw LeaderScheduleTargetEmployeeInactiveException()
        }

        // 마감 가드 (admin validateClosedEmployeeModification 동등 — 조장=비admin)
        val criticalChanged = pe.employeeId != targetEmployeeId || pe.scheduleDate != workingDate
        if (pe.teamMemberScheduleId != null && pe.promoCloseByTm && criticalChanged) {
            throw LeaderEventScheduleClosedException()
        }

        // 진실원본 PromotionEmployee 갱신 (다른 필드 보존 — pe.update() 전체초기화 호출 금지)
        pe.employeeId = targetEmployeeId
        pe.scheduleDate = workingDate
        promotionEmployeeRepository.save(pe)

        // TMS 재파생 (기존 TMS 가 있으면 in-place update, 없으면 신규 생성). 행사기간/한도/중복 검증 포함.
        val promotionId = pe.promotionId ?: throw PromotionNotFoundException()
        promotionSchedulesUpsertHelper.upsert(promotionId)

        // 재파생된 TMS 조회 후 응답
        val updated = teamMemberScheduleRepository.findById(pe.teamMemberScheduleId ?: scheduleId)
            .orElse(schedule)
        return LeaderEventScheduleChangeResponse.from(updated)
    }

    /**
     * 조장 행사 일정 삭제 — 행사 배정 해제 (레거시 `scheduleChangePromo` updatePromtSchedule D 동등).
     *
     * TMS cascade 삭제(MFEIS 일관) + PromotionEmployee 삭제 (admin `deleteEmployee` 동등).
     */
    @Transactional
    fun deleteEventAssignment(registrantId: Long, scheduleId: Long) {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val (schedule, pe) = resolveEventAssignment(scheduleId, registrant)

        // 출근완료 가드 (레거시 deleteblock — 비admin 삭제 차단)
        if (schedule.attendanceLog != null) throw LeaderEventScheduleAttendedException()

        // 마감 가드 (admin deleteEmployee 동등 — 사번 00000009 예외 유지)
        if (pe.teamMemberScheduleId != null && pe.promoCloseByTm &&
            pe.employee?.employeeCode != SPECIAL_BYPASS_EMPLOYEE_CODE
        ) {
            throw LeaderEventScheduleClosedException()
        }

        // TMS cascade 삭제 (조장=비admin) + PromotionEmployee 삭제
        teamMemberScheduleCascadeHelper.cascadeDeleteByIds(actorIsAdminGrade = false, listOf(schedule.id))
        promotionEmployeeRepository.delete(pe)
        promotionEmployeeRepository.flush()
    }

    /**
     * 행사 일정 변경/삭제 공통 — scheduleId 로 TMS 를 로드하고 행사·팀원·연관 검증 후 (TMS, PE) 반환.
     */
    private fun resolveEventAssignment(
        scheduleId: Long,
        registrant: Employee
    ): Pair<TeamMemberSchedule, PromotionEmployee> {
        val schedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { LeaderEventScheduleNotFoundException() }

        // 행사 근무 일정만 대상
        if (schedule.workingCategory1 != WorkingCategory1.EVENT) {
            throw LeaderEventScheduleNotEventException()
        }

        // 현재 담당 여사원이 조장 팀원인지 검증
        if (schedule.employee?.costCenterCode != registrant.costCenterCode) {
            throw LeaderScheduleNotTeamMemberException()
        }

        // 진실원본 PromotionEmployee 연관 필수
        val pe = schedule.promotionEmployee
            ?: throw LeaderEventScheduleNotFoundException()

        return schedule to pe
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
     * 조장 — 본인 팀원(여사원) 단말 초기화 (deviceUuid 회수 + 기존 기기 즉시 로그아웃).
     *
     * 레거시 SF `EmployeeUUIDReset` Quick Action(조장 레이아웃) 을 신규 모바일 조장 경로로 이관.
     * 레거시엔 없던 **본인 지점 소속 검증** 을 추가한다(costCenterCode 일치 + 조장/지점장 제외).
     * 실제 초기화/캐시·토큰 정리는 [AdminEmployeeCredentialService.resetDevice] 에 위임.
     */
    @Transactional
    fun resetTeamMemberDevice(registrantId: Long, targetEmployeeId: Long): ResetDeviceResponse {
        requireTeamMember(registrantId, targetEmployeeId)
        return adminEmployeeCredentialService.resetDevice(targetEmployeeId)
    }

    /**
     * 조장 — 본인 팀원(여사원) 비밀번호 임시 초기화 (임시비번 `pwrs1234!` + 강제 변경).
     *
     * 레거시 SF `EmployeePasswordReset` Quick Action(조장 레이아웃) 을 신규 모바일 조장 경로로 이관.
     * 본인 지점 소속 검증 후 [AdminEmployeeCredentialService.resetPassword] 에 위임.
     */
    @Transactional
    fun resetTeamMemberPassword(registrantId: Long, targetEmployeeId: Long): ResetPasswordResponse {
        requireTeamMember(registrantId, targetEmployeeId)
        return adminEmployeeCredentialService.resetPassword(targetEmployeeId)
    }

    /**
     * 초기화 대상이 조장 본인 팀원(여사원)인지 검증.
     *
     * getTeamMembers 명단 범위와 동일: 조장 costCenterCode 일치 + 조장/지점장 제외.
     * 타 지점/타 팀이면 [LeaderScheduleNotTeamMemberException].
     */
    private fun requireTeamMember(registrantId: Long, targetEmployeeId: Long): Employee {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val target = employeeRepository.findById(targetEmployeeId)
            .orElseThrow { LeaderScheduleTargetEmployeeNotFoundException() }

        val sameBranch = registrant.costCenterCode != null &&
            target.costCenterCode == registrant.costCenterCode
        val isFemaleStaff = target.role != AppAuthority.LEADER &&
            target.role != AppAuthority.BRANCH_MANAGER
        if (!sameBranch || !isFemaleStaff) {
            throw LeaderScheduleNotTeamMemberException()
        }
        return target
    }

    /**
     * 조장 여사원 일별 현황 조회 (레거시 `employee/mngDaily.jsp` + `EmployeeController.mgnDaily` 결과 동등).
     *
     * **레거시 결과 재현 규칙** (조회 전용):
     * - 대상 인원: 조장의 `cost_center_code` 소속 전체 인원(레거시 sfid 서브쿼리 정합, 권한 필터 없음).
     *   조회는 모두 내부 PK(`employee.id`/`account.id`) 기준 — sfid 미사용.
     * - 진열: `DisplayWorkSchedule` 마스터(`confirmed=true` + 기간 유효, `selectHomeScheduleDisplay` 정합)
     *   중 **안전점검 제출자(`SafetyCheckSubmission` 존재 = 레거시 comm_cnt>0)** 만 포함.
     *   거래처별 출근 = 동일 (여사원,거래처,일자) 진열 `team_member_schedule` 행의 `attendanceLog` 존재.
     * - 행사: `team_member_schedule` `WORK && cat1=EVENT` (`selectHomeSchedulePromote` 정합).
     * - 연차: `team_member_schedule` `ANNUAL_LEAVE` (레거시 `costcentercode` 키 불일치 버그를
     *   계승하지 않고 정상 표시 — 코스트센터 인원 × 해당 일자).
     * - 요약 총원/출근: 레거시 `dislength`/`promotelength` 와 동일 — 진열·행사 distinct 여사원 수
     *   (mergedList 여사원 단위 카운터). 근무자 목록 헤더 개수와 항상 일치(cat2/cat3 그룹 분할 아님).
     * - 정렬: 레거시 mergedList 버킷 순서 — 진열=출근완료→임시(미출근)→정규(미출근),
     *   행사=출근완료→미출근, 그 안은 이름·거래처명순(레거시 accList `order by name`).
     */
    fun getDailyStatus(registrantId: Long, date: LocalDate): LeaderDailyStatusResponse {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val costCenterCode = registrant.costCenterCode
            ?: return teamDailyStatusCalculator.emptyDailyStatus(date)

        // 코스트센터 전체 인원(내부 PK). 레거시는 sfid 서브쿼리지만 신규는 cost_center_code 직접.
        val teamEmployeeIds = employeeRepository
            .findByCostCenterCodeIn(listOf(costCenterCode))
            .mapNotNull { it.id.takeIf { v -> v != 0L } }

        // 집계/정렬/요약은 조장·대리출근 공유 계산 코어에 위임.
        return teamDailyStatusCalculator.computeDailyStatus(teamEmployeeIds, date)
    }

    /**
     * 조장 — 여사원 월간 일정 캘린더 (레거시 `employee/mgnSchedule.jsp` + `calSchedule` 동등).
     *
     * 레거시 캘린더는 일정 있는 날짜마다 `출근완료수 / 전체수`(sum/cnt)를 표시한다.
     * 일별 집계는 검증된 `getDailyStatus` 와 동일한 `computeDailyWorkers` 를 월 전체에 반복 적용한다.
     * - [employeeId] null → "여사원 전체" 모드(조 전체 인원 합산).
     * - [employeeId] 지정 → 해당 조원 단독 집계. 조장 본인 팀원(`cost_center_code` 일치) 인지 검증.
     *
     * 카운트: total = (진열[안전점검 제출] + 행사) 건수, attended = 그 중 출근완료(commutelog 존재) 건수.
     * total=0 인 날짜는 응답에서 제외(레거시 cnt>0 만 표시).
     */
    fun getMonthlyCalendar(
        registrantId: Long,
        employeeId: Long?,
        year: Int,
        month: Int
    ): LeaderMonthlyCalendarResponse {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val costCenterCode = registrant.costCenterCode
            ?: return LeaderMonthlyCalendarResponse(year, month, emptyList())

        val teamEmployeeIds = employeeRepository
            .findByCostCenterCodeIn(listOf(costCenterCode))
            .mapNotNull { it.id.takeIf { v -> v != 0L } }

        // 개인 모드: 대상이 조장 본인 팀원인지 검증.
        if (employeeId != null && employeeId !in teamEmployeeIds) {
            throw LeaderScheduleNotTeamMemberException()
        }
        // 집계 대상: 개인 모드면 해당 조원만, 전체 모드면 조 전체.
        val targetIds = if (employeeId != null) listOf(employeeId) else teamEmployeeIds
        if (targetIds.isEmpty()) {
            return LeaderMonthlyCalendarResponse(year, month, emptyList())
        }

        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val days = mutableListOf<LeaderCalendarDay>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            teamDailyStatusCalculator.computeCalendarDay(targetIds, currentDate)?.let { days.add(it) }
            currentDate = currentDate.plusDays(1)
        }

        return LeaderMonthlyCalendarResponse(year, month, days)
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
}
