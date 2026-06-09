package com.otoki.powersales.sales.repository

import com.otoki.powersales.sales.entity.DailySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesHistoryRepository : JpaRepository<DailySalesHistory, Long> {

    fun findByExternalKey(externalKey: String): DailySalesHistory?

    fun findByExternalKeyIn(externalKeys: List<String>): List<DailySalesHistory>

    /**
     * 거래처 코드 + 매출월(`YYYYMM`) prefix 로 한 달치 일별 row 조회.
     *
     * ORORA 일별 적재 후 월별 합계 갱신(Spec #855 §2.3)에서 거래처+월 단위 daily 합산용.
     * `sales_date` 는 `YYYYMMDD` 8자 문자열이라 `YYYYMM` prefix 매칭으로 한 달치를 조회한다.
     */
    fun findBySapAccountCodeAndSalesDateStartingWith(
        sapAccountCode: String,
        salesDatePrefix: String,
    ): List<DailySalesHistory>
}
