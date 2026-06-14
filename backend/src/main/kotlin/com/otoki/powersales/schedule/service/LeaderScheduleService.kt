package com.otoki.powersales.schedule.service

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmploymentStatus
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.service.PromotionSchedulesUpsertHelper
import com.otoki.powersales.schedule.dto.request.LeaderEventScheduleChangeRequest
import com.otoki.powersales.schedule.dto.request.LeaderProxyAttendanceRequest
import com.otoki.powersales.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.schedule.dto.response.LeaderEventScheduleChangeResponse
import com.otoki.powersales.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.schedule.dto.response.LeaderDailyEmployeeItem
import com.otoki.powersales.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.schedule.dto.response.LeaderDailyStatusSummary
import com.otoki.powersales.schedule.dto.response.LeaderCalendarDay
import com.otoki.powersales.schedule.dto.response.LeaderDailyWorkerItem
import com.otoki.powersales.schedule.dto.response.LeaderMonthlyCalendarResponse
import com.otoki.powersales.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.schedule.exception.LeaderScheduleAccountRequiredException
import com.otoki.powersales.schedule.exception.LeaderScheduleInvalidWorkCategory2Exception
import com.otoki.powersales.schedule.exception.LeaderScheduleInvalidWorkingTypeException
import com.otoki.powersales.schedule.exception.LeaderScheduleMissingWorkCategory3Exception
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderAccountException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotTeamMemberException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeInactiveException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.schedule.exception.LeaderEventScheduleAttendedException
import com.otoki.powersales.schedule.exception.LeaderEventScheduleClosedException
import com.otoki.powersales.schedule.exception.LeaderEventScheduleNotEventException
import com.otoki.powersales.schedule.exception.LeaderEventScheduleNotFoundException
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

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
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository,
    private val scheduleConflictValidator: ScheduleConflictValidator,
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver,
    private val attendanceService: AttendanceService,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionSchedulesUpsertHelper: PromotionSchedulesUpsertHelper,
    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper
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

        /** 진열 cat2 '임시' 표시값 (레거시 tempList `workingcategory2__c.contains("임시")` 정합). */
        private val DISPLAY_CATEGORY2_TEMPORARY = TypeOfWork5.TEMPORARY.displayName
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
     * 조장 대리출근 등록 (레거시 mngDaily `addScheduleProc` 동등).
     *
     * 조장 권한 + 대상이 조장 팀원(cost_center_code 일치) 검증 후, 실제 출근 등록은
     * [AttendanceService.registerProxy] 에 위임 (Orora WorkReport 경로 공유, GPS 스킵).
     */
    @Transactional
    fun registerProxyAttendance(
        registrantId: Long,
        request: LeaderProxyAttendanceRequest
    ): AttendanceRegisterResponse {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val targetEmployeeId = request.targetEmployeeId
            ?: throw LeaderScheduleTargetEmployeeNotFoundException()
        val targetEmployee = employeeRepository.findById(targetEmployeeId)
            .orElseThrow { LeaderScheduleTargetEmployeeNotFoundException() }

        // 팀원 검증 (조장 코스트센터 일치)
        if (targetEmployee.costCenterCode != registrant.costCenterCode) {
            throw LeaderScheduleNotTeamMemberException()
        }

        return attendanceService.registerProxy(
            targetEmployee = targetEmployee,
            scheduleId = request.scheduleId,
            displayWorkScheduleId = request.displayWorkScheduleId
        )
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
     * - 요약 총원/출근: 레거시 `dislength`=(여사원,cat2) 그룹 수, `promotelength`=(여사원,cat2,cat3) 그룹 수.
     * - 정렬: 레거시 mergedList 버킷 순서 — 진열=출근완료→임시(미출근)→정규(미출근),
     *   행사=출근완료→미출근, 그 안은 이름·거래처명순(레거시 accList `order by name`).
     */
    fun getDailyStatus(registrantId: Long, date: LocalDate): LeaderDailyStatusResponse {
        val registrant = findRegistrant(registrantId)
        requireLeader(registrant)

        val costCenterCode = registrant.costCenterCode
            ?: return emptyDailyStatus(date)

        // 코스트센터 전체 인원(내부 PK). 레거시는 sfid 서브쿼리지만 신규는 cost_center_code 직접.
        val teamEmployeeIds = employeeRepository
            .findByCostCenterCodeIn(listOf(costCenterCode))
            .mapNotNull { it.id.takeIf { v -> v != 0L } }
        if (teamEmployeeIds.isEmpty()) return emptyDailyStatus(date)

        // 일별 진열/행사 워커 집계 (월간 캘린더와 공유하는 핵심 로직).
        val workers = computeDailyWorkers(teamEmployeeIds, date)
        val schedules = workers.schedules
        val rawDisplay = workers.display
        val rawEvent = workers.event

        // ── 정렬 (레거시 mergedList 버킷 순서, 여사원 단위) ──
        // 진열: 출근완료 → 임시(미출근) → 정규(미출근), 그 안은 이름·거래처명순.
        val displayAttendedEmps = rawDisplay.filter { it.attended }.mapNotNull { it.employeeId }.toSet()
        val displayTempEmps = rawDisplay
            .filter {
                it.employeeId != null && it.employeeId !in displayAttendedEmps &&
                    it.workingCategory2 == DISPLAY_CATEGORY2_TEMPORARY
            }
            .mapNotNull { it.employeeId }
            .toSet()
        val displayWorkers = rawDisplay.sortedWith(
            compareBy(
                { w -> when {
                    w.employeeId in displayAttendedEmps -> 0
                    w.employeeId in displayTempEmps -> 1
                    else -> 2
                } },
                { it.employeeName },
                { it.accountName },
            )
        )
        // 행사: 출근완료 → 미출근, 그 안은 이름·거래처명순.
        val eventAttendedEmps = rawEvent.filter { it.attended }.mapNotNull { it.employeeId }.toSet()
        val eventWorkers = rawEvent.sortedWith(
            compareBy(
                { w -> if (w.employeeId in eventAttendedEmps) 0 else 1 },
                { it.employeeName },
                { it.accountName },
            )
        )

        // ── 연차: team_member_schedule ANNUAL_LEAVE (레거시 버그 수정 — 정상 표시) ──
        val annualLeaveWorkers = schedules
            .filter { it.workingType == WorkingType.ANNUAL_LEAVE }
            .mapNotNull { it.employee }
            .distinctBy { it.id }
            .sortedBy { it.name }
            .map { LeaderDailyEmployeeItem(employeeId = it.id, employeeName = it.name, employeeCode = it.employeeCode) }

        // ── 요약: 레거시 dislength/promotelength 와 동일 그룹 단위 ──
        // dislength = (여사원, cat2) 그룹 수 / promotelength = (여사원, cat2, cat3) 그룹 수.
        val summary = LeaderDailyStatusSummary(
            displayTotal = rawDisplay.map { it.employeeId to it.workingCategory2 }.toSet().size,
            displayAttended = rawDisplay.filter { it.attended }
                .map { it.employeeId to it.workingCategory2 }.toSet().size,
            eventTotal = rawEvent.map { Triple(it.employeeId, it.workingCategory2, it.workingCategory3) }.toSet().size,
            eventAttended = rawEvent.filter { it.attended }
                .map { Triple(it.employeeId, it.workingCategory2, it.workingCategory3) }.toSet().size,
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
            val workers = computeDailyWorkers(targetIds, currentDate)
            val total = workers.display.size + workers.event.size
            if (total > 0) {
                val attended =
                    workers.display.count { it.attended } + workers.event.count { it.attended }
                days.add(
                    LeaderCalendarDay(
                        date = currentDate.toString(),
                        total = total,
                        attended = attended
                    )
                )
            }
            currentDate = currentDate.plusDays(1)
        }

        return LeaderMonthlyCalendarResponse(year, month, days)
    }

    /**
     * 일별 진열/행사 워커 집계 — `getDailyStatus`(일별 현황) 와 월간 캘린더가 공유하는 핵심 로직.
     * - 진열: `DisplayWorkSchedule`(확정·기간유효) 중 안전점검 제출자만, 거래처별 출근은 진열
     *   team_member_schedule 행의 attendanceLog 존재로 판정.
     * - 행사: team_member_schedule cat1=EVENT.
     */
    private fun computeDailyWorkers(teamEmployeeIds: List<Long>, date: LocalDate): DailyWorkers {
        val schedules = teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, teamEmployeeIds)

        val safetyEmployeeIds = safetyCheckSubmissionRepository
            .findByEmployeeIdInAndWorkingDate(teamEmployeeIds, date)
            .mapNotNull { it.employeeId }
            .toSet()
        val displayAttendedKeys = schedules
            .asSequence()
            .filter { it.workingCategory1 == WorkingCategory1.DISPLAY && it.attendanceLog != null }
            .mapNotNull { s -> pairOrNull(s.employee?.id, s.account?.id) }
            .toSet()
        val display = displayWorkScheduleRepository
            .findConfirmedValidByEmployeeIdsAndDate(teamEmployeeIds, date)
            .filter { it.employee?.id in safetyEmployeeIds }
            .map { it.toDisplayWorkerItem(displayAttendedKeys) }

        val event = schedules
            .filter { it.workingCategory1 == WorkingCategory1.EVENT }
            .map { it.toWorkerItem() }

        return DailyWorkers(schedules = schedules, display = display, event = event)
    }

    private data class DailyWorkers(
        val schedules: List<TeamMemberSchedule>,
        val display: List<LeaderDailyWorkerItem>,
        val event: List<LeaderDailyWorkerItem>
    )

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
            displayWorkScheduleId = null,
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

    /**
     * 진열 마스터 → 일별 현황 워커 항목 (레거시 `selectDisplayAccList` 컬럼 매핑).
     * 진열은 마스터 출처(조회 전용)라 편집용 schedule id 가 없어 [scheduleId] = 0.
     * 카테고리: typeOfWork1→cat1(진열), typeOfWork5→cat2(상시/임시), typeOfWork3→cat3.
     */
    private fun DisplayWorkSchedule.toDisplayWorkerItem(
        attendedKeys: Set<Pair<Long, Long>>
    ): LeaderDailyWorkerItem {
        val empId = employee?.id
        val accId = account?.id
        return LeaderDailyWorkerItem(
            scheduleId = 0L,
            displayWorkScheduleId = id,
            employeeId = empId,
            employeeName = employee?.name.orEmpty(),
            employeeCode = employee?.employeeCode.orEmpty(),
            accountName = account?.name.orEmpty(),
            accountCode = account?.externalKey.orEmpty(),
            workingCategory1 = typeOfWork1?.displayName,
            workingCategory2 = typeOfWork5?.displayName,
            workingCategory3 = typeOfWork3?.displayName,
            attended = empId != null && accId != null && (empId to accId) in attendedKeys,
        )
    }

    private fun pairOrNull(a: Long?, b: Long?): Pair<Long, Long>? =
        if (a != null && b != null) a to b else null
}
