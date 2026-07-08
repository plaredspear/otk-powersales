package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.domain.sales.repository.MonthlySalesHistoryRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * RDS `MonthlySalesHistory` (SF `MonthlySalesHistory__c` 복제 적재) 기반 월별 마감실적 조회 게이트웨이.
 *
 * ## 책임
 * - 월별여사원 통합일정 화면 / refresh / batch 의 6개월 평균 ABC 마감실적 산출 source 를
 *   ORORA view (`OroraMonthlySalesHistory`) → RDS `MonthlySalesHistory` 로 일원화.
 * - ORORA 게이트웨이 ([OroraMonthlySalesHistoryQueryGateway]) 와 동등한 인터페이스 (`YYYYMM`
 *   문자열 리스트 + 거래처 코드) 를 제공해 호출 측 변경을 최소화.
 *
 * ## 마감실적 합계 산출 (SF `ClosingAmountSum__c` formula 동등)
 * SF formula (`ABCClosingSumAmount__c + ShipClosingSumAmount__c`) 와 동등하게 **원본 합계 컬럼**
 * (`abcClosingSumAmount` / `shipClosingSumAmount`) 을 더한다. 개별 카테고리 컬럼(abc1~4 / ship1~4)
 * 재합산은 합계 컬럼과 항상 같지 않아 (운영 데이터에 따라 개별 컬럼이 비고 합계 컬럼에만 적재된
 * 거래처/월 존재) 물류매출을 누락시키므로 사용하지 않는다 — 상세는 [closingAmountSum] 참조.
 * SF formula 의 `formulaTreatBlanksAs=BlankAsZero` 정합 — null 컬럼은 `ZERO` 치환.
 *
 * ## ORORA 게이트웨이와의 차이
 * - RDS 는 메인 DataSource 이므로 ORORA 게이트웨이의 graceful fallback (VPN 장애 흡수) 이 불필요.
 * - 매출월 매칭 키: ORORA 는 `SalesDate` (`YYYYMM` 문자열), RDS 는 `salesYear` + `salesMonth`
 *   picklist enum 조합. 본 게이트웨이가 `YYYYMM` → (`SalesYear`, `SalesMonth`) 변환을 흡수.
 * - SF `IsDeleted` soft-delete row 는 제외 (`isDeleted == true` 필터).
 */
