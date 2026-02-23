package com.otoki.internal.service

import com.otoki.internal.dto.request.MonthlySalesRequest
import com.otoki.internal.dto.response.MonthlySalesResponse
import com.otoki.internal.repository.MonthlySalesHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 월매출 관련 비즈니스 로직
 * TODO: V1 필드(salesYear, salesMonth, accountExternalKey, targetMonthResults, shipClosingAmount 등) 기반으로 재구현 필요 (후속 스펙)
 */
@Service
class MonthlySalesService(
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository
) {

    @Transactional(readOnly = true)
    fun getMonthlySales(request: MonthlySalesRequest): MonthlySalesResponse {
        // TODO: V1 스키마 필드 기반으로 재구현 (후속 스펙)
        val customerId = request.customerId ?: "ALL"
        return MonthlySalesResponse(
            customerId = customerId,
            customerName = customerId,
            yearMonth = request.yearMonth,
            targetAmount = 0,
            achievedAmount = 0,
            achievementRate = 0.0,
            categorySales = emptyList(),
            yearComparison = MonthlySalesResponse.YearComparisonInfo(0, 0),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(0, 0, 1, request.getMonth())
        )
    }
}
