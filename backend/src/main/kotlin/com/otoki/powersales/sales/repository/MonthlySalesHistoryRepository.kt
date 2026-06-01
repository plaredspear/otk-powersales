package com.otoki.powersales.sales.repository

import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 월매출 이력 Repository
 *
 * SF `MonthlySalesHistory__c` → RDS 복제 적재 대상 entity 의 기본 JPA Repository.
 * 적재 흐름의 기본 CRUD + 월별여사원 통합일정 화면의 6개월 평균 마감실적 조회를 제공한다.
 */
interface MonthlySalesHistoryRepository : JpaRepository<MonthlySalesHistory, Long> {

    /**
     * 다월 일괄 + 다중 거래처 조회 — 월별여사원 통합일정의 6개월 평균 ABC 마감실적 산출용.
     *
     * `(salesYear, salesMonth)` 쌍의 후보 집합을 한 번에 가져온다 (salesYear IN × salesMonth IN
     * 의 cartesian 후보). 정확한 (년, 월) 쌍 매칭 + `isDeleted` 필터는 호출 측
     * [com.otoki.powersales.sales.service.MonthlySalesHistoryQueryGateway] 가 담당한다 —
     * ORORA view 조회 (`YYYYMM` 문자열) 와 동등한 인터페이스를 게이트웨이가 제공하기 위함.
     */
    fun findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(
        salesYears: Collection<SalesYear>,
        salesMonths: Collection<SalesMonth>,
        sapAccountCodes: Collection<String>,
    ): List<MonthlySalesHistory>
}