@Component
class MonthlySalesHistoryQueryGateway(
    private val repository: MonthlySalesHistoryRepository,
) {

    /**
     * 다월 일괄 조회 — `YYYYMM` 문자열 리스트 + 거래처 코드로 월별 마감실적 row 를 반환.
     *
     * 거래처 코드 또는 매출월 collection 이 비어 있으면 빈 결과 즉시 반환.
     * Repository 가 (salesYear IN × salesMonth IN) cartesian 후보를 가져오므로, 요청한 정확한
     * (년, 월) 쌍 집합에 속하는 row 만 남기고 `isDeleted == true` 는 제외한다.
     */
    fun findBySalesDates(
        salesDates: Collection<String>,
        sapAccountCodes: Collection<String>,
    ): List<MonthlySalesRow> {
        if (sapAccountCodes.isEmpty() || salesDates.isEmpty()) return emptyList()

        val requestedYearMonths: Set<Pair<SalesYear, SalesMonth>> = salesDates
            .mapNotNull { toYearMonthPair(it) }
            .toSet()
        if (requestedYearMonths.isEmpty()) return emptyList()

        val salesYears = requestedYearMonths.map { it.first }.distinct()
        val salesMonths = requestedYearMonths.map { it.second }.distinct()

        return repository
            .findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(salesYears, salesMonths, sapAccountCodes)
            .toMonthlySalesRows(requestedYearMonths)
    }

    /**
     * 다월 일괄 + 다중 거래처(account_id) 조회 — 배치적합성 6개월 평균 마감실적 산출용.
     *
     * SF `SalesComparisonSearchController` (cls:289-294) 의 `WHERE AccountId__c IN :accountIds2`
     * 정합. 반환 row 의 [MonthlySalesRow.accountId] 가 채워지므로 호출 측은 account_id 로 직접 집계한다
     * (sapAccountCode 기준 집계와 달리 `account_id` 가 null 인 row 는 IN 절에서 자연 배제).
     */
    fun findBySalesDatesByAccountId(
        salesDates: Collection<String>,
        accountIds: Collection<Long>,
    ): List<MonthlySalesRow> {
        if (accountIds.isEmpty() || salesDates.isEmpty()) return emptyList()

        val requestedYearMonths: Set<Pair<SalesYear, SalesMonth>> = salesDates
            .mapNotNull { toYearMonthPair(it) }
            .toSet()
        if (requestedYearMonths.isEmpty()) return emptyList()

        val salesYears = requestedYearMonths.map { it.first }.distinct()
        val salesMonths = requestedYearMonths.map { it.second }.distinct()

        return repository
            .findBySalesYearInAndSalesMonthInAndAccountIdIn(
                salesYears,
                salesMonths,
                accountIds,
            )
            .toMonthlySalesRows(requestedYearMonths)
    }

    /**
     * Repository cartesian 후보 → 정확한 (년, 월) 쌍 매칭 + `isDeleted` 필터 + [MonthlySalesRow] 변환.
     * `findBySalesDates` / `findBySalesDatesByAccountId` 공통.
     */
    private fun List<MonthlySalesHistory>.toMonthlySalesRows(
        requestedYearMonths: Set<Pair<SalesYear, SalesMonth>>,
    ): List<MonthlySalesRow> =
        asSequence()
            .filter { it.isDeleted != true }
            .filter { row ->
                val year = row.salesYear ?: return@filter false
                val month = row.salesMonth ?: return@filter false
                (year to month) in requestedYearMonths
            }
            .mapNotNull { row ->
                // account_id 조회 경로(findBySalesDatesByAccountId)의 row 는 sap_account_code 가 비어
                // 있을 수 있다 (레거시는 Account 관계 formula `Account_ExternalKey__c` 로 조인하고, 신규는
                // FK `account_id` 로 조인하므로 별도 텍스트 컬럼 `SAPAccountCode__c` 적재 여부와 무관해야 함).
                // 문자열 조회 경로(findBySalesDates)의 row 는 조회 키 자체가 sap_account_code 라 항상 채워져
                // 있어 "" fallback 은 발생하지 않는다.
                val sapCode = row.sapAccountCode ?: ""
                MonthlySalesRow(
                    sapAccountCode = sapCode,
                    salesDate = salesDateOf(row),
                    closingAmountSum = closingAmountSum(row),
                    accountId = row.account?.id?.toLong(),
                    abcClosingAmount1 = row.abcClosingAmount1?.let { BigDecimal.valueOf(it) },
                    abcClosingAmount2 = row.abcClosingAmount2?.let { BigDecimal.valueOf(it) },
                    abcClosingAmount3 = row.abcClosingAmount3?.let { BigDecimal.valueOf(it) },
                    abcClosingAmount4 = row.abcClosingAmount4?.let { BigDecimal.valueOf(it) },
                    shipClosingAmount1 = row.shipClosingAmount1?.let { BigDecimal.valueOf(it) },
                    shipClosingAmount2 = row.shipClosingAmount2?.let { BigDecimal.valueOf(it) },
                    shipClosingAmount3 = row.shipClosingAmount3?.let { BigDecimal.valueOf(it) },
                    shipClosingAmount4 = row.shipClosingAmount4?.let { BigDecimal.valueOf(it) },
                )
            }
            .toList()

    /**
     * SF `ClosingAmountSum__c` formula 동등 — `ABCClosingSumAmount__c + ShipClosingSumAmount__c`.
     * null 컬럼은 `BlankAsZero` 정합으로 `ZERO` 치환.
     *
     * SF formula (field-meta `ClosingAmountSum__c`) 는 개별 카테고리 컬럼(abc1~4 / ship1~4) 이 아니라
     * **원본 합계 컬럼** (`ABCClosingSumAmount__c` / `ShipClosingSumAmount__c`) 을 더한다. 두 값은
     * 항상 같지 않다 — 운영 데이터에 따라 개별 컬럼이 비어 있고 합계 컬럼에만 물류매출이 적재된
     * 거래처/월이 있어, 개별 재합산은 물류매출을 누락시킨다 (실측: 거래처 1000077 의 2024-08·09·10
     * 은 ship1~4=0 이지만 ship_closing_sum_amount 에 물류매출 존재 → 6개월 평균이 SF 65.45M ≠ 70.02M
     * 으로 어긋남). SF 화면값 동등을 위해 원본 합계 컬럼을 그대로 사용한다.
     */
    private fun closingAmountSum(row: MonthlySalesHistory): BigDecimal {
        val abcSum = row.abcClosingSumAmount ?: 0.0
        val shipSum = row.shipClosingSumAmount ?: 0.0
        return BigDecimal.valueOf(abcSum + shipSum)
    }

    /**
     * row 의 (`salesYear`, `salesMonth`) picklist enum → `YYYYMM` 문자열 복원.
     * 호출 측에서 조회월/전년월 구분 키로 사용. enum value 가 `YYYY` + `MM` 형식이라 단순 연결.
     */
    private fun salesDateOf(row: MonthlySalesHistory): String =
        (row.salesYear?.value ?: "") + (row.salesMonth?.value ?: "")

    /**
     * `YYYYMM` 6자 문자열 → (`SalesYear`, `SalesMonth`) 변환. picklist enum 범위 밖이면 null.
     */
    private fun toYearMonthPair(yyyymm: String): Pair<SalesYear, SalesMonth>? {
        if (yyyymm.length != 6) return null
        val year = SalesYear.fromValueOrNull(yyyymm.substring(0, 4)) ?: return null
        val month = SalesMonth.fromValueOrNull(yyyymm.substring(4, 6)) ?: return null
        return year to month
    }
}

