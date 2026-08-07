package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.dto.response.HomeResponse
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.support.notice.repository.NoticeRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.domain.activity.safetycheck.service.SafetyCheckService
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.productexpiration.repository.ProductExpirationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 홈 화면 비즈니스 로직 Service
 */
@Service
@Transactional(readOnly = true)
class HomeService(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val noticeRepository: NoticeRepository,
    private val accountRepository: AccountRepository,
    private val safetyCheckService: SafetyCheckService,
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository,
    private val productExpirationRepository: ProductExpirationRepository
) {

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    }

    /**
     * 홈 화면 데이터 통합 조회
     *
     * 역할별 분기:
     * - USER(여사원): 본인 스케줄, 안전점검 확인
     * - LEADER(조장): 팀 전체 스케줄, 안전점검 항상 false
     */
    fun getHomeData(userId: Long): HomeResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val today = LocalDate.now()

        // 역할별 TMS 조회 (행사 일정 집계 + 진열 출근여부 조회용)
        val (teamMemberSchedules, employeeMap) = fetchSchedulesByRole(employee, today)

        // 행사 일정: TMS 중 workingCategory1 = 행사 만 집계.
        // 레거시 `selectHomeSchedulePromote` 의 `workingcategory1__c = '행사'` 하드 필터 정합 —
        // 레거시는 진열 TMS 행을 홈 일정으로 카운트하지 않는다 (진열은 아래 확정 마스터에서만).
        val eventSchedules = teamMemberSchedules.filter { it.workingCategory1 == WorkingCategory1.EVENT }

        // 진열 출근여부 조회용: TMS 중 workingCategory1 = 진열 → (employeeId, accountId) 매핑.
        // 레거시 `selectHomeScheduleDisplay` 의 dtc2 LEFT JOIN(workingcategory1='진열') 정합 —
        // 진열 일정 존재 판정은 확정 마스터로, 출근여부(commutelogid)는 진열 TMS 에서 읽는다.
        val displayTmsByKey = teamMemberSchedules
            .filter { it.workingCategory1 == WorkingCategory1.DISPLAY }
            .mapNotNull { tms ->
                val empId = tms.employee?.id
                val accId = tms.account?.id
                if (empId != null && accId != null) Pair(empId, accId) to tms else null
            }
            .toMap()

        // 진열 일정: 확정 진열마스터 (레거시 `selectHomeScheduleDisplay` 의 `confirmed__c is true` 정합)
        val displayWorkSchedules = fetchDisplaySchedulesByRole(employee, employeeMap, today)

        // 스케줄 → 거래처명 매핑 (batch fetch)
        val accountMap = fetchAccountMap(eventSchedules, displayWorkSchedules)

        // 행사 TMS 중복 제거 + DTO 변환. 우선순위 판정을 위해 사원 PK 를 함께 보존한다
        // (DTO 의 employeeCode 는 nullable 컬럼이 `?: ""` 로 붕괴되므로 그룹핑 키로 쓸 수 없다).
        val eventInfos = eventSchedules
            .distinctBy { it.id }
            .map { tms -> tms.employee?.id to toTeamMemberScheduleInfo(tms, employeeMap, accountMap) }

        // 확정 진열마스터 → DTO 변환 (출근여부는 매칭되는 진열 TMS 에서 읽음)
        val displayInfos = displayWorkSchedules.map { dws ->
            dws.employee?.id to toDisplayWorkScheduleInfo(dws, employeeMap, accountMap, displayTmsByKey)
        }

        // 진열 임시 판정은 DTO 의 다형 label 슬롯(workCategory2 — 진열은 TypeOfWork5, 행사는
        // WorkingCategory2 의 displayName 이 담긴다) 이 아니라 enum 원본으로 수행한다.
        val temporaryDisplayEmployeeIds = displayWorkSchedules
            .filter { it.typeOfWork5 == TypeOfWork5.TEMPORARY }
            .mapNotNull { it.employee?.id }
            .toSet()

        // 진열 ∪ 행사 출처 선택 (레거시 HomeController:127-144 정합).
        //
        // 레거시는 사원×날짜 단위로 출처를 "배타적 승자독식"으로 하나 고르고(mergedList),
        // 이후 그 행의 workingcategory1 에 따라 진열/행사 중 한쪽 거래처 목록만 조회한다(:162-166).
        // 우선순위는 내 일정(MyScheduleService)과 동일:
        //   ① 출근등록 완료 → ② 진열 임시(typeOfWork5=임시) → ③ 행사 → ④ 진열
        //
        // 단순 합집합으로 두면 상시 진열과 행사가 겹친 날 레거시보다 항목이 많이 노출되고,
        // 같은 날 "내 일정"(4단 우선순위 적용)과 표시 건수가 어긋난다.
        val todaySchedules = selectByLegacyPriority(eventInfos, displayInfos, temporaryDisplayEmployeeIds)
            // 레거시 home.jsp 정합: 최종 표시는 거래처명 오름차순 정렬
            // (HomeController personmergedList.sort(name), null 은 "" 취급).
            .sortedBy { it.accountName ?: "" }

        // 출근/근태 영역 노출 대상 여부 (레거시 home.jsp: appauthority ∈ {여사원, 조장} 만 노출)
        // 지점장 / AccountViewAll / null(미매핑) 은 출근 영역 비노출 — 모바일이 이 플래그로 카드 자체를 숨긴다.
        val attendanceApplicable =
            employee.role == AppAuthority.WOMAN || employee.role == AppAuthority.LEADER

        // 출근 현황 집계
        // 조장: 레거시 home.jsp(promcnt/sum) 정합 — 팀원 단위(employeeId distinct) 집계 + 진열 비대칭.
        // 그 외(여사원/지점장): 본인 스케줄 건수 그대로 집계.
        val attendanceSummary = if (employee.role == AppAuthority.LEADER) {
            computeLeaderAttendanceSummary(
                teamMemberSchedules = teamMemberSchedules,
                eventSchedules = eventSchedules,
                displayWorkSchedules = displayWorkSchedules,
                teamEmployeeIds = employeeMap.keys,
                today = today
            )
        } else {
            HomeResponse.AttendanceSummaryInfo(
                totalCount = todaySchedules.size,
                registeredCount = todaySchedules.count { it.isCommuteRegistered }
            )
        }

        // 안전점검 필요 여부 (조장은 항상 false)
        val safetyCheckRequired = if (employee.role == AppAuthority.WOMAN) {
            val todayStatus = safetyCheckService.getTodayStatus(userId)
            !todayStatus.completed
        } else {
            false
        }

        val expiryCount = productExpirationRepository.countByEmployeeIdAndAlarmDate(employee.id, today)

        val expiryAlert = HomeResponse.ExpiryAlertInfo(
            branchName = employee.orgName ?: "",
            employeeName = employee.name,
            employeeCode = employee.employeeCode,
            expiryCount = expiryCount.toInt()
        )

        // 최근 공지사항 조회 (최신 5건)
        val notices = noticeRepository
            .findRecentNotices(branchCode = employee.costCenterCode ?: "")
            .map { notice ->
                HomeResponse.NoticeInfo(
                    id = notice.id,
                    title = notice.name ?: "",
                    category = notice.category?.apiCode ?: "",
                    categoryName = notice.category?.homeDisplayName ?: "",
                    createdAt = notice.createdAt ?: LocalDateTime.MIN
                )
            }

        return HomeResponse(
            todaySchedules = todaySchedules,
            attendanceSummary = attendanceSummary,
            attendanceApplicable = attendanceApplicable,
            safetyCheckRequired = safetyCheckRequired,
            expiryAlert = expiryAlert,
            notices = notices,
            currentDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * 진열 ∪ 행사 중 사원별 표시 출처를 레거시 우선순위로 선택 (레거시 HomeController:127-144).
     *
     * 레거시는 `employeeid__c` 기준으로 진열/행사 중 하나만 남기는 배타 선택을 수행한다.
     * 조장(팀 전원)·여사원(본인) 양쪽 모두 같은 규칙을 타므로 사원 단위로 그룹핑해 판정한다.
     *
     * 우선순위:
     *  1. 출근등록 완료행이 있으면 진열·행사를 함께 노출 (레거시 staticCommList 우선).
     *     신규 차이(의도적): 레거시 HomeController:178-188 은 이때 (근무유형1, 근무유형2) 가
     *     일치하지 않는 미등록 거래처를 버리지만(`disAccList` 미할당), 그날 방문할 다른 매장이
     *     사라지는 정보 손실이라 재현하지 않는다 (MyScheduleService 와 동일 판단).
     *  2. 진열 임시(typeOfWork5=임시)가 있으면 진열만 (레거시 tempList)
     *  3. 행사가 있으면 행사만 (레거시 promoteTempList — 진열을 밀어낸다)
     *  4. 나머지는 진열 (레거시 displayTempList)
     *
     * 그룹핑 키는 사원 PK 다. DTO 의 `employeeCode` 는 nullable 컬럼(`Employee.employeeCode`)이
     * `?: ""` 로 붕괴되므로, 사번 미보유 사원이 여러 명이면 한 그룹으로 병합되어 한 사람의 행사가
     * 다른 사람의 진열을 밀어낸다. 레거시 그룹핑 축(`employeeid__c` = 사원 Id)과도 PK 가 정합이다.
     *
     * @param temporaryDisplayEmployeeIds 진열 임시(TypeOfWork5.TEMPORARY) 를 가진 사원 PK 집합.
     *   판정을 DTO 의 다형 label 슬롯이 아니라 enum 원본으로 수행하기 위해 호출부에서 계산해 넘긴다.
     */
    private fun selectByLegacyPriority(
        eventInfos: List<Pair<Long?, HomeResponse.TeamMemberScheduleInfo>>,
        displayInfos: List<Pair<Long?, HomeResponse.TeamMemberScheduleInfo>>,
        temporaryDisplayEmployeeIds: Set<Long>
    ): List<HomeResponse.TeamMemberScheduleInfo> {
        // 출처를 명시적으로 태깅한다 — DTO 내용이 우연히 같아도(같은 사원·거래처) 오분류되지 않도록.
        val tagged = eventInfos.map { (empId, info) -> Triple(empId, true, info) } +
            displayInfos.map { (empId, info) -> Triple(empId, false, info) }

        // 사원 PK 가 없는 고아 스케줄은 그룹 병합에서 제외하고 그대로 노출한다
        // (null 끼리 한 그룹으로 묶여 서로를 밀어내는 것을 막는다).
        val (identified, orphans) = tagged.partition { (empId, _, _) -> empId != null }

        val selected = identified.groupBy { (empId, _, _) -> empId }.values.flatMap { entries ->
            val employeeId = entries.first().first
            val events = entries.filter { (_, isEvent, _) -> isEvent }.map { (_, _, info) -> info }
            val displays = entries.filterNot { (_, isEvent, _) -> isEvent }.map { (_, _, info) -> info }
            when {
                entries.any { (_, _, info) -> info.isCommuteRegistered } -> entries.map { (_, _, info) -> info }
                employeeId in temporaryDisplayEmployeeIds -> displays
                events.isNotEmpty() -> events
                else -> displays
            }
        }

        return selected + orphans.map { (_, _, info) -> info }
    }

    /**
     * 역할별 스케줄 조회
     * @return Pair(스케줄 목록, employeeId→Employee 매핑)
     */
    private fun fetchSchedulesByRole(employee: Employee, today: LocalDate): Pair<List<TeamMemberSchedule>, Map<Long, Employee>> {
        return when (employee.role) {
            AppAuthority.LEADER -> {
                // 레거시 home.jsp 조장 팀 범위 정합: 조장과 동일 costcentercode 전원
                // (employeeMapper 의 costcentercode__c 서브쿼리). orgName 이 아니라 costCenterCode 기준.
                val teamEmployees = employeeRepository.findByCostCenterCode(employee.costCenterCode ?: "")
                val teamMemberSchedules = if (teamEmployees.isNotEmpty()) {
                    teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(today, teamEmployees)
                } else {
                    emptyList()
                }
                val employeeMap = teamEmployees.associateBy { it.id }
                Pair(teamMemberSchedules, employeeMap)
            }
            else -> {
                val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)
                val employeeMap = mapOf(employee.id to employee)
                Pair(teamMemberSchedules, employeeMap)
            }
        }
    }

    /**
     * 조장 홈 "팀 출근 현황: N명 중 M명 등록 완료" 집계 (레거시 home.jsp:509~531 정합)
     *
     * 레거시 mergedList 는 `employeeid__c` 기준 1팀원 1행으로 중복 제거되므로, 카운트도 팀원 단위(distinct).
     *
     * - 분모 N (promcnt): 행사 근무자(무조건) + 진열 근무자 중 `comm_cnt > 0`(= 안전점검 실시 = swm 레코드 존재)인 팀원.
     *   진열 비대칭 — 진열만 있고 안전점검 미실시 팀원은 분모에서 제외 (레거시 home.jsp:515 정합).
     * - 분자 M (sum): `commutelogid__c != null`(= attendanceLog 존재 = 출근 등록 완료)인 팀원 (레거시 home.jsp:518 정합).
     *
     * 분모/분자는 독립 집계이므로, 안전점검 없이 출근 등록만 한 진열 팀원은 M 에는 잡히나 N 에는 빠질 수 있다(레거시 동등).
     */
    private fun computeLeaderAttendanceSummary(
        teamMemberSchedules: List<TeamMemberSchedule>,
        eventSchedules: List<TeamMemberSchedule>,
        displayWorkSchedules: List<DisplayWorkSchedule>,
        teamEmployeeIds: Collection<Long>,
        today: LocalDate
    ): HomeResponse.AttendanceSummaryInfo {
        // 안전점검 실시(swm = safetycheck__workschedule__member 레코드 존재) 팀원 집합 — 레거시 comm_cnt > 0 정합
        val safetyCheckedIds = if (teamEmployeeIds.isEmpty()) {
            emptySet()
        } else {
            safetyCheckSubmissionRepository
                .findByEmployeeIdInAndWorkingDate(teamEmployeeIds.toList(), today)
                .mapNotNull { it.employeeId }
                .toSet()
        }

        val eventEmployeeIds = eventSchedules.mapNotNull { it.employee?.id }.toSet()
        val displayEmployeeIds = displayWorkSchedules.mapNotNull { it.employee?.id }.toSet()

        // 분모 N: 행사 근무자(무조건) + 진열 근무자 중 안전점검 실시자 — employeeId distinct
        val denominatorIds = eventEmployeeIds + displayEmployeeIds.filter { it in safetyCheckedIds }

        // 분자 M: 출근 등록 완료(attendanceLog != null) 팀원 — employeeId distinct
        val registeredIds = teamMemberSchedules
            .filter { it.attendanceLog != null }
            .mapNotNull { it.employee?.id }
            .toSet()

        return HomeResponse.AttendanceSummaryInfo(
            totalCount = denominatorIds.size,
            registeredCount = registeredIds.size
        )
    }

    /**
     * 역할별 확정 진열마스터 조회
     */
    private fun fetchDisplaySchedulesByRole(
        employee: Employee,
        employeeMap: Map<Long, Employee>,
        today: LocalDate
    ): List<DisplayWorkSchedule> {
        val employeeIds = employeeMap.keys.toList()
        if (employeeIds.isEmpty()) return emptyList()
        return displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(employeeIds, today)
    }

    /**
     * 스케줄의 accountId → Account 이름 매핑 (batch fetch)
     */
    private fun fetchAccountMap(
        teamMemberSchedules: List<TeamMemberSchedule>,
        displayWorkSchedules: List<DisplayWorkSchedule> = emptyList()
    ): Map<Long, String> {
        val accountIds = (
            teamMemberSchedules.mapNotNull { it.account?.id } +
            displayWorkSchedules.mapNotNull { it.account?.id }
        ).distinct()
        if (accountIds.isEmpty()) return emptyMap()
        return accountRepository.findByIdIn(accountIds)
            .associate { it.id to (it.name ?: "") }
    }

    /**
     * TeamMemberSchedule entity → TeamMemberScheduleInfo DTO 변환
     */
    private fun toTeamMemberScheduleInfo(
        teamMemberSchedule: TeamMemberSchedule,
        employeeMap: Map<Long, Employee>,
        accountMap: Map<Long, String>
    ): HomeResponse.TeamMemberScheduleInfo {
        val matchedEmployee = teamMemberSchedule.employee?.id?.let { employeeMap[it] }
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = teamMemberSchedule.id,
            employeeName = matchedEmployee?.name ?: "",
            employeeCode = matchedEmployee?.employeeCode ?: "",
            accountName = teamMemberSchedule.account?.id?.let { accountMap[it] },
            accountId = teamMemberSchedule.account?.id,
            workCategory = teamMemberSchedule.workingCategory1?.displayName ?: "",
            // 행사 일정 2번째 토큰 — 레거시 selectAccList `workingcategory2__c`(전담/진열겸임) 정합.
            workCategory2 = teamMemberSchedule.workingCategory2?.displayName,
            // 근무형태(고정/순회/격고) — 레거시 workingcategory3__c 정합.
            // (DisplayWorkSchedule 측 typeOfWork3 와 의미 일치)
            workType = teamMemberSchedule.workingCategory3?.displayName,
            // 근무유형4 — 레거시 selectAccList(행사) 만 조회하는 컬럼. 정본은 SF `SecondWorkType__c`
            // (행사마스터 제품유형 파생), 구 `WorkingCategory4__c` 는 SF 이관 row 용 fallback.
            secondWorkType = teamMemberSchedule.secondWorkTypeLabel,
            isCommuteRegistered = teamMemberSchedule.attendanceLog != null,
            commuteRegisteredAt = teamMemberSchedule.commuteReportDatetime
        )
    }

    /**
     * DisplayWorkSchedule entity → TeamMemberScheduleInfo DTO 변환
     * 레거시 동작: 확정 진열마스터가 홈화면 스케줄에 포함되어 출근 등록 가능.
     * 출근여부는 매칭되는 진열 TMS([displayTmsByKey])에서 읽는다
     * (레거시 `selectHomeScheduleDisplay` 의 dtc2 LEFT JOIN 정합).
     */
    private fun toDisplayWorkScheduleInfo(
        displayWorkSchedule: DisplayWorkSchedule,
        employeeMap: Map<Long, Employee>,
        accountMap: Map<Long, String>,
        displayTmsByKey: Map<Pair<Long, Long>, TeamMemberSchedule>
    ): HomeResponse.TeamMemberScheduleInfo {
        val matchedEmployee = displayWorkSchedule.employee?.id?.let { employeeMap[it] }
        val empId = displayWorkSchedule.employee?.id
        val accId = displayWorkSchedule.account?.id
        val matchedTms = if (empId != null && accId != null) displayTmsByKey[Pair(empId, accId)] else null
        return HomeResponse.TeamMemberScheduleInfo(
            scheduleId = 0,
            displayWorkScheduleId = displayWorkSchedule.id,
            employeeName = matchedEmployee?.name ?: "",
            employeeCode = matchedEmployee?.employeeCode ?: "",
            accountName = displayWorkSchedule.account?.id?.let { accountMap[it] },
            accountId = displayWorkSchedule.account?.id,
            workCategory = displayWorkSchedule.typeOfWork1?.displayName ?: "진열",
            // 진열 일정 2번째 토큰 — 레거시 selectDisplayAccList `typeofwork5__c`(상시/임시) 정합.
            workCategory2 = displayWorkSchedule.typeOfWork5?.displayName,
            // 근무형태(고정/순회/격고) — 레거시 workingcategory3__c 정합.
            workType = displayWorkSchedule.typeOfWork3?.displayName,
            // 진열은 레거시 selectDisplayAccList 가 workingcategory4__c 를 조회하지 않아 항상 null.
            secondWorkType = null,
            isCommuteRegistered = matchedTms?.attendanceLog != null,
            commuteRegisteredAt = matchedTms?.commuteReportDatetime
        )
    }
}
