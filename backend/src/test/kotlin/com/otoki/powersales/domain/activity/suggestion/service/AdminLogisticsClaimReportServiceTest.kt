package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionStatus
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.domain.activity.suggestion.service.AdminLogisticsClaimReportService
import com.otoki.powersales.domain.activity.suggestion.service.LogisticsClaimReportPeriod
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@DisplayName("AdminLogisticsClaimReportService 테스트 (Spec #844)")
class AdminLogisticsClaimReportServiceTest {

    private val repository: SuggestionRepository = mockk()
    private val service = AdminLogisticsClaimReportService(repository)

    private fun employee(): Employee =
        Employee(employeeCode = "20230016", name = "홍길동", orgName = "영업1팀").apply {
            jikwee = "사원"
            jobCode = "J01"
        }

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "EXT001")
        acc.name = "○○마트"
        acc.branchName = "서울지점"
        return acc
    }

    private fun suggestion(): Suggestion = Suggestion(
        id = 1L,
        proposalNumber = "S-20260509-000001",
        title = "물류 파손",
        content = "운송 중 파손",
        category = SuggestionCategory.LOGISTICS_CLAIM,
        status = SuggestionStatus.SUBMITTED,
        isDeleted = false,
        employee = employee(),
        account = account(),
        claimDate = LocalDate.of(2026, 5, 9),
        carNumber = "12가3456",
    )

    @Nested
    @DisplayName("조회 — 기간 프리셋")
    inner class GetReport {

        @Test
        @DisplayName("THIS_MONTH 면 당월 1일~말일 산출")
        fun thisMonth() {
            val startSlot = slot<LocalDate>()
            val endSlot = slot<LocalDate>()
            every { repository.findLogisticsClaimReport(capture(startSlot), capture(endSlot), any()) } returns listOf(suggestion())

            val res = service.getReport(LogisticsClaimReportPeriod.THIS_MONTH, null, null, EffectiveBranchResult.All)

            val today = LocalDate.now(TimeZones.SEOUL_ZONE)
            assertThat(startSlot.captured).isEqualTo(today.withDayOfMonth(1))
            assertThat(endSlot.captured).isEqualTo(today.with(TemporalAdjusters.lastDayOfMonth()))
            assertThat(res.period).isEqualTo("THIS_MONTH")
            assertThat(res.items).hasSize(1)
            assertThat(res.items[0].branchName).isEqualTo("서울지점")
            assertThat(res.items[0].jikwee).isEqualTo("사원")
            assertThat(res.items[0].carNumber).isEqualTo("12가3456")
        }

        @Test
        @DisplayName("LAST_MONTH 면 전월 1일~말일 산출")
        fun lastMonth() {
            val startSlot = slot<LocalDate>()
            val endSlot = slot<LocalDate>()
            every { repository.findLogisticsClaimReport(capture(startSlot), capture(endSlot), any()) } returns emptyList()

            service.getReport(LogisticsClaimReportPeriod.LAST_MONTH, null, null, EffectiveBranchResult.All)

            val lastMonth = LocalDate.now(TimeZones.SEOUL_ZONE).minusMonths(1)
            assertThat(startSlot.captured).isEqualTo(lastMonth.withDayOfMonth(1))
            assertThat(endSlot.captured).isEqualTo(lastMonth.with(TemporalAdjusters.lastDayOfMonth()))
        }

        @Test
        @DisplayName("CUSTOM 면 입력 기간 그대로")
        fun custom() {
            val startSlot = slot<LocalDate>()
            val endSlot = slot<LocalDate>()
            every { repository.findLogisticsClaimReport(capture(startSlot), capture(endSlot), any()) } returns emptyList()

            service.getReport(
                LogisticsClaimReportPeriod.CUSTOM,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                EffectiveBranchResult.All,
            )

            assertThat(startSlot.captured).isEqualTo(LocalDate.of(2026, 3, 1))
            assertThat(endSlot.captured).isEqualTo(LocalDate.of(2026, 3, 31))
        }

        @Test
        @DisplayName("CUSTOM 인데 기간 누락 시 IllegalArgumentException")
        fun customMissing() {
            assertThatThrownBy {
                service.getReport(LogisticsClaimReportPeriod.CUSTOM, null, null, EffectiveBranchResult.All)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("당월 파일명")
        fun exportThisMonth() {
            every { repository.findLogisticsClaimReport(any(), any(), any()) } returns listOf(suggestion())

            val result = service.exportReport(LogisticsClaimReportPeriod.THIS_MONTH, null, null, EffectiveBranchResult.All)

            assertThat(result.filename).startsWith("물류클레임보고서_당월_")
            assertThat(result.bytes).isNotEmpty()
        }

        @Test
        @DisplayName("CUSTOM 파일명 — 기간별")
        fun exportCustom() {
            every { repository.findLogisticsClaimReport(any(), any(), any()) } returns listOf(suggestion())

            val result = service.exportReport(
                LogisticsClaimReportPeriod.CUSTOM,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                EffectiveBranchResult.All,
            )

            assertThat(result.filename).isEqualTo("물류클레임보고서_기간별_2026-03-01_2026-03-31.xlsx")
        }
    }

    @Nested
    @DisplayName("지점 스코프 (등록 사원 소속 orgCostCenterCode)")
    inner class BranchScope {

        @Test
        @DisplayName("Filtered → 선택 지점 코드를 branchScopeCodes 로 전달")
        fun filtered() {
            val codesSlot = slot<List<String>>()
            every { repository.findLogisticsClaimReport(any(), any(), capture(codesSlot)) } returns emptyList()

            service.getReport(
                LogisticsClaimReportPeriod.THIS_MONTH, null, null,
                EffectiveBranchResult.Filtered(listOf("A001")),
            )

            assertThat(codesSlot.captured).containsExactly("A001")
        }

        @Test
        @DisplayName("All(전사) → 빈 branchScopeCodes 전달")
        fun all() {
            val codesSlot = slot<List<String>>()
            every { repository.findLogisticsClaimReport(any(), any(), capture(codesSlot)) } returns emptyList()

            service.getReport(LogisticsClaimReportPeriod.THIS_MONTH, null, null, EffectiveBranchResult.All)

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("NoAccess → repository 미호출 + 빈 결과")
        fun noAccess() {
            val res = service.getReport(
                LogisticsClaimReportPeriod.THIS_MONTH, null, null,
                EffectiveBranchResult.NoAccess,
            )

            assertThat(res.items).isEmpty()
            io.mockk.verify(exactly = 0) { repository.findLogisticsClaimReport(any(), any(), any()) }
        }
    }
}
