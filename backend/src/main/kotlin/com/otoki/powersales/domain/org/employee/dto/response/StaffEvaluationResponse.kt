package com.otoki.powersales.domain.org.employee.dto.response

/**
 * 여사원 평가조회 응답 DTO.
 *
 * ## 레거시 매핑
 * - 진입점: 레거시 `GET /employee/evaluationList` (+ `evaluationListAjax`) → `mypage/evaluationList.jsp`
 * - 데이터: `accountMapper.selectEvalList` (거래처별 목표/실적/달성률) + `accountMapper.selectBranchEval` (지점평가 점수)
 *
 * ## 데이터 source 정합 (목표/실적/달성률)
 * 레거시는 `monthlysaleshistory__c` 의 formula 캐시 컬럼(`lastmonthtargetfomula__c`/`TargetMonthResults__c`/
 * `lastmonthtargetachievedratio__c`) 을 직접 읽었으나, 신규 `MonthlySalesHistory` 엔티티에는 해당 캐시 컬럼이
 * 없다. 따라서 **월매출 현황 화면과 동일하게** 목표는 `SalesProgressRateMaster`(RT+FR+RM+FO 합), 실적은
 * `MonthlySalesHistory.ClosingAmountSum`(ABC합+Ship합), 달성률은 `round(실적/목표×100)` 으로 산출한다
 * (이미 수용된 진도율 deviation 과 동일 성격 — 값은 정합, 1:1 컬럼 복사는 아님).
 *
 * @property yearMonth 조회 연월 `YYYYMM`
 * @property branchScore 지점평가 점수 (SF `StaffReview.EmployeeTotalScore__c`). 평가 미생성 월은 null
 * @property branchMaxScore 지점평가 만점 (레거시 하드코딩 30)
 * @property accounts 담당 거래처별 평가 행 (그 달 근무한 거래처 中 월매출실적 row 존재분 — 레거시 INNER JOIN 정합)
 */
data class StaffEvaluationResponse(
    val yearMonth: String,
    val branchScore: Double?,
    val branchMaxScore: Int = BRANCH_MAX_SCORE,
    val accounts: List<AccountEvaluationInfo>,
) {
    /**
     * 거래처별 평가 행.
     *
     * @property accountCode 거래처 SAP 코드 (`Account.externalKey`)
     * @property accountName 거래처명
     * @property accountType 거래처유형 (`Account.Type` displayName). null 가능
     * @property targetAmount 목표 금액 (RT+FR+RM+FO 합)
     * @property performanceAmount 실적 금액 (ClosingAmountSum)
     * @property attainmentRate 달성률 % (`round(실적/목표×100)`, 목표 0 이면 0)
     */
    data class AccountEvaluationInfo(
        val accountCode: String,
        val accountName: String,
        val accountType: String?,
        val targetAmount: Long,
        val performanceAmount: Long,
        val attainmentRate: Double,
    )

    companion object {
        /** 지점평가 만점 — 레거시 `selectBranchEval` 의 `30 as target_pt` 하드코딩 정합. */
        const val BRANCH_MAX_SCORE: Int = 30
    }
}
