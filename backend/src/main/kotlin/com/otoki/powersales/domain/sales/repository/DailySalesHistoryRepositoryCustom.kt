package com.otoki.powersales.domain.sales.repository

interface DailySalesHistoryRepositoryCustom {

    /**
     * 거래처 코드 목록 + 매출월(`YYYYMM`) 의 거래처별 일별 금액 합계 집계.
     *
     * ORORA 일별 적재 후 월별 합계 재합산(멱등 갱신)용 — entity 전량 로드 대신 DB `SUM` 집계로
     * 거래처당 1 row 만 반환한다 (일별 배치 소요시간 회귀 방지). `sales_date` 는 `YYYYMMDD` 8자
     * 문자열이라 `YYYYMM` prefix 매칭으로 한 달치를 집계한다. EntityManager 경유 JPQL 실행이라
     * 같은 트랜잭션에서 방금 saveAll 한 일별 적재분도 auto-flush 로 집계에 반영된다.
     */
    fun sumMonthlyBySapAccountCodeIn(
        sapAccountCodes: Collection<String>,
        salesMonth: String,
    ): List<DailySalesMonthlySum>
}

/**
 * [DailySalesHistoryRepositoryCustom.sumMonthlyBySapAccountCodeIn] 집계 결과.
 * 합계 필드는 그룹 내 대상 컬럼이 전부 null 이면 null (호출측 null→0 처리).
 */
data class DailySalesMonthlySum(
    val sapAccountCode: String,
    val erpSalesSum: Double?,
    val erpDistributionSum: Double?,
    val ledgerSum: Double?,
)
