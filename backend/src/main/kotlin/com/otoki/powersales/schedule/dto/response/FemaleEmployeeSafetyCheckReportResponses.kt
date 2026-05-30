package com.otoki.powersales.schedule.dto.response

/**
 * 판매여사원 일일 안전점검 현황 응답 — 영업지원실/지점용 보고서 (Spec #841).
 *
 * 레거시 매핑: SF Report `new_report_wce` (영업지원실용) + `new_report_oJO` (지점별-어제).
 * 어제자(또는 지정 일자) 안전점검 완료 건을 일정 단위로 나열 (Tabular, 24컬럼).
 */
data class FemaleEmployeeSafetyCheckReportResponse(
    val date: String,
    val items: List<FemaleEmployeeSafetyCheckReportItem>,
)

/**
 * 안전점검 현황 1행 — (여사원일정 1건) × employee × account 조인.
 *
 * 24컬럼: 레거시 두 변형의 합집합 (CommuteDate 포함). enum 필드는 `@JsonValue` 로 한글 displayName 직렬화.
 * checkTime 은 startTime 직접 (레거시 `StartTime - 9/24` 의 KST 보정은 신규 KST 저장이라 불요).
 */
data class FemaleEmployeeSafetyCheckReportItem(
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
    val commuteDate: String?,
)
