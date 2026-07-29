package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesHistoryRepository : JpaRepository<DailySalesHistory, Long>, DailySalesHistoryRepositoryCustom {

    fun findByExternalKey(externalKey: String): DailySalesHistory?

    fun findByExternalKeyIn(externalKeys: List<String>): List<DailySalesHistory>

    /**
     * 거래처코드 + 매출월(`yyyyMM`) 의 일별 매출 일람 (web admin "ORORA 일매출" 조회용).
     *
     * `sales_date` 는 `yyyyMMdd` 8자 문자열 컬럼이라 월 필터는 prefix 매칭으로 수행한다
     * ([DailySalesHistoryRepositoryCustomImpl] 의 월 합계 집계와 동일 방식). 조인 키를 `account_id`
     * FK 가 아니라 `sap_account_code` 텍스트로 두는 이유는, SF 이관분 중 FK 미해소 row 가 있어도
     * 원본 거래처코드 기준으로는 빠짐없이 보이게 하기 위함이다 (적재 원본 확인 화면의 목적).
     */
    fun findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc(
        sapAccountCode: String,
        salesDate: String,
    ): List<DailySalesHistory>
}
