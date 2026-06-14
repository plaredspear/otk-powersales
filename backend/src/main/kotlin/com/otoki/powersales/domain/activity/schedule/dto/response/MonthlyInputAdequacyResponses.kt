package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 월별 진열사원 투입적합성 조회 응답.
 *
 * 사용자가 입력한 연도의 1~12월 매트릭스. 각 행은 (사원, 거래처) 단위, 각 컬럼은 1~12월 적합성 라벨 (적합/경계/재검토/공백).
 */
data class MonthlyInputAdequacyResponse(
    val year: Int,
    val items: List<MonthlyInputAdequacyItem>
)

/**
 * 매트릭스 한 행 — (사원, 거래처) 단위.
 *
 * monthlySuitability 의 인덱스 0..11 이 1~12월에 대응. 빈 문자열은 일정/매출 데이터 없음을 의미.
 */
data class MonthlyInputAdequacyItem(
    val branchName: String,
    val workingCategory3: String?,
    val employeeName: String,
    val employeeCode: String,
    val title: String?,
    val accountCategory: String,
    val accountCategoryCode: String?,
    val accountName: String,
    val accountCode: String,
    val monthlySuitability: List<String>
)
