package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
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
 * ## 마감실적 합계 산출 (ORORA `ClosingAmountSum__c` formula 동등)
 * SF formula 복제 컬럼 (`abcClosingSumAmount` / `shipClosingSumAmount`) 에 의존하지 않고,
 * 개별 카테고리 컬럼을 코드로 재합산한다 — `(abc1+abc2+abc3+abc4) + (ship1+ship2+ship3+ship4)`.
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
            .asSequence()
            .filter { it.isDeleted != true }
            .filter { row ->
                val year = row.salesYear ?: return@filter false
                val month = row.salesMonth ?: return@filter false
                (year to month) in requestedYearMonths
            }
            .mapNotNull { row ->
                val sapCode = row.sapAccountCode ?: return@mapNotNull null
                MonthlySalesRow(
                    sapAccountCode = sapCode,
                    closingAmountSum = closingAmountSum(row),
                    abcClosingAmount1 = row.abcClosingAmount1?.let { BigDecimal.valueOf(it) },
                )
            }
            .toList()
    }

    /**
     * SF `ClosingAmountSum__c` formula 동등 — `(abc1+abc2+abc3+abc4) + (ship1+ship2+ship3+ship4)`.
     * null 컬럼은 `BlankAsZero` 정합으로 `ZERO` 치환.
     */
    private fun closingAmountSum(row: MonthlySalesHistory): BigDecimal {
        val abcSum = listOf(
            row.abcClosingAmount1, row.abcClosingAmount2,
            row.abcClosingAmount3, row.abcClosingAmount4,
        ).sumOf { it ?: 0.0 }
        val shipSum = listOf(
            row.shipClosingAmount1, row.shipClosingAmount2,
            row.shipClosingAmount3, row.shipClosingAmount4,
        ).sumOf { it ?: 0.0 }
        return BigDecimal.valueOf(abcSum + shipSum)
    }

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
 * @property closingAmountSum SF `ClosingAmountSum__c` formula 동등 합계 (ABC합 + Ship합)
 * @property abcClosingAmount1 전산마감실적_상온 (refresh/batch 의 양수 필터 평균 산출용). null 가능
 */
data class MonthlySalesRow(
    val sapAccountCode: String,
    val closingAmountSum: BigDecimal,
    val abcClosingAmount1: BigDecimal?,
)
