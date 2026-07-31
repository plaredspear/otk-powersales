package com.otoki.powersales.domain.activity.schedule.dto.response

import java.math.BigDecimal

/**
 * 기간별 근무내역(개인) — 특정 여사원 1명의 월별 근무 집계 행 (표의 1단계).
 *
 * 좌측 패널에서 여사원을 선택하면 선택한 기간(시작년월~종료년월) 내 근무 행을 **년월 단위로 먼저**
 * 그룹핑하고, 각 월 안에서 거래처(account) 별로 다시 나눈다([accounts], 행 펼침).
 * 모수는 출근 등록된 근무 행이므로 모든 행이 거래처를 가진다 — 연차는 거래처가 없어 이 표가 아니라
 * 사원 단위 요약([WorkHistoryEmployeeAccountResponse.annualLeaveDays]) 으로 제공한다.
 */
data class WorkHistoryMonthStat(
    /** 대상 년월 (yyyy-MM). */
    val yearMonth: String,
    /** 이 월에 근무한 거래처 수 ([accounts] 크기). */
    val accountCount: Int,
    /** 총 근무일수 (출근 등록된 일정 행 수). */
    val totalWorkingDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 진열. */
    val displayDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 행사. */
    val eventDays: Int,
    /** 구분(WorkingType)별 일수 — 근무. */
    val workDays: Int,
    /** 총 투입횟수 — 이 월 거래처별 투입횟수의 합 (통합일정(MFEIS) 정의 동등). */
    val totalInputCount: Int,
    /** 총 환산근무일수 — 이 월 거래처별 환산근무일수의 합. scale 4 HALF_UP. */
    val equivalentWorkingDays: BigDecimal,
    /**
     * 이 월의 거래처별 분해 (총 근무일수 내림차순 → 거래처명 오름차순).
     * 환산인원(convertedHeadcount)·근무형태 대표값은 월 단위로만 정의되므로 여기에만 담긴다.
     */
    val accounts: List<WorkHistoryAccountStat>,
)

/**
 * 기간별 근무내역(개인) — 월 안의 거래처별 근무 집계 행 (표의 2단계).
 *
 * 통합일정(MFEIS) 의 월 단위 집계를 거래처별로 재현한 것으로, 거래처 속성(지점명/유통형태/거래처유형)과
 * B그룹 지표(투입횟수/환산근무일수/환산인원/근무형태)를 함께 제공한다.
 */
data class WorkHistoryAccountStat(
    /** 거래처명 (Account.name). */
    val accountName: String?,
    /** 거래처 코드 (Account.externalKey). */
    val accountExternalKey: String?,
    /** 거래처 지점명 (Account.branchName). */
    val accountBranchName: String?,
    /** 유통형태 — 거래처유형마스터 "{거래처유형코드} {이름}" (예: "06 슈퍼"). AccountCategoryLookup 정본. */
    val distributionChannelLabel: String?,
    /** 거래처유형 (Account.abcTypeLabel — ABC유형코드 + ABC유형). */
    val abcTypeLabel: String?,
    /** 이 거래처의 해당 월 근무일수 (출근 등록된 일정 행 수). */
    val totalWorkingDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 진열. */
    val displayDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 행사. */
    val eventDays: Int,
    /** 구분(WorkingType)별 일수 — 근무. */
    val workDays: Int,
    /** 총 투입횟수 — 이 거래처+월 의 (근무유형 조합)별 distinct 근무일 수 합. */
    val totalInputCount: Int = 0,
    /** 환산근무일수 — 이 거래처+월 의 Σ(1/N), N = 그날 사원의 (거래처 무관) 출근 row 수. scale 4 HALF_UP. */
    val equivalentWorkingDays: BigDecimal = BigDecimal.ZERO,
    /**
     * 환산인원 — 환산근무일수(미반올림) ÷ 당월근무일수(사원+costCenter distinct 근무일). scale 4 HALF_UP.
     * 당월근무일수 0 이면 0. 분모가 월마다 달라 기간 합산이 불가하므로 월 안에서만 의미를 갖는다.
     */
    val convertedHeadcount: BigDecimal = BigDecimal.ZERO,
    /** 근무형태1 대표값 (이 거래처+월 최다 조합의 WorkingCategory1). null 가능. */
    val workingCategory1: String? = null,
    /** 근무형태3 대표값. null 가능. */
    val workingCategory3: String? = null,
    /** 근무형태4 대표값 (TMS.secondWorkType). null 가능. */
    val workingCategory4: String? = null,
    /** 근무형태5 대표값 (WorkingCategory5). null 가능. */
    val workingCategory5: String? = null,
)

data class WorkHistoryEmployeeAccountResponse(
    val fromYearMonth: String,
    val toYearMonth: String,
    /** 조회 대상 여사원 사번. */
    val employeeCode: String,
    /** 조회 대상 여사원 이름 (기간 내 근무 행이 없으면 null). */
    val employeeName: String?,
    /** 년월 오름차순 월별 집계. 근무 행이 있는 월만 담긴다 (근무 없는 월은 행 자체가 없음). */
    val months: List<WorkHistoryMonthStat>,
    /** 월 수 ([months] 크기). */
    val monthCount: Int,
    /** 기간 전체의 distinct 거래처 수 (월이 달라도 같은 거래처는 1개로 센다). */
    val totalCount: Int,
    /**
     * 기간 내 연차 일수 (사원 단위 합계, distinct 근무일).
     *
     * 연차는 거래처가 없어 월/거래처 표([months])에 담을 수 없으므로 여기에 별도로 제공한다.
     */
    val annualLeaveDays: Int = 0,
)
