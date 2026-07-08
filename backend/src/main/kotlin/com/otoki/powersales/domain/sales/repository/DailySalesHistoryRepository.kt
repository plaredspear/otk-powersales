package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DailySalesHistoryRepository : JpaRepository<DailySalesHistory, Long> {

    fun findByExternalKey(externalKey: String): DailySalesHistory?

    fun findByExternalKeyIn(externalKeys: List<String>): List<DailySalesHistory>

    /**
     * 거래처 코드 목록 + 매출월(`YYYYMM`) 의 거래처별 일별 금액 합계 집계.
     *
     * ORORA 일별 적재 후 월별 합계 재합산(멱등 갱신)용 — entity 전량 로드 대신 DB `SUM` projection 으로
     * 거래처당 1 row 만 반환한다 (일별 배치 소요시간 회귀 방지). `sales_date` 는 `YYYYMMDD` 8자
     * 문자열이라 `YYYYMM` prefix 매칭으로 한 달치를 집계한다. JPQL 이라 같은 트랜잭션에서 방금
     * saveAll 한 일별 적재분도 auto-flush 로 집계에 반영된다.
     */
    @Query(
        """
        select d.sapAccountCode as sapAccountCode,
               sum(d.erpSalesAmount) as erpSalesSum,
               sum(d.erpDistributionAmount) as erpDistributionSum,
               sum(d.ledgerAmount) as ledgerSum
        from DailySalesHistory d
        where d.sapAccountCode in :sapAccountCodes
          and d.salesDate like concat(:salesMonth, '%')
        group by d.sapAccountCode
        """
    )
    fun sumMonthlyBySapAccountCodeIn(
        @Param("sapAccountCodes") sapAccountCodes: Collection<String>,
        @Param("salesMonth") salesMonth: String,
    ): List<DailySalesMonthlySumRow>
}

/**
 * [DailySalesHistoryRepository.sumMonthlyBySapAccountCodeIn] 집계 결과 projection.
 * alias(`sapAccountCode`/`erpSalesSum`/`erpDistributionSum`/`ledgerSum`) ↔ getter 매핑.
 * 합계 getter 는 그룹 내 대상 컬럼이 전부 null 이면 null (호출측 null→0 처리).
 */
interface DailySalesMonthlySumRow {
    fun getSapAccountCode(): String
    fun getErpSalesSum(): Double?
    fun getErpDistributionSum(): Double?
    fun getLedgerSum(): Double?
}
