package com.otoki.powersales.claim.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.enums.ClaimDateType
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.claim.repository.AdminClaimRepository
import com.otoki.powersales.common.util.TimeZones
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.product.entity.Product
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
            every { repository.findPeriodReport(any(), any(), capture(type1Slot)) } returns listOf(claim())

            val res = service.getReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.PACKAGING)

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
            every { repository.findPeriodReport(any(), any(), isNull()) } returns
                listOf(claim(ClaimType1.A), claim(ClaimType1.B), claim(ClaimType1.C))

            val res = service.getReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL)

            assertThat(res.type).isEqualTo("ALL")
            assertThat(res.items).hasSize(3)
        }

        @Test
        @DisplayName("totalQuantity 는 수량 합계")
        fun totalQuantity() {
            every { repository.findPeriodReport(any(), any(), any()) } returns
                listOf(claim(qty = 3), claim(qty = 5), claim(qty = 2))

            val res = service.getReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL)

            assertThat(res.totalQuantity).isEqualByComparingTo(BigDecimal.valueOf(10))
        }

        @Test
        @DisplayName("기간 미지정 시 당월 1일~오늘")
        fun defaultsToThisMonth() {
            val startSlot = slot<LocalDate>()
            val endSlot = slot<LocalDate>()
            every { repository.findPeriodReport(capture(startSlot), capture(endSlot), any()) } returns emptyList()

            service.getReport(null, null, ClaimPeriodReportType.ALL)

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
            every { repository.findPeriodReport(any(), any(), any()) } returns listOf(claim())

            val result = service.exportReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.PACKAGING)

            assertThat(result.filename).isEqualTo("기간별클레임_포장불량_2026-05-01_2026-05-31.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }

        @Test
        @DisplayName("ALL 파일명")
        fun exportAll() {
            every { repository.findPeriodReport(any(), any(), any()) } returns listOf(claim())

            val result = service.exportReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ClaimPeriodReportType.ALL)

            assertThat(result.filename).isEqualTo("기간별클레임_모든클레임_2026-05-01_2026-05-31.xlsx")
        }
    }
}
