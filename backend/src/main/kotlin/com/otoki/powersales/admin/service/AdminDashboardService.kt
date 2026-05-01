package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.response.BasicStats
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.dto.response.PreviousMonthData
import com.otoki.powersales.admin.dto.response.SalesSummary
import com.otoki.powersales.admin.dto.response.StaffDeployment
import com.otoki.powersales.admin.dto.response.StaffTypeCount
import com.otoki.powersales.admin.dto.response.TotalByPosition
import com.otoki.powersales.admin.dto.response.WorkTypeStats
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class AdminDashboardService {

    fun getDashboard(yearMonth: String?, branchCode: String?): DashboardResponse {
        val ym = yearMonth ?: YearMonth.now().format(YEAR_MONTH_FORMATTER)
        return DashboardResponse(
            salesSummary = SalesSummary(
                yearMonth = ym,
                branchName = null,
                targetAmount = 0L,
                actualAmount = 0L,
                progressRate = 0.0,
                referenceProgressRate = 0.0,
                lastYearAmount = 0L,
                lastYearRatio = 0.0,
                channelSales = emptyList()
            ),
            staffDeployment = StaffDeployment(
                yearMonth = ym,
                branchName = null,
                byAccountType = emptyList(),
                byWorkType = emptyList(),
                byChannelAndWorkType = emptyList(),
                previousMonth = PreviousMonthData(byWorkType = emptyList())
            ),
            basicStats = BasicStats(
                branchName = null,
                staffType = StaffTypeCount(promotion = 0, osc = 0),
                totalByPosition = TotalByPosition(active = 0, onLeave = 0),
                byAgeGroup = emptyList(),
                byWorkType = WorkTypeStats(fixed = 0, alternating = 0, visiting = 0)
            )
        )
    }

    companion object {
        private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
