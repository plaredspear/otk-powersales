package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.dto.response.DailySummaryDto
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.sap.TeamMemberScheduleSapPayloadRow
import com.otoki.powersales.domain.foundation.account.entity.Account
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 사원의 가장 최근 출근(근무)등록 1건의 근무 분류 (여사원 현황 근무형태 컬럼용).
 * workingCategory1(진열/행사) + workingCategory3(고정/격고/순회) 의 displayName 조합 표시는 서비스 레이어 책임.
 */
data class LatestAttendanceCategory(
    val employeeId: Long,
    val workingCategory1: String?,
    val workingCategory3: String?,
)

interface TeamMemberScheduleRepositoryCustom {

    /**
     * 진열스케줄마스터 목록 출근등록 수 표시용 — 진열마스터 id 목록별 **실제 출근(commuteReportDatetime 채워짐)**
     * 일정 건수를 페이지 단위 1쿼리로 집계 (id IN + GROUP BY, N+1 회피).
     * 출근 0건 마스터는 결과에서 제외되므로, 반환 Map 에 키가 없으면 호출처가 0 으로 처리한다.
     *
     * @return Map<displayWorkScheduleId, attendanceCount> (출근 1건 이상인 마스터만 포함)
     */
    fun countAttendedByDisplayWorkScheduleIds(scheduleIds: List<Long>): Map<Long, Long>

    /**
     * 여사원 현황 "근무형태" 컬럼용 — 사원별 **가장 최근 출근(근무)등록 1건**의
     * workingCategory1(진열/행사) + workingCategory3(고정/격고/순회) 를 페이지 단위 1쿼리로 조회.
     *
     * "출근등록한 내용" 판정: `attendance_log_id IS NOT NULL` (출퇴근 로그가 연결된 일정 = `isWorkReport` 정합).
     * "가장 최근" 판정: 사원별 MAX(working_date), 동일 일자 다건이면 id 최대(=마지막 등록) 1건.
     *
     * 성능: 전체 이력을 메모리로 가져오지 않도록 2쿼리로 분리한다 — (1) 사원별 MAX(working_date) GROUP BY
     * (부분 covering index idx_team_member_schedule_employee_id_working_date 로 index-only scan),
     * (2) (사원, 최근일자) 쌍에 해당하는 소수 행만 조회해 category1/3 추출. employeeId IN 으로 현재
     * 페이지 사원만 조회(N+1 없음, 전송 행 수 ≈ 사원 수). 출근등록 0건 사원은 반환 Map 에 키가 없다.
     *
     * @return Map<employeeId, LatestAttendanceCategory> (출근등록 1건 이상인 사원만 포함)
     */
    fun findLatestAttendanceCategoriesByEmployeeIds(employeeIds: List<Long>): Map<Long, LatestAttendanceCategory>

    /**
     * 출근 등록 시점의 attendance_log id-FK 채움 (Spec #789 — sfid 비즈니스 로직 사용 금지 정책 정합).
     * 운영에서는 HC sync 가 sfid 컬럼을 채우고 본 메서드는 application 레이어 backlink 보강 책임 (dev mock 측 동등 시뮬레이션 포함).
     */
    fun updateAttendanceLog(id: Long, attendanceLogId: Long)

    fun updateSafetyCheckData(
        id: Long,
        equipment1: String?,
        equipment2: String?,
        equipment3: String?,
        equipment4: String?,
        equipment5: String?,
        equipment6: String?,
        equipment7: String?,
        equipment8: String?,
        equipment9: String?,
        yesChkCnt: Double?,
        noChkCnt: Double?,
        startTime: LocalDateTime?,
        completeTime: LocalDateTime?,
        precaution: String?,
        precautionChk: Double?,
        traversalFlag: String?
    )

    fun findByEmployeeIdAndWorkingDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    /**
     * 조장 여사원 일별 현황 조회 (레거시 `employee/mngDaily.jsp` — 조회 전용).
     * `team_member_schedule` ⋈ employee ⋈ account ⋈ attendance_log fetchJoin 으로
     * 분류(진열/행사/연차)·출근 판정에 필요한 LAZY 연관을 한 번에 로드 (N+1 회피).
     * 필터: workingDate = date, employee.id ∈ employeeIds.
     */
    fun findDailyStatusByEmployeeIds(date: LocalDate, employeeIds: List<Long>): List<TeamMemberSchedule>

