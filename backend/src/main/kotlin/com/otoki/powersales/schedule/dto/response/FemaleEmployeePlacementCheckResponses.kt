package com.otoki.powersales.schedule.dto.response

/**
 * 여사원 배치 점검 현황 응답 — 영업지원실용 월간 배치 점검 (퇴직자 포함 · 여사원/조장 · 상시/임시).
 *
 * 레거시 매핑: SF Report `InternalSalesReportFolder/new_report_4Ic`
 * (여사원 배치 점검 퇴직자 포함 (영업지원실 용) 상시_임시(조장포함)). Tabular 형식 — 일정 행 단위 나열.
 */
data class FemaleEmployeePlacementCheckResponse(
    val year: Int,
    val month: Int,
    val items: List<FemaleEmployeePlacementCheckItem>,
)

/**
 * 배치 점검 1행 — (여사원/조장) × (여사원일정 1건) 조인 결과.
 *
 * 21컬럼: 레거시 Report 컬럼 순서 보존. enum 필드는 `@JsonValue` 로 한글 displayName 직렬화.
 * `age` / `yearsOfService` 는 SF formula 필드 대체 (조회 시점 계산값).
 */
data class FemaleEmployeePlacementCheckItem(
    val workingDate: String?,
    val orgName: String?,
    val employeeCode: String,
    val jikwee: String?,
    val name: String,
    val professionalPromotionTeam: String?,
    val employmentStatus: String?,
    val accountType: String?,
    val accountName: String?,
    val accountBranchCode: String?,
    val accountBranchName: String?,
    val workingCategory1: String?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val secondWorkType: String?,
    val workingCategory5: String?,
    val commuteDate: String?,
    val isWorkReport: String?,
    val startDate: String?,
    val age: Int?,
    val yearsOfService: Int?,
)
