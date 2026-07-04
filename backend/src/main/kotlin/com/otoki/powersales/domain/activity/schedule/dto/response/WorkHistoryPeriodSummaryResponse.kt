package com.otoki.powersales.domain.activity.schedule.dto.response

import java.math.BigDecimal

/**
 * 기간별 근무내역(개인) — 여사원별 근무 집계 항목.
 *
 * 근무기간 조회(월별근무내역 목록)와 동일하게 [TeamMemberSchedule] 을 원천으로 하되,
 * 단일 월이 아닌 기간(시작년월~종료년월) 전체를 여사원별로 집계해 1행으로 제공한다.
 * 통합일정(월별여사원 통합일정)과 유사한 "여사원별 1행" 레이아웃이나, 집계 원천이 MFEIS 가 아닌
 * TeamMemberSchedule 일자별 실적이라는 점이 다르다.
 */
data class WorkHistoryPeriodSummaryItem(
    /** 소속지점명 (Employee.orgName). */
    val orgName: String?,
    /** 사번 (Employee.employeeCode). */
    val employeeCode: String?,
    /** 이름 (Employee.name). */
    val employeeName: String?,
    /** 직위 (Employee.jikwee). */
    val title: String?,
    /** 총 근무일수 (출근 등록된 일정 행 수). */
    val totalWorkingDays: Int,
    /** 근무 거래처 수 (distinct account). */
    val workingAccountCount: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 진열. */
    val displayDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 행사. */
    val eventDays: Int,
    /** 구분(WorkingType)별 일수 — 근무. */
    val workDays: Int,
    /** 구분(WorkingType)별 일수 — 연차. */
    val annualLeaveDays: Int,
    /** 구분(WorkingType)별 일수 — 대휴. */
    val altHolidayDays: Int,
    /**
     * 월별 통계 분해 (yyyy-MM 오름차순). 합계 행을 펼치면 표시.
     * 조회 기간이 단일 월이면 빈 리스트 (펼칠 분해가 없음).
     */
    val monthlyBreakdown: List<WorkHistoryMonthlyStat> = emptyList(),
)

/**
 * 여사원별 월 단위 근무 통계 — [WorkHistoryPeriodSummaryItem] 의 월별 분해 행.
 * 컬럼은 합계 항목과 동일하며 `yearMonth` 로 어느 달인지 구분한다.
 */
data class WorkHistoryMonthlyStat(
    /** 대상 년월 (yyyy-MM). */
    val yearMonth: String,
    val totalWorkingDays: Int,
    val workingAccountCount: Int,
    val displayDays: Int,
    val eventDays: Int,
    val workDays: Int,
    val annualLeaveDays: Int,
    val altHolidayDays: Int,
)

data class WorkHistoryPeriodSummaryResponse(
    val fromYearMonth: String,
    val toYearMonth: String,
    val items: List<WorkHistoryPeriodSummaryItem>,
    val totalCount: Int,
)

/**
 * 기간별 근무내역(개인) — 특정 여사원 1명의 거래처별 근무 집계 행.
 *
 * 좌측 패널에서 여사원을 선택하면 선택한 기간(시작년월~종료년월) 내 근무 행을
 * 거래처(account) 단위로 그룹핑해 제공한다. 거래처 미연결 행(연차/대휴 등)은
 * accountName=null 1행으로 묶는다.
 */
