package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.dto.response.DailySummaryDto
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.sap.TeamMemberScheduleSapPayloadRow
import com.otoki.powersales.domain.foundation.account.entity.Account
import java.time.LocalDate

/**
 * 사원의 가장 최근 출근(근무)등록 1건의 정보 (여사원 현황 근무형태/근무거래처 컬럼용).
 * - 근무형태: workingCategory1(진열/행사) + workingCategory3(고정/격고/순회) 의 displayName 조합 — 서비스 레이어 책임.
 * - 근무거래처: 해당 일정의 거래처명(account.name) / 거래처코드(account.externalKey, SAP거래처코드).
 */
data class LatestAttendanceInfo(
    val employeeId: Long,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val accountName: String?,
    val accountCode: String?,
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
     * 여사원 현황 "근무형태/근무거래처" 컬럼용 — 사원별 **가장 최근 출근(근무)등록 1건**의
     * workingCategory1(진열/행사) + workingCategory3(고정/격고/순회) + 거래처명/거래처코드를 조회.
     *
     * "출근등록한 내용" 판정: `attendance_log_id IS NOT NULL` (출퇴근 로그가 연결된 일정 = `isWorkReport` 정합).
     * "가장 최근" 판정: 사원별 MAX(working_date), 동일 일자 다건이면 id 최대(=마지막 등록) 1건.
     *
     * 성능: 전체 이력을 메모리로 가져오지 않도록 2쿼리로 분리한다 — (1) 사원별 MAX(working_date) GROUP BY
     * (부분 covering index idx_team_member_schedule_employee_id_working_date 로 index-only scan),
     * (2) (사원, 최근일자) 쌍에 해당하는 소수 행만 조회해 category1/3 + 거래처(account JOIN) 추출.
     * employeeId IN 으로 현재 페이지 사원만 조회(N+1 없음, 전송 행 수 ≈ 사원 수). 출근등록 0건 사원은
     * 반환 Map 에 키가 없다.
     *
     * @return Map<employeeId, LatestAttendanceInfo> (출근등록 1건 이상인 사원만 포함)
     */
    fun findLatestAttendanceInfoByEmployeeIds(employeeIds: List<Long>): Map<Long, LatestAttendanceInfo>

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

    /**
     * MFEIS(월별여사원 통합일정) 집계 모수 — 사원+월의 **출근등록된** TMS row 전건.
     *
     * SF 레거시 `TeamMemberScheduleTriggerHandler.updateMonthlyFemaleEmployeeIntegrationSchedule` 의
     * 모수 SOQL (`WHERE EmpCode IN … AND CALENDAR_YEAR/MONTH … AND DKRetail__CommuteLogId__c != null
     * AND AccountId__c != null`) 동등. 레거시와 동일하게 workingType 필터는 없다 — 출근현황
     * (attendanceLog) 연결 자체가 근무 필터 역할. soft-delete(isNotDeleted) 는 신규 시스템 자체 개념으로 유지.
     */
    fun findAttendedSchedulesByEmployeeAndMonth(employeeId: Long, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    /**
     * MFEIS row 의 집계 근거 일정 — `monthly_female_employee_integration_schedule_id` FK 기반 조회.
     *
     * `refreshIntegration` 재집계 시 각 근거 TMS row 에 세팅한 FK 로 역참조한다 (externalKey 문자열
     * 재매칭 대신). 상세 응답에 필요한 employee/account/attendanceLog 는 fetch join 으로 선로딩한다.
     */
    fun findSchedulesByIntegrationScheduleId(integrationScheduleId: Long): List<TeamMemberSchedule>

    /**
     * 삭제 예정 MFEIS row 를 가리키던 TMS 의 FK(`monthly_female_employee_integration_schedule_id`) 를
     * 일괄 null 처리한다 (dangling FK 방지). QueryDSL 벌크 update — 반환값은 갱신 row 수.
     * 벌크 연산이라 영속 컨텍스트를 우회하므로, 같은 tx 에서 해당 TMS 를 이후 다시 읽지 않는 흐름에서만 사용.
     */
    fun detachIntegrationScheduleByIds(integrationScheduleIds: List<Long>): Long

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
     * 여사원일정 근무 SAP **단건** 테스트 송신용 조회 (admin SAP outbound 테스트 탭).
     * `findRegularAttendancesForSapPaged` 와 동일한 [TeamMemberScheduleSapPayloadRow] projection 을
     * scheduleId 단건으로 조회한다. batch 의 WHERE 필터(workingType='근무'/날짜 branch)는 적용하지 않고
     * id 로만 특정한다 — 테스트 목적상 임의 일정 1건을 그대로 송신 payload 로 만들 수 있게 한다.
     * `WorkingCategory4`(전일 보정분) 채움 여부는 상위 [TeamMemberScheduleSapPayloadFactory] 가
     * 기준일(today) 대비 `workingDate.isBefore(today)` 로 판정하므로, 발송 당시 payload 를 재현하려면
     * 호출부가 기준일을 그에 맞게 전달한다. 존재하지 않으면 null.
     */
    fun findByIdForSap(scheduleId: Long): TeamMemberScheduleSapPayloadRow?

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
     * 기간별 근무내역(개인) 거래처별 집계용 조회 — 특정 여사원 1명의 기간 근무 행.
     * `team_member_schedule` ⋈ employee ⋈ account.
     * 필터: employee.employeeCode 정확일치, workingDate ∈ [from, to],
     *       attendance_log_id IS NOT NULL (출근 등록 기준 — 기간별 집계와 정합),
     *       branchCodes 비어있지 않으면 teamMemberSchedule.costCenterCode ∈ branchCodes (사원 소속 지점).
     * 거래처별 집계는 서비스 레이어에서 수행 (account.id 기준 그룹핑).
     */
    fun findWorkHistoryForPeriodByEmployee(
        employeeCode: String,
        from: LocalDate,
        to: LocalDate,
        branchCodes: List<String>,
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
     * `findSafetyCheckReport` 와 동일 필터(workingDate/traversalFlag='O'/yesChkCnt IS NOT NULL)에
     * branchCodes 비어있지 않으면 teamMemberSchedule.costCenterCode ∈ branchCodes (사원 소속 지점 — SF 정합).
     * `findSafetyCheckReport` 와 차이: ownerUser fetchJoin (CUST_NAME 컬럼용).
     * 신규 차이 — 레거시 RPA 는 SF scope=organization 전사였으나, 지점 필터 지원을 위해 branchCodes 스코프 추가.
     * 정렬: workingCategory1 오름차순.
     */
    fun findSafetyCheckReportRpa(
        date: LocalDate,
        branchCodes: List<String>,
    ): List<TeamMemberSchedule>
}
