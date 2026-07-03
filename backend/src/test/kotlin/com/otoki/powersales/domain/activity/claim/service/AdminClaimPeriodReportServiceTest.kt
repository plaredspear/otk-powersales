package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.domain.activity.claim.repository.AdminClaimRepository
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.product.entity.Product
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("AdminClaimPeriodReportService 테스트 (Spec #843)")
class AdminClaimPeriodReportServiceTest {

    private val repository: AdminClaimRepository = mockk()
    private val service = AdminClaimPeriodReportService(repository)

    private fun employee(): Employee =
        Employee(employeeCode = "20230016", name = "홍길동", orgName = "영업1팀").apply { phone = "010-1234-5678" }

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "EXT001")
        acc.name = "○○마트"
        acc.branchName = "서울지점"
        return acc
    }

    private fun product(): Product = Product(productCode = "P001").apply { name = "진라면" }

    private fun claim(type1: ClaimType1 = ClaimType1.A, qty: Long = 3): Claim =
        Claim(
            id = 1L,
            employee = employee(),
            account = account(),
            product = product(),
            dateType = ClaimDateType.EXPIRY_DATE,
            date = LocalDate.of(2026, 5, 9),
            claimType1 = type1,
            claimType2 = ClaimType2.AA,
            defectDescription = "포장 불량",
            defectQuantity = BigDecimal.valueOf(qty),
            status = ClaimStatus.SENT,
            name = "CL-0001",
        )

    @Nested
    @DisplayName("조회")
    inner class GetReport {

        @Test
        @DisplayName("PACKAGING 이면 claimType1=A 필터를 전달한다")
        fun packagingFilter() {
            val type1Slot = slot<ClaimType1>()
            every { repository.findPeriodReport(any(), any(), capture(type1Slot), any()) } returns listOf(claim())

            val res = service.getReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.PACKAGING, EffectiveBranchResult.All)

            assertThat(type1Slot.captured).isEqualTo(ClaimType1.A)
            assertThat(res.type).isEqualTo("PACKAGING")
            assertThat(res.items).hasSize(1)
            assertThat(res.items[0].claimName).isEqualTo("CL-0001")
            assertThat(res.items[0].branchName).isEqualTo("서울지점")
            assertThat(res.items[0].mobilePhone).isEqualTo("010-1234-5678")
            assertThat(res.items[0].productName).isEqualTo("진라면")
            assertThat(res.items[0].claimType1).isEqualTo("포장불량")
        }

        @Test
        @DisplayName("ALL 이면 claimType1 필터를 null(전체)로 전달한다")
        fun allNoTypeFilter() {
            every { repository.findPeriodReport(any(), any(), isNull(), any()) } returns
                listOf(claim(ClaimType1.A), claim(ClaimType1.B), claim(ClaimType1.C))

            val res = service.getReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL, EffectiveBranchResult.All)

            assertThat(res.type).isEqualTo("ALL")
            assertThat(res.items).hasSize(3)
        }

        @Test
        @DisplayName("totalQuantity 는 수량 합계")
        fun totalQuantity() {
            every { repository.findPeriodReport(any(), any(), any(), any()) } returns
                listOf(claim(qty = 3), claim(qty = 5), claim(qty = 2))

            val res = service.getReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL, EffectiveBranchResult.All)

            assertThat(res.totalQuantity).isEqualByComparingTo(BigDecimal.valueOf(10))
        }

        @Test
        @DisplayName("기간 미지정 시 당월 1일~오늘")
        fun defaultsToThisMonth() {
            val startSlot = slot<LocalDate>()
            val endSlot = slot<LocalDate>()
            every { repository.findPeriodReport(capture(startSlot), capture(endSlot), any(), any()) } returns emptyList()

            service.getReport(null, null, ClaimPeriodReportType.ALL, EffectiveBranchResult.All)

            val today = LocalDate.now(TimeZones.SEOUL_ZONE)
            assertThat(startSlot.captured).isEqualTo(today.withDayOfMonth(1))
            assertThat(endSlot.captured).isEqualTo(today)
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("PACKAGING 파일명 + 합계 행")
        fun exportPackaging() {
            every { repository.findPeriodReport(any(), any(), any(), any()) } returns listOf(claim())

            val result = service.exportReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.PACKAGING, EffectiveBranchResult.All)

            assertThat(result.filename).isEqualTo("기간별클레임_포장불량_2026-05-01_2026-05-31.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }

        @Test
        @DisplayName("ALL 파일명")
        fun exportAll() {
            every { repository.findPeriodReport(any(), any(), any(), any()) } returns listOf(claim())

            val result = service.exportReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL, EffectiveBranchResult.All)

            assertThat(result.filename).isEqualTo("기간별클레임_모든클레임_2026-05-01_2026-05-31.xlsx")
        }
    }

    @Nested
    @DisplayName("지점 스코프 (사원 소속 costCenterCode)")
    inner class BranchScope {

        @Test
        @DisplayName("Filtered → 선택 지점 코드를 branchScopeCodes 로 전달")
        fun filtered() {
            val codesSlot = slot<List<String>>()
            every { repository.findPeriodReport(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getReport(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL,
                EffectiveBranchResult.Filtered(listOf("A001")),
            )

            assertThat(codesSlot.captured).containsExactly("A001")
        }

        @Test
        @DisplayName("All(전사) → 빈 branchScopeCodes 전달")
        fun all() {
            val codesSlot = slot<List<String>>()
            every { repository.findPeriodReport(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getReport(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL,
                EffectiveBranchResult.All,
            )

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("NoAccess → repository 미호출 + 빈 결과")
        fun noAccess() {
            val res = service.getReport(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL,
                EffectiveBranchResult.NoAccess,
            )

            assertThat(res.items).isEmpty()
            assertThat(res.totalQuantity).isEqualByComparingTo(BigDecimal.ZERO)
            io.mockk.verify(exactly = 0) { repository.findPeriodReport(any(), any(), any(), any()) }
        }
    }
}
