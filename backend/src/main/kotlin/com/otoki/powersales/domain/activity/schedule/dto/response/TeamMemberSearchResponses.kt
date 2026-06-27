package com.otoki.powersales.domain.activity.schedule.dto.response

import java.math.BigDecimal

/**
 * 팀멤버 카테고리 검색 결과 (SF `CategorySearchByTeamMemberController.ResultItem`).
 */
data class TeamMemberCategorySearchResult(
    val resultCode: String,
    val resultMsg: String? = null,
    val result: List<TeamMemberCategoryResultItem>,
)

/**
 * 팀멤버 카테고리 검색 row (SF `CategorySearchByTeamMemberController.ResultItems`).
 *
 * SF `setNull()` (cls:341-363) 정합: 현월/전월 모두 0 인 경우 모든 수치 필드는 null.
 */
data class TeamMemberCategoryResultItem(
    val branchName: String,
    val fix: BigDecimal? = null,
    val store: BigDecimal? = null,
    val rotate: BigDecimal? = null,
    val currentExhibitionTotal: BigDecimal? = null,
    val lastExhibitionTotal: BigDecimal? = null,
    val exhibitionIncrease: BigDecimal? = null,
    /** 상온 = 상온(라면 제외) + 라면. */
    val roomTemperature: BigDecimal? = null,
    /** 냉동/냉장 = 냉동 + 냉장 + 만두 + 냉동/냉장. */
    val refrigerationAndFreezing: BigDecimal? = null,
    val currentEventTotal: BigDecimal? = null,
    val lastEventTotal: BigDecimal? = null,
    val eventIncrease: BigDecimal? = null,
    val currentMonthTotal: BigDecimal? = null,
    val lastMonthTotal: BigDecimal? = null,
    val totalIncrease: BigDecimal? = null,
)

/**
 * 팀멤버 일정 검색 결과 (SF `ScheduleSearchByTeamMemberController.ResultItem`).
 */
data class TeamMemberScheduleSearchResult(
    val resultCode: String,
    val resultMsg: String? = null,
    val result: List<TeamMemberScheduleResultItem>,
)

/**
 * 팀멤버 일정 검색 row (SF `ScheduleSearchByTeamMemberController.ResultItems`).
 *
 * SF formula 필드 (BranchName/EmployeeNumber/Title/AccountBranchName/AccountCode) 는
 * backend MFEIS lazy join 으로 환원 (D4=a).
 *
 * ABC 마감실적 (actualAmount) = 6개월 평균 `ClosingAmountSum` = `(abc1+abc2+abc3+abc4) + (ship1+ship2+ship3+ship4)` (D3=a).
 */
data class TeamMemberScheduleResultItem(
    val year: String?,
    val month: String?,
    val name: String?,
    /** 거래처 지점명. SF formula `Account__r.BranchName__c`. */
    val accountBranchName: String?,
    /** 거래처명. SF `Account__r.Name`. */
    val accountName: String?,
    /** 거래처 코드. SF formula `Account__r.ExternalKey__c`. */
    val accountCode: String?,
    /** 유통형태 — 거래처상태코드 + 거래처유형명 조합 (예: "02 슈퍼"). */
    val distributionChannelLabel: String?,
    /** 거래처유형 — ABC유형코드 + ABC유형 조합 (예: "6111 이마트"). */
    val abcTypeLabel: String?,
    /** 사원 조직명. SF formula `FullName__r.DKRetail__OrgName__c`. */
    val orgName: String?,
    /** 사번. SF formula `FullName__r.DKRetail__EmpCode__c`. */
    val employeeNumber: String?,
    /** 직위. SF formula `FullName__r.DKRetail__Jikwee__c`. */
    val title: String?,
    /** 사원명. SF `FullName__r.Name`. */
    val employeeName: String?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    val numberOfInputs: BigDecimal?,
    val equivalentNumberOfWorkingDays: BigDecimal,
    val convertedHeadcount: BigDecimal,
    /** 6개월 평균 ABC 마감실적. */
    val actualAmount: BigDecimal,
)
