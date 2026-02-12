package com.otoki.internal.service

import com.otoki.internal.dto.request.MonthlySalesRequest
import com.otoki.internal.dto.response.MonthlySalesResponse
import com.otoki.internal.repository.MonthlySalesRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 월매출 관련 비즈니스 로직
 */
@Service
class MonthlySalesService(
    private val monthlySalesRepository: MonthlySalesRepository
) {

    /**
     * 월매출 조회
     * - 거래처별 해당 월의 매출 집계 데이터 조회
     * - 제품유형별(상온, 냉장/냉동) 목표/달성 금액 분리
     * - 전년 동월 데이터 조회하여 비교 데이터 생성
     * - 월 평균 실적: 1월~조회월까지의 전년도/금년도 월별 매출 평균 계산
     *
     * TODO: 외부 시스템(Orora, SAP) 연동 구현 후 실제 데이터 조회
     */
    @Transactional(readOnly = true)
    fun getMonthlySales(request: MonthlySalesRequest): MonthlySalesResponse {
        val customerId = request.customerId ?: "ALL"
        val yearMonth = request.yearMonth

        // 해당 연월의 매출 데이터 조회
        val currentMonthData = monthlySalesRepository.findByCustomerIdAndYearMonth(
            customerId = customerId,
            yearMonth = yearMonth
        )

        // 전년 동월 데이터 조회
        val previousYearMonth = request.getPreviousYearMonth()
        val previousMonthData = monthlySalesRepository.findByCustomerIdAndYearMonth(
            customerId = customerId,
            yearMonth = previousYearMonth
        )

        // 연초~조회월까지의 현재년도 데이터 조회
        val currentYearStart = "${request.getYear()}01"
        val currentYearData = monthlySalesRepository.findByCustomerIdAndYearMonthRange(
            customerId = customerId,
            startYearMonth = currentYearStart,
            endYearMonth = yearMonth
        )

        // 연초~조회월까지의 전년도 데이터 조회
        val previousYear = request.getYear() - 1
        val previousYearStart = "${previousYear}01"
        val previousYearEnd = String.format("%04d%02d", previousYear, request.getMonth())
        val previousYearData = monthlySalesRepository.findByCustomerIdAndYearMonthRange(
            customerId = customerId,
            startYearMonth = previousYearStart,
            endYearMonth = previousYearEnd
        )

        // 전체 목표/달성 금액 계산
        val totalTarget = currentMonthData.sumOf { it.targetAmount }
        val totalAchieved = currentMonthData.sumOf { it.achievedAmount }
        val achievementRate = if (totalTarget > 0) {
            (totalAchieved.toDouble() / totalTarget) * 100
        } else {
            0.0
        }

        // 제품유형별 매출 정보
        val categorySales = currentMonthData
            .groupBy { it.category }
            .map { (category, sales) ->
                val categoryTarget = sales.sumOf { it.targetAmount }
                val categoryAchieved = sales.sumOf { it.achievedAmount }
                val categoryRate = if (categoryTarget > 0) {
                    (categoryAchieved.toDouble() / categoryTarget) * 100
                } else {
                    0.0
                }

                MonthlySalesResponse.CategorySalesInfo(
                    category = category,
                    targetAmount = categoryTarget,
                    achievedAmount = categoryAchieved,
                    achievementRate = categoryRate
                )
            }

        // 전년 동월 비교
        val currentYearTotal = currentMonthData.sumOf { it.achievedAmount }
        val previousYearTotal = previousMonthData.sumOf { it.achievedAmount }

        // 월 평균 실적
        val currentYearAverage = if (currentYearData.isNotEmpty()) {
            val months = currentYearData.map { it.yearMonth }.distinct().count()
            if (months > 0) {
                currentYearData.sumOf { it.achievedAmount } / months
            } else {
                0L
            }
        } else {
            0L
        }

        val previousYearAverage = if (previousYearData.isNotEmpty()) {
            val months = previousYearData.map { it.yearMonth }.distinct().count()
            if (months > 0) {
                previousYearData.sumOf { it.achievedAmount } / months
            } else {
                0L
            }
        } else {
            0L
        }

        // TODO: Customer 엔티티가 구현되면 실제 거래처명 조회
        val customerName = customerId

        return MonthlySalesResponse(
            customerId = customerId,
            customerName = customerName,
            yearMonth = yearMonth,
            targetAmount = totalTarget,
            achievedAmount = totalAchieved,
            achievementRate = achievementRate,
            categorySales = categorySales,
            yearComparison = MonthlySalesResponse.YearComparisonInfo(
                currentYear = currentYearTotal,
                previousYear = previousYearTotal
            ),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(
                currentYearAverage = currentYearAverage,
                previousYearAverage = previousYearAverage,
                startMonth = 1,
                endMonth = request.getMonth()
            )
        )
    }
}
