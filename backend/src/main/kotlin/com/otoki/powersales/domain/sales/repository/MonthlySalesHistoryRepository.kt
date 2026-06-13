package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
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
     * [com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway] 가 담당한다 —
     * ORORA view 조회 (`YYYYMM` 문자열) 와 동등한 인터페이스를 게이트웨이가 제공하기 위함.
     */
    fun findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(
        salesYears: Collection<SalesYear>,
        salesMonths: Collection<SalesMonth>,
        sapAccountCodes: Collection<String>,
    ): List<MonthlySalesHistory>

    /**
     * 다월 일괄 + 다중 거래처(account_id) 조회 — 배치적합성 6개월 평균 마감실적 산출용.
     *
     * SF `SalesComparisonSearchController` (cls:289-294) 의 `WHERE AccountId__c IN :accountIds2`
     * 정합 — `MonthlySalesHistory__c.AccountId__c` (Account Id) 기준 조회로, `AccountId__c` 가
     * null 인 row (SF `deleteConstraint=SetNull` 로 Account 삭제 시 잔존하는 매출 행) 는 IN 절에서
     * 자연 배제된다. `sapAccountCode` 기준 조회는 이 null-AccountId row 를 SAP 코드로 끌어와
     * SF 와 평균 모수가 달라지므로 배치적합성 경로는 account_id 기준을 쓴다.
     */
    fun findBySalesYearInAndSalesMonthInAndAccountIdIn(
        salesYears: Collection<SalesYear>,
        salesMonths: Collection<SalesMonth>,
        accountIds: Collection<Long>,
    ): List<MonthlySalesHistory>
}
