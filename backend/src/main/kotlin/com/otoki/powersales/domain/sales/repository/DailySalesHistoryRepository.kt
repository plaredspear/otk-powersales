package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesHistoryRepository : JpaRepository<DailySalesHistory, Long> {

    fun findByExternalKey(externalKey: String): DailySalesHistory?

    fun findByExternalKeyIn(externalKeys: List<String>): List<DailySalesHistory>

    /**
     * 거래처 코드 목록 + 매출월(`YYYYMM`) prefix 로 한 달치 일별 row 조회.
     *
     * ORORA 일별 적재 후 월별 합계 재합산(멱등 갱신)에서 거래처+월 단위 daily 전량 조회용.
     * `sales_date` 는 `YYYYMMDD` 8자 문자열이라 `YYYYMM` prefix 매칭으로 한 달치를 조회한다.
     */
    fun findBySapAccountCodeInAndSalesDateStartingWith(
        sapAccountCodes: Collection<String>,
        salesDatePrefix: String,
    ): List<DailySalesHistory>
}