data class WorkHistoryAccountStat(
    /** 거래처명 (Account.name). 거래처 미연결 행 묶음이면 null. */
    val accountName: String?,
    /** 거래처 코드 (Account.externalKey). 거래처 미연결이면 null. */
    val accountExternalKey: String?,
    /** 거래처 지점명 (Account.branchName). 거래처 미연결이면 null. */
    val accountBranchName: String?,
    /** 유통형태 (Account.distributionChannelLabel — 거래처상태코드 + 거래처유형). 미연결이면 null. */
    val distributionChannelLabel: String?,
    /** 거래처유형 (Account.abcTypeLabel — ABC유형코드 + ABC유형). 미연결이면 null. */
    val abcTypeLabel: String?,
    /** 총 근무일수 (출근 등록된 일정 행 수). */
    val totalWorkingDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 진열. */
    val displayDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 행사. */
    val eventDays: Int,
    /** 구분(WorkingType)별 일수 — 근무. */
    val workDays: Int,
    /** 구분(WorkingType)별 일수 — 연차. */
    val annualLeaveDays: Int,
    /** 구분(WorkingType)별 일수 — 대휴. */
    val altHolidayDays: Int,
    /**
     * 총 투입횟수 — 통합일정(MFEIS) 정의 동등. 이 거래처 내 (근무유형 조합)별 distinct 근무일 수의 합.
     * 거래처 미연결 행은 0.
     */
    val totalInputCount: Int = 0,
    /**
     * 총 환산근무일수 — 통합일정(MFEIS) 정의 동등. Σ(1/N), N = 그날 사원의 (거래처 무관) 출근 row 수.
     * 기간 조회면 각 월의 환산근무일수를 합산한다 (환산근무일수는 합산 가능). scale 4 HALF_UP.
     * 거래처 미연결 행은 0.
     */
    val equivalentWorkingDays: BigDecimal = BigDecimal.ZERO,
    /**
     * 월별 통계 분해 (yyyy-MM 오름차순). 행을 펼치면 표시.
     * 환산인원(convertedHeadcount)은 분모(당월근무일수)가 월마다 달라 기간 합산이 불가하므로
     * 이 월별 분해에만 담는다. 근무형태1/3/4/5 대표값도 월별로 제공.
     * 단일 월 조회면 빈 리스트 (펼칠 분해가 없음).
     */
    val monthlyStats: List<WorkHistoryAccountMonthlyStat> = emptyList(),
)

/**
 * 기간별 근무내역(개인) — 거래처별 행의 월별 분해 (통합일정 B그룹 지표).
 *
 * 통합일정(MFEIS) 의 월 단위 집계를 거래처별 뷰 안에서 재현한 것으로,
 * 환산인원(월 단위로만 정의)과 근무형태 대표값을 월별로 제공한다.
 */
data class WorkHistoryAccountMonthlyStat(
    /** 대상 년월 (yyyy-MM). */
    val yearMonth: String,
    /** 이 거래처의 해당 월 근무일수 (출근 등록 행 수). */
    val totalWorkingDays: Int,
    /** 총 투입횟수 — 이 거래처+월 의 (근무유형 조합)별 distinct 근무일 수 합. */
    val totalInputCount: Int,
    /** 환산근무일수 — 이 거래처+월 의 Σ(1/N). scale 4 HALF_UP. */
    val equivalentWorkingDays: BigDecimal,
    /**
     * 환산인원 — 환산근무일수(미반올림) ÷ 당월근무일수(사원+costCenter distinct 근무일). scale 4 HALF_UP.
     * 당월근무일수 0 이면 0.
     */
    val convertedHeadcount: BigDecimal,
    /** 근무형태1 대표값 (이 거래처+월 최다 조합의 WorkingCategory1). null 가능. */
    val workingCategory1: String?,
    /** 근무형태3 대표값. null 가능. */
    val workingCategory3: String?,
    /** 근무형태4 대표값 (TMS.secondWorkType). null 가능. */
    val workingCategory4: String?,
    /** 근무형태5 대표값 (WorkingCategory5). null 가능. */
    val workingCategory5: String?,
)

data class WorkHistoryEmployeeAccountResponse(
    val fromYearMonth: String,
    val toYearMonth: String,
    /** 조회 대상 여사원 사번. */
    val employeeCode: String,
    /** 조회 대상 여사원 이름 (기간 내 근무 행이 없으면 null). */
    val employeeName: String?,
    val items: List<WorkHistoryAccountStat>,
    val totalCount: Int,
)
