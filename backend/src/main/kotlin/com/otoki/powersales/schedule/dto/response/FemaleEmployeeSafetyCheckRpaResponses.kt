package com.otoki.powersales.schedule.dto.response

/**
 * 판매여사원 일일 안전점검 현황 (RPA용) 응답 (Spec #842).
 *
 * 레거시 매핑: SF Report `X00/new_report_xdB` (RPA용·scope=organization·24컬럼).
 * #841 영업지원실용 23컬럼과 동일하되 마지막 컬럼이 CommuteDate 대신 CUST_NAME(소유자명).
 * 전사 고정 조회 (지점 스코프 없음).
 */
data class FemaleEmployeeSafetyCheckRpaResponse(
    val date: String,
    val items: List<FemaleEmployeeSafetyCheckRpaItem>,
)

/**
 * 안전점검 현황 (RPA) 1행 — (여사원일정 1건) × employee × account × owner(User) 조인.
 *
 * 24컬럼: #841 23컬럼 + CUST_NAME. enum 필드는 `@JsonValue` 로 한글 displayName 직렬화.
 * custName = 레코드 Owner User 이름 (SF CUST_NAME 의사 컬럼). owner 부재 시 null.
 */
data class FemaleEmployeeSafetyCheckRpaItem(
    val employeeCode: String,
    val ladyName: String,
    val employeeOrgName: String?,
    val accountType: String?,
    val accountBranchCode: String?,
    val accountName: String?,
    val workingCategory1: String?,
    val checkTime: String?,
    val isWorkReport: String?,
    val hrCode: String?,
    val equipment1: String?,
    val equipment2: String?,
    val equipment3: String?,
    val equipment4: String?,
    val equipment5: String?,
    val equipment6: String?,
    val equipment7: String?,
    val equipment8: String?,
    val equipment9: String?,
    val precaution: String?,
    val precautionChk: Double?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val secondWorkType: String?,
    val custName: String?,
)
