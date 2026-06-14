package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 여사원 근무내역 (개인별 조회) 응답 — 영업지원실용 보고서 (Spec #840).
 *
 * 레거시 매핑: SF Report `InternalSalesReportFolder/new_report_nEX` (여사원 근무내역).
 * 특정 사번 1명의 월간 여사원일정 행을 일자별로 나열 (Tabular, 15컬럼).
 */
data class FemaleEmployeeWorkHistoryResponse(
    val employeeCode: String,
    val year: Int,
    val month: Int,
    val items: List<FemaleEmployeeWorkHistoryItem>,
)

/**
 * 근무내역 1행 — (여사원) × (여사원일정 1건) 조인 결과.
 *
 * 15컬럼: 레거시 Report 컬럼 순서 보존. enum 필드는 `@JsonValue` 로 한글 displayName 직렬화.
 * `age` 는 SF formula 필드 대체 (조회 시점 계산값).
 */
data class FemaleEmployeeWorkHistoryItem(
    val scheduleName: String?,
    val name: String,
    val employeeCode: String,
    val age: Int?,
    val workingDate: String?,
    val accountBranchName: String?,
    val accountBranchCode: String?,
    val accountName: String?,
    val workingType: String?,
    val workingCategory1: String?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val secondWorkType: String?,
    val isWorkReport: String?,
    val commuteDate: String?,
)
