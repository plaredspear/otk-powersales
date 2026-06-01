package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.dto.request.MonthlySalesRequest
import com.otoki.powersales.sales.dto.response.MonthlySalesResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 월매출 조회 service.
 *
 * ## 데이터 source
 * RDS `MonthlySalesHistory` (SF `MonthlySalesHistory__c` 복제 적재) 의 실측 마감실적만 사용
 * ([MonthlySalesHistoryQueryGateway] 경유). SF 레거시도 모바일 `IF_REST_MOBILE_MonthlySalesHistory.cls`
 * 가 ORORA 직접이 아닌 `MonthlySalesHistory__c` SObject 를 조회한 것과 동등.
 * 목표 (`thisMonthTarget`) / 확정 상태 (`isConfirmed`) 는 폐기 — 응답 필드 호환성 유지를 위해
 * `targetAmount = 0`, `achievementRate = 0.0` 로 고정 반환.
 *
 * ## 응답 산출
 * - `achievedAmount` = 카테고리 4종 ABC + Ship 합산 ([MonthlySalesRow.closingAmountSum] 동등)
 * - `categorySales` = 카테고리별 ABC + Ship 합산 — SF Apex `IF_REST_MOBILE_MonthlySalesHistory.cls` 정합
 * - `yearComparison` = 당년 / 전년 동월 ABC+Ship 합산
 * - `monthlyAverage` = 1월~조회월 누적 ABC+Ship 합산 / 월수 (당년 / 전년)
 */
@Service
@Transactional(readOnly = true)
class MonthlySalesService(
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
) {

    fun getMonthlySales(request: MonthlySalesRequest): MonthlySalesResponse {
        val customerId = request.customerId
        val year = request.getYear()
        val month = request.getMonth()
        val sapCode = customerId ?: ""
        if (sapCode.isBlank()) {
            return emptyResponse(customerId ?: "ALL", request.yearMonth, year, month)
        }

        val currentRangeSalesDates = (1..month).map { toSalesDate(year, it) }
        val previousRangeSalesDates = (1..month).map { toSalesDate(year - 1, it) }
        val rowsByDate = monthlySalesHistoryGateway
            .findBySalesDates((currentRangeSalesDates + previousRangeSalesDates).distinct(), listOf(sapCode))
            .associateBy { it.salesDate }

        val currentRow = rowsByDate[toSalesDate(year, month)]
        val previousRow = rowsByDate[toSalesDate(year - 1, month)]

        val achieved = currentRow?.closingAmountSum?.toLong() ?: 0L
        val previousAchieved = previousRow?.closingAmountSum?.toLong() ?: 0L

        val currentAvg = currentRangeSalesDates
            .sumOf { rowsByDate[it]?.closingAmountSum?.toLong() ?: 0L } / month
        val previousAvg = previousRangeSalesDates
            .sumOf { rowsByDate[it]?.closingAmountSum?.toLong() ?: 0L } / month

        return MonthlySalesResponse(
            customerId = customerId ?: sapCode,
            customerName = customerId ?: sapCode,
            yearMonth = request.yearMonth,
            targetAmount = 0L,
            achievedAmount = achieved,
            achievementRate = 0.0,
            categorySales = buildCategorySales(currentRow),
            yearComparison = MonthlySalesResponse.YearComparisonInfo(achieved, previousAchieved),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(
                currentYearAverage = currentAvg,
                previousYearAverage = previousAvg,
                startMonth = 1,
                endMonth = month,
            ),
        )
    }

    private fun emptyResponse(customerId: String, yearMonth: String, year: Int, month: Int) =
        MonthlySalesResponse(
            customerId = customerId,
            customerName = customerId,
            yearMonth = yearMonth,
            targetAmount = 0L,
            achievedAmount = 0L,
            achievementRate = 0.0,
            categorySales = emptyList(),
            yearComparison = MonthlySalesResponse.YearComparisonInfo(0L, 0L),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(0L, 0L, 1, month),
        )

    private fun buildCategorySales(oro: MonthlySalesRow?): List<MonthlySalesResponse.CategorySalesInfo> {
        if (oro == null) return emptyList()
        return SalesCategory.entries.map { category ->
            MonthlySalesResponse.CategorySalesInfo(
                category = category.name,
                targetAmount = 0L,
                achievedAmount = categoryAchieved(oro, category),
                achievementRate = 0.0,
            )
        }
    }

    private fun categoryAchieved(oro: MonthlySalesRow, category: SalesCategory): Long {
        val abc = when (category) {
            SalesCategory.AMBIENT -> oro.abcClosingAmount1
            SalesCategory.NOODLE -> oro.abcClosingAmount2
            SalesCategory.FROZEN_REFRIGERATED -> oro.abcClosingAmount3
            SalesCategory.OIL_FAT -> oro.abcClosingAmount4
        }
        val ship = when (category) {
            SalesCategory.AMBIENT -> oro.shipClosingAmount1
            SalesCategory.NOODLE -> oro.shipClosingAmount2
            SalesCategory.FROZEN_REFRIGERATED -> oro.shipClosingAmount3
            SalesCategory.OIL_FAT -> oro.shipClosingAmount4
        }
        return (abc ?: BigDecimal.ZERO).toLong() + (ship ?: BigDecimal.ZERO).toLong()
    }

    private fun toSalesDate(year: Int, month: Int): String = "%04d%02d".format(year, month)
}