    fun findMonthlyByEmployeeIds(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
        promotionTeams: List<String>? = null
    ): List<TeamMemberSchedule>

    fun findMonthlyByAccountIds(
        accountIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
        promotionTeams: List<String>? = null
    ): List<TeamMemberSchedule>

    /**
     * 여사원 일정관리 무필터 조회용 일별 요약 집계 (SF `FullCalendarComponentController.fetchScheduleSummary` 정합).
     *
     * 개별 일정 row 를 fetch 하지 않고 DB GROUP BY 로 날짜별 COUNT 만 산출 — 거래처/여사원 미선택 시
     * 캘린더/목록 요약 배지에만 쓰이는 집계를 가볍게 조회한다. `employeeIds` (본인/특수사번 costCenterCode
     * 기준 active 여사원) 와 기간으로만 필터하며 account JOIN 을 타지 않는다.
     */
    fun aggregateDailySummaryByEmployeeIds(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate
    ): List<DailySummaryDto>

    fun findActiveByEmployeeIdAndDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    fun deleteAnnualLeaveByEmployeeIdAndDateRange(employeeId: Long, from: LocalDate, to: LocalDate): Long

    fun deleteFutureWorkSchedulesByEmployeeId(employeeId: Long, fromDate: LocalDate): Long

    fun findAnnualLeaveByDateRange(from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findAnnualLeaveByDateRangeAndEmployeeIds(from: LocalDate, to: LocalDate, employeeIds: List<Long>): List<TeamMemberSchedule>

    fun findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId: Long, fromDate: LocalDate, toDate: LocalDate): List<Long>

    /**
     * 팀멤버스케줄에 등록된 거래처 (중복 제거) — 거래처명/코드 keyword 필터 + 결과 상한(limit).
     * 레거시 `accountMapper.selectAllAccount` 정합 — 부서장(AppAuthority.ACCOUNT_VIEW_ALL) 매출 조회.
     * 레거시 드롭다운이 `keyvalue` 검색 + `LIMIT/OFFSET` 페이지네이션을 쓰던 것과 동일하게,
     * 전사 일정 거래처 전체(수천 건)를 한 번에 반환하지 않도록 DB 레벨에서 필터·제한한다.
     */
    fun findDistinctScheduledAccounts(keyword: String?, limit: Int): List<Account>

    /**
     * 팀장(teamLeader) 기준 거래처 ID (중복 제거).
     * 레거시 `accountMapper.selectMyAccount` 의 조장 분기(`teamleadersfid__c = 본인 sfid`) 정합 —
     * 특정 조장이 팀장으로 배정된 일정의 거래처를 기간 내 조회한다. sfid 비즈니스 로직 금지 정책 정합으로
     * sfid 가 아닌 teamLeader id-FK 로 필터한다. `toDate` 는 반열림(exclusive) 상한.
     */
    fun findDistinctAccountIdsByTeamLeaderIdAndDateRange(teamLeaderId: Long, fromDate: LocalDate, toDate: LocalDate): List<Long>

