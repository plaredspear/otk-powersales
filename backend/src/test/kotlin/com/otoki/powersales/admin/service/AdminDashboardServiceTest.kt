package com.otoki.powersales.admin.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@DisplayName("AdminDashboardService 테스트 (스텁 모드)")
class AdminDashboardServiceTest {

    private val adminDashboardService = AdminDashboardService()

    @Nested
    @DisplayName("getDashboard - 스텁 모드 fake 응답")
    inner class GetDashboardTests {

        @Test
        @DisplayName("입력 yearMonth 보존 - getDashboard(\"2026-03\", null) -> 응답의 yearMonth 동일")
        fun preservesInputYearMonth() {
            val result = adminDashboardService.getDashboard("2026-03", null)

            assertThat(result.salesSummary.yearMonth).isEqualTo("2026-03")
            assertThat(result.staffDeployment.yearMonth).isEqualTo("2026-03")
        }

        @Test
        @DisplayName("yearMonth 누락 - 서버 현재 연월 사용")
        fun usesCurrentYearMonthWhenNull() {
            val expected = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

            val result = adminDashboardService.getDashboard(null, null)

            assertThat(result.salesSummary.yearMonth).isEqualTo(expected)
            assertThat(result.staffDeployment.yearMonth).isEqualTo(expected)
            assertThat(result.salesSummary.yearMonth).matches("\\d{4}-(0[1-9]|1[0-2])")
        }

        @Test
        @DisplayName("branchCode 무시 - getDashboard(\"2026-03\", \"B001\") -> 모든 branchName 필드 null")
        fun ignoresBranchCode() {
            val result = adminDashboardService.getDashboard("2026-03", "B001")

            assertThat(result.salesSummary.branchName).isNull()
            assertThat(result.staffDeployment.branchName).isNull()
            assertThat(result.basicStats.branchName).isNull()
        }

        @Test
        @DisplayName("매출 수치 모두 0 - 모든 금액/비율 필드 0")
        fun salesSummaryFieldsAreZero() {
            val result = adminDashboardService.getDashboard("2026-03", null)

            assertThat(result.salesSummary.targetAmount).isZero()
            assertThat(result.salesSummary.actualAmount).isZero()
            assertThat(result.salesSummary.progressRate).isZero()
            assertThat(result.salesSummary.referenceProgressRate).isZero()
            assertThat(result.salesSummary.lastYearAmount).isZero()
            assertThat(result.salesSummary.lastYearRatio).isZero()
        }

        @Test
        @DisplayName("매출 채널 빈 배열 - channelSales 비어 있음")
        fun channelSalesIsEmpty() {
            val result = adminDashboardService.getDashboard("2026-03", null)

            assertThat(result.salesSummary.channelSales).isEmpty()
        }

        @Test
        @DisplayName("인력 배치 빈 배열 - 모든 리스트/이전 달 byWorkType 비어 있음")
        fun staffDeploymentListsAreEmpty() {
            val result = adminDashboardService.getDashboard("2026-03", null)

            assertThat(result.staffDeployment.byAccountType).isEmpty()
            assertThat(result.staffDeployment.byWorkType).isEmpty()
            assertThat(result.staffDeployment.byChannelAndWorkType).isEmpty()
            assertThat(result.staffDeployment.previousMonth.byWorkType).isEmpty()
        }

        @Test
        @DisplayName("기본 현황 0/빈 배열 - staffType, totalByPosition, byAgeGroup, byWorkType")
        fun basicStatsAreZero() {
            val result = adminDashboardService.getDashboard("2026-03", null)

            assertThat(result.basicStats.staffType.promotion).isZero()
            assertThat(result.basicStats.staffType.osc).isZero()
            assertThat(result.basicStats.totalByPosition.active).isZero()
            assertThat(result.basicStats.totalByPosition.onLeave).isZero()
            assertThat(result.basicStats.byAgeGroup).isEmpty()
            assertThat(result.basicStats.byWorkType.fixed).isZero()
            assertThat(result.basicStats.byWorkType.alternating).isZero()
            assertThat(result.basicStats.byWorkType.visiting).isZero()
        }

        @Test
        @DisplayName("Repository 의존성 0 - 생성자 파라미터 없음")
        fun hasNoRepositoryDependencies() {
            val constructors = AdminDashboardService::class.java.declaredConstructors

            assertThat(constructors).hasSize(1)
            assertThat(constructors[0].parameterCount).isZero()
        }
    }
}