/**
 * 월별 마감실적 조회 결과 row — 화면/계산에 필요한 컬럼만 보유.
 *
 * @property sapAccountCode 거래처 SAP 코드
 * @property salesDate 매출 연월 `YYYYMM` (조회월/전년월 구분 키). 물류매출 화면에서 사용
 * @property closingAmountSum SF `ClosingAmountSum__c` formula 동등 합계 (ABC합 + Ship합)
 * @property abcClosingAmount1 전산마감실적_상온 (refresh/batch 의 양수 필터 평균 산출용 + 카테고리별 합산). null 가능
 * @property abcClosingAmount2 전산마감실적_라면 (카테고리별 합산용). null 가능
 * @property abcClosingAmount3 전산마감실적_냉장냉동 (카테고리별 합산용). null 가능
 * @property abcClosingAmount4 전산마감실적_유지 (카테고리별 합산용). null 가능
 * @property shipClosingAmount1 물류마감실적_상온 (물류매출 온도대별 표시 + 카테고리별 합산용). null 가능
 * @property shipClosingAmount2 물류마감실적_라면 (물류매출 온도대별 표시 + 카테고리별 합산용). null 가능
 * @property shipClosingAmount3 물류마감실적_냉장냉동 (물류매출 온도대별 표시 + 카테고리별 합산용). null 가능
 * @property shipClosingAmount4 물류마감실적_유지 (카테고리별 합산용). null 가능
 */
data class MonthlySalesRow(
    val sapAccountCode: String,
    val salesDate: String,
    val closingAmountSum: BigDecimal,
    /** Account FK (`account_id`). account_id 기준 조회 시에만 채워짐 — sapAccountCode 기준 조회에서는 null */
    val accountId: Long? = null,
    val abcClosingAmount1: BigDecimal?,
    val abcClosingAmount2: BigDecimal? = null,
    val abcClosingAmount3: BigDecimal? = null,
    val abcClosingAmount4: BigDecimal? = null,
    val shipClosingAmount1: BigDecimal? = null,
    val shipClosingAmount2: BigDecimal? = null,
    val shipClosingAmount3: BigDecimal? = null,
    val shipClosingAmount4: BigDecimal? = null,
)