    fun findIntegrationScheduleRecords(employeeIds: List<Long>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findWorkSchedulesByEmployeeAndAccountAndMonth(employeeId: Long, accountId: Long, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    /**
     * 사원의 특정 일자 근무(WORK) TMS row 개수.
     * 환산근무일수 산정 시 그날 1/N 분할의 N — SF 레거시 insert 경로(`TeamMemberScheduleTriggerHandler.getEquivalentNumberOfWorkingDays`)
     * 와 동일하게 거래처 distinct 가 아니라 TMS row 개수를 센다.
     */
    fun countWorkScheduleRowsByEmployeeAndDate(employeeId: Long, workingDate: LocalDate): Int

    /**
     * 사원이 해당 지점(costCenterCode) 소속으로 그 달 근무(WORK)한 서로 다른 날짜 수.
     * 환산인원 산정 시 분모(SF 레거시 `WorkingDaysMonth__c`) — 거래처 무관, 사원+지점코드+년월 단위 distinct workingDate.
     */
    fun countDistinctWorkingDatesByEmployeeAndCostCenterAndMonth(
        employeeId: Long,
        costCenterCode: String,
        from: LocalDate,
        to: LocalDate
    ): Int

    /**
     * 일반 출근(REGULAR) SAP daily batch 용 페이지 조회.
     * `team_member_schedule` ⋈ `attendance_log` (attendance_log_id 단방향 FK JOIN, Spec #789) + employee + account.
     * 필터: attendanceType=REGULAR, workingType='근무', workingDate ∈ {today, yesterday}.
     */
    fun findRegularAttendancesForSapPaged(
        today: LocalDate,
        yesterday: LocalDate,
        limit: Int,
        offset: Int
    ): List<TeamMemberScheduleSapPayloadRow>

    /**
     * 여사원 배치 점검 조회 (Spec #839 — SF Report `InternalSalesReportFolder/new_report_4Ic` 이식).
     * `team_member_schedule` ⋈ employee ⋈ account.
     * 필터: workingDate ∈ [from, to], workingType='근무', employee.role ∈ {여사원, 조장},
     *       employee.name 더미(테스트용/관리자/파워세일즈) 제외, employee.isDeleted=false (퇴직 status 는 포함 — 퇴직자 포함),
     *       branchCodes 비어있지 않으면 teamMemberSchedule.costCenterCode ∈ branchCodes (사원 소속 지점 — SF 정합).
     */
    fun findPlacementCheck(
        from: LocalDate,
        to: LocalDate,
        roles: List<String>,
        branchCodes: List<String>,
    ): List<TeamMemberSchedule>

    /**
     * 여사원 개인별 근무내역 조회 (Spec #840 — SF Report `InternalSalesReportFolder/new_report_nEX` 이식).
     * `team_member_schedule` ⋈ employee ⋈ account.
     * 필터: employee.employeeCode = employeeCode, workingDate ∈ [from, to],
     *       branchCodes 비어있지 않으면 teamMemberSchedule.costCenterCode ∈ branchCodes (사원 소속 지점 — SF 정합).
     */
    fun findWorkHistory(
        employeeCode: String,
        from: LocalDate,
        to: LocalDate,
        branchCodes: List<String>,
    ): List<TeamMemberSchedule>

    /**
     * 기간별 근무내역(개인) 집계용 조회 — 지점 스코프 내 전체 여사원의 기간 근무 행.
     * `team_member_schedule` ⋈ employee ⋈ account.
     * 필터: workingDate ∈ [from, to], attendance_log_id IS NOT NULL (출근 등록 기준 — 월별근무내역 목록과 정합),
     *       branchCodes 비어있지 않으면 teamMemberSchedule.costCenterCode ∈ branchCodes (사원 소속 지점),
     *       keyword 있으면 employee.employeeCode 정확일치 OR employee.name 부분일치.
     * 여사원별 집계는 서비스 레이어에서 수행 (employee.employeeCode 기준 그룹핑).
     */
    fun findWorkHistoryForPeriod(
        from: LocalDate,
        to: LocalDate,
        branchCodes: List<String>,
        keyword: String?,
    ): List<TeamMemberSchedule>

    /**
     * 판매여사원 일일 안전점검 현황 조회 (Spec #841 — SF Report `new_report_wce`/`new_report_oJO` 이식).
     * `team_member_schedule` ⋈ employee ⋈ account.
     * 필터: workingDate = date, traversalFlag='O' (순회/점검 대상), yesChkCnt IS NOT NULL (점검 완료),
     *       branchCodes 비어있지 않으면 teamMemberSchedule.costCenterCode ∈ branchCodes (사원 소속 지점 — SF 정합).
     * 정렬: workingCategory1 오름차순.
     */
    fun findSafetyCheckReport(
        date: LocalDate,
        branchCodes: List<String>,
    ): List<TeamMemberSchedule>

    /**
     * 판매여사원 일일 안전점검 현황 (RPA용) 조회 (Spec #842 — SF Report `X00/new_report_xdB` 이식).
     * `findSafetyCheckReport` 와 동일 필터(workingDate/traversalFlag='O'/yesChkCnt IS NOT NULL)지만,
     * (a) 지점 스코프 없음 (전사 고정 — SF scope=organization), (b) ownerUser fetchJoin (CUST_NAME 컬럼용).
     * 정렬: workingCategory1 오름차순.
     */
    fun findSafetyCheckReportRpa(
        date: LocalDate,
    ): List<TeamMemberSchedule>
}
