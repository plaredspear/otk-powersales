package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule

/**
 * MFEIS Querydsl 확장 — 거래처유형별 환산인원 현황 보고서 조회 (Spec #847).
 */
interface MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustom {

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
     *
     * 전사 스코프 (DataScope 미적용). isDeleted 제외. account fetch join.
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
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>

    /**
     * 투입현황 대시보드용 월별 투입 row 조회 (Spec 850 — 환산인원 차트 베이스).
     *
     * 연월 + 지점 스코프(costCenterCode IN) 필터 후 account fetch join. isDeleted 제외.
     * 집계(거래처유형별/근무유형별 환산인원 SUM)는 service 단 메모리에서 수행.
     *
     * @param year             연도 (entity year 가 String — 문자열 비교)
     * @param month            월 (entity month 가 String)
     * @param costCenterCodes  지점 스코프 코스트센터 코드 목록. 빈 목록이면 전사 (전체 지점)
     */
    fun findDeploymentDashboardRows(
        year: String,
        month: String,
        costCenterCodes: List<String>,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule>
}
