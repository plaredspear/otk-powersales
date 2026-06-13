package com.otoki.powersales.promotion.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("AdminPromotionTargetActualReportService 테스트 (Spec #845)")
class AdminPromotionTargetActualReportServiceTest {

    private val repository: PromotionEmployeeRepository = mockk()
    private val service = AdminPromotionTargetActualReportService(repository)

    private fun promotion(name: String): Promotion {
        // SF Promotion.Name = promotionNumber (별도 name 필드 없음)
        val p = Promotion(
            promotionNumber = name,
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 5, 31),
        )
        p.account = Account(id = 1, externalKey = "B001").apply {
            this.name = "○○점"
            branchName = "서울지점"
            branchCode = "B001"
        }
        return p
    }

    private fun employee(): Employee = Employee(employeeCode = "12345", name = "홍길동", orgName = "영업1팀")

    private fun pe(
        promotionName: String,
        scheduleDate: LocalDate,
        target: Long,
        primaryPrice: BigDecimal,
        primaryQty: BigDecimal,
        otherQty: BigDecimal,
    ): PromotionEmployee {
        val e = PromotionEmployee(
            scheduleDate = scheduleDate,
            targetAmount = target,
            primarySalesPrice = primaryPrice,
            primarySalesQuantity = primaryQty,
            otherSalesQuantity = otherQty,
        )
        e.promotion = promotion(promotionName)
        e.employee = employee()
        return e
    }

    @Nested
    @DisplayName("조회 — 그룹/소계/합계/차트")
    inner class GetReport {

        @Test
        @DisplayName("행사명별로 그룹핑하고 소계/합계를 산출한다")
        fun groupsAndSubtotals() {
            // A행사: 목표 100+200, 대표수량 2+3, 기타수량 1+1 / B행사: 목표 50
            every { repository.findTargetActualReport(any(), any()) } returns listOf(
                pe("A행사", LocalDate.of(2026, 3, 1), 100, BigDecimal.TEN, BigDecimal(2), BigDecimal.ONE),
                pe("A행사", LocalDate.of(2026, 3, 2), 200, BigDecimal.TEN, BigDecimal(3), BigDecimal.ONE),
                pe("B행사", LocalDate.of(2026, 3, 3), 50, BigDecimal.TEN, BigDecimal(1), BigDecimal.ZERO),
            )

            val res = service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31))

            assertThat(res.groups).hasSize(2)
            val a = res.groups.first { it.promotionName == "A행사" }
            assertThat(a.rows).hasSize(2)
            assertThat(a.subtotalTargetAmount).isEqualTo(300L)
            assertThat(a.subtotalPrimaryQuantity).isEqualByComparingTo(BigDecimal(5))
            assertThat(a.subtotalOtherQuantity).isEqualByComparingTo(BigDecimal(2))
            // 전체 합계
            assertThat(res.totalTargetAmount).isEqualTo(350L)
            // 차트 = 행사명별 실적금액 합계 (2항목)
            assertThat(res.chart).hasSize(2)
        }

        @Test
        @DisplayName("실적금액은 SF formula(dkDailyActualSalesAmount) 재현 — price*priQty + otherQty^2")
        fun actualAmountFormula() {
            // price=10, priQty=2, otherQty=3 → 10*2 + 3*3 = 29
            every { repository.findTargetActualReport(any(), any()) } returns listOf(
                pe("A행사", LocalDate.of(2026, 3, 1), 100, BigDecimal.TEN, BigDecimal(2), BigDecimal(3)),
            )

            val res = service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31))

            assertThat(res.groups[0].rows[0].actualAmount).isEqualByComparingTo(BigDecimal(29))
            assertThat(res.groups[0].subtotalActualAmount).isEqualByComparingTo(BigDecimal(29))
        }

        @Test
        @DisplayName("기간 누락 시 IllegalArgumentException")
        fun missingPeriod() {
            assertThatThrownBy {
                service.getReport(null, null)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("기간을 repository 에 그대로 전달한다")
        fun passesPeriod() {
            val startSlot = slot<LocalDate>()
            val endSlot = slot<LocalDate>()
            every { repository.findTargetActualReport(capture(startSlot), capture(endSlot)) } returns emptyList()

            service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31))

            assertThat(startSlot.captured).isEqualTo(LocalDate.of(2026, 3, 1))
            assertThat(endSlot.captured).isEqualTo(LocalDate.of(2026, 5, 31))
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("그룹/소계/합계 행 포함 xlsx + 파일명")
        fun exportsXlsx() {
            every { repository.findTargetActualReport(any(), any()) } returns listOf(
                pe("A행사", LocalDate.of(2026, 3, 1), 100, BigDecimal.TEN, BigDecimal(2), BigDecimal.ONE),
            )

            val result = service.exportReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31))

            assertThat(result.filename).isEqualTo("행사사원목표대비실적_2026-03-01_2026-05-31.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
