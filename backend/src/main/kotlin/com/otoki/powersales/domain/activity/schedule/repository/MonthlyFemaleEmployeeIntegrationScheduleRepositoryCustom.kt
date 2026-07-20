package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import java.math.BigDecimal

/**
 * 투입현황 대시보드 집계 전용 평면 projection — 집계에 실제 쓰이는 MFEIS 5필드 + 투입 거래처 3필드만 노출.
 *
 * 기존 [MonthlyFemaleEmployeeIntegrationSchedule] entity 전 컬럼 + [account fetch join] (account 80여 컬럼)
 * 대신 select 페이로드를 8개 컬럼으로 축소한다 (대시보드 3섹션이 쓰는 필드 합집합):
 * - MFEIS: convertedHeadcount(환산인원 SUM), workingCategory1(진열/행사), workingCategory3(고정/격고/순회),
 *   workingCategory4(상온/냉동/냉장/라면/만두 — 행사 스택 축, SF WorkTypeForReport__c 정합)
 * - 거래처: accountId(distinct 키), accountExternalKey(SAP 매출 매칭), accountType(유통 구분 라벨)
 *
 * 투입 거래처 정보가 없는 row 는 account* 가 null.
 */
data class DashboardDeploymentRow(
    val convertedHeadcount: BigDecimal?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val accountId: Long?,
    val accountExternalKey: String?,
    val accountType: String?,
)

/**
 * MFEIS 외부 조회(OVIP inbound) 전량 스냅샷 projection — 레거시 여사원 통합일정 조회/엑셀 export 컬럼 셋 정합.
 *
 * entity 전 컬럼 + account/employee fetch join(80여 컬럼) 대신 노출 필드만 명시 select 하여
 * select 페이로드를 축소한다 (전량 스냅샷 배치 조회 부하 방지). [id] 는 keyset 커서 기준.
 * 관계(account/employee) 미연결 row 는 해당 컬럼이 null (left join).
 */
data class MfeisSnapshotRow(
    val id: Long,
    val sfid: String?,
    val externalKey: String?,
    val year: String?,
    val month: String?,
    val costCenterCode: String?,
    val orgName: String?,
    val employeeCode: String?,
    val employeeName: String?,
    val title: String?,
    val accountCode: String?,
    val accountName: String?,
    val accountBranchName: String?,
    val accountType: String?,
    val abcType: String?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    val numberOfInputs: BigDecimal?,
    val equivalentNumberOfWorkingDays: BigDecimal?,
    val convertedHeadcount: BigDecimal?,
)

/**
 * MFEIS Querydsl 확장 — 거래처유형별 환산인원 현황 보고서 조회 (Spec #847).
 */
interface MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustom {

    /**
     * MFEIS 전량 스냅샷 keyset 페이지 조회 (OVIP inbound).
     *
     * `year` + `month` 로 좁힌 뒤 PK keyset(`id > cursor`) 으로 순차 인출한다. isDeleted 제외.
     * account/employee 는 left join 하여 노출 필드만 [MfeisSnapshotRow] projection 으로 select
     * (fetch join 회피). ORDER BY id ASC 로 결정적 순서 보장.
     *
     * year+month 를 요청마다 고정하므로 단일 연월 안에서 PK 커서가 이동 → PK 단일 커서로 충분.
     *
     * @param year   연도 (entity year 가 String — 문자열 비교)
     * @param month  월 (entity month 가 String)
     * @param cursor 직전 페이지 마지막 id. null 이면 처음부터 (id 조건 미적용)
     * @param limit  최대 조회 건수. hasNext 판정을 위해 호출자가 (pageSize + 1) 을 넘긴다
     */
    fun findSnapshotByKeyset(
        year: String,
        month: String,
        cursor: Long?,
        limit: Int,
    ): List<MfeisSnapshotRow>

    /**
     * MFEIS 단건 + employee/account fetch join — 상세(집계 근거) 조회용.
     * LAZY 미초기화 → 응답 필드 null 방지 (enhancement 환경).
     */
    fun findByIdWithEmployeeAndAccount(id: Long): MonthlyFemaleEmployeeIntegrationSchedule?

    /**
     * 거래처유형별 환산인원 현황 보고서 조회 (SF Report 5변형 베이스).
     *
     * @param year                연도 (entity year 가 String — 문자열 비교)
     * @param month               월 (entity month 가 String)
     * @param workingCategory5In  근무유형5 IN 값집합 (variant 별)
     * @param includeNullWc5      근무유형5 빈/NULL 포함 여부 (SF multi-value equals 의 선두 빈 값)
     * @param excludeConsignment  위탁농협(Account.consignmentAcc) 제외 여부 (1-2/1-5)
     * @param costCenterCode      영업지원2팀 코스트센터 코드 필터 (2-1 = "4889", 그 외 null)
     * @param accountTypeFilter   구분(Account.accountType) equals 필터 displayName (대리점 3종 = "대리점" 등). null = 전체
     * @param accountTypeNotIn    구분(Account.accountType) notIn 제외 필터 displayName 목록 (2팀분리 = 대리점·백화점). 빈 목록 = 미적용
     * @param excludeEmpBranchName 사원지점명(EmpBranchName) notEqual 제외 필터 (2팀분리 = 영업지원2팀). null = 미적용
     * @param branchScopeCodes    지점 스코프 코스트센터 코드 목록 (mfeis.costCenterCode IN). 빈 목록 = 전사(미적용).
     *                            소속기준 variant 만 채워 넘긴다 (거래처기준 variant 는 빈 목록 = 전사 유지).
     *
     * isDeleted 제외. account fetch join. 지점 스코프는 여사원 소속 지점(costCenterCode) 기준.
     */
    fun findConvertedHeadcountReport(
        year: String,
        month: String,
        workingCategory5In: List<String>,
        includeNullWc5: Boolean,
        excludeConsignment: Boolean,
        costCenterCode: String?,
        accountTypeFilter: String?,
        accountTypeNotIn: List<String>,
        excludeEmpBranchName: String?,
        branchScopeCodes: List<String>,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>

    /**
     * 투입현황 대시보드용 월별 투입 row 조회 (Spec 850 — 환산인원 차트 베이스).
     *
     * 연월 + 지점 스코프(costCenterCode IN) 필터 후 거래처(account) left join. isDeleted 제외.
     * 집계(거래처유형별/근무유형별 환산인원 SUM)는 service 단 메모리에서 수행.
     *
     * entity/account 전 컬럼 적재 대신 집계가 쓰는 7개 필드만 [DashboardDeploymentRow] projection 으로
     * 가져와 select 페이로드를 축소한다 (account fetch join 의 80여 컬럼 회피).
     *
     * @param year             연도 (entity year 가 String — 문자열 비교)
     * @param month            월 (entity month 가 String)
     * @param costCenterCodes  지점 스코프 코스트센터 코드 목록. 빈 목록이면 전사 (전체 지점)
     */
    fun findDeploymentDashboardRows(
        year: String,
        month: String,
        costCenterCodes: List<String>,
    ): List<DashboardDeploymentRow>
}
