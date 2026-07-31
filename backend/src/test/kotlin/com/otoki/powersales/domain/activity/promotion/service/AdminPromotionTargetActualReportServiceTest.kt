package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
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
        targetCount: BigDecimal,
        basePrice: BigDecimal,
        primaryQty: BigDecimal,
        otherQty: BigDecimal,
        primaryAmount: BigDecimal? = null,
        otherAmount: BigDecimal? = null,
    ): PromotionEmployee {
        val e = PromotionEmployee(
            scheduleDate = scheduleDate,
            dailyTargetCount = targetCount,
            basePrice = basePrice,
            primarySalesQuantity = primaryQty,
            otherSalesQuantity = otherQty,
            primaryProductAmount = primaryAmount,
            otherSalesAmount = otherAmount,
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
            // A행사: 목표 (10×10)+(20×10), 대표수량 2+3, 기타수량 1+1 / B행사: 목표 5×10
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(
                pe("A행사", LocalDate.of(2026, 3, 1), BigDecimal(10), BigDecimal.TEN, BigDecimal(2), BigDecimal.ONE),
                pe("A행사", LocalDate.of(2026, 3, 2), BigDecimal(20), BigDecimal.TEN, BigDecimal(3), BigDecimal.ONE),
                pe("B행사", LocalDate.of(2026, 3, 3), BigDecimal(5), BigDecimal.TEN, BigDecimal(1), BigDecimal.ZERO),
            )

            val res = service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31), EffectiveBranchResult.All)

            assertThat(res.groups).hasSize(2)
            val a = res.groups.first { it.promotionName == "A행사" }
            assertThat(a.rows).hasSize(2)
            assertThat(a.subtotalTargetAmount).isEqualByComparingTo(BigDecimal(300))
            assertThat(a.subtotalPrimaryQuantity).isEqualByComparingTo(BigDecimal(5))
            assertThat(a.subtotalOtherQuantity).isEqualByComparingTo(BigDecimal(2))
            // 전체 합계
            assertThat(res.totalTargetAmount).isEqualByComparingTo(BigDecimal(350))
            // 차트 = 행사명별 실적금액 합계 (2항목)
            assertThat(res.chart).hasSize(2)
        }

        @Test
        @DisplayName("목표금액 = 목표갯수×기준단가, 실적금액 = 대표금액+기타금액 (SF Report 컬럼 formula 재현)")
        fun targetAndActualAmountFormula() {
            // 목표 = 10×100 = 1000, 실적(총 실적) = 300 + 70 = 370
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(
                pe(
                    "A행사", LocalDate.of(2026, 3, 1),
                    targetCount = BigDecimal(10), basePrice = BigDecimal(100),
                    primaryQty = BigDecimal(2), otherQty = BigDecimal(3),
                    primaryAmount = BigDecimal(300), otherAmount = BigDecimal(70),
                ),
            )

            val res = service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31), EffectiveBranchResult.All)

            assertThat(res.groups[0].rows[0].targetAmount).isEqualByComparingTo(BigDecimal(1000))
            assertThat(res.groups[0].rows[0].actualAmount).isEqualByComparingTo(BigDecimal(370))
            assertThat(res.groups[0].subtotalActualAmount).isEqualByComparingTo(BigDecimal(370))
        }

        @Test
        @DisplayName("거래처코드 = ExternalKey, 전문행사조 = 조원일정(투입 당시) 값")
        fun accountCodeAndPptMapping() {
            val row = pe("A행사", LocalDate.of(2026, 3, 1), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO)
            row.teamMemberSchedule = TeamMemberSchedule(professionalPromotionTeam = "라면세일조")
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(row)

            val res = service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31), EffectiveBranchResult.All)

            assertThat(res.groups[0].rows[0].accountCode).isEqualTo("B001")
            assertThat(res.groups[0].rows[0].professionalPromotionTeam).isEqualTo("라면세일조")
        }

        @Test
        @DisplayName("기간 누락 시 IllegalArgumentException")
        fun missingPeriod() {
            assertThatThrownBy {
                service.getReport(null, null, EffectiveBranchResult.All)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("기간을 repository 에 그대로 전달한다")
        fun passesPeriod() {
            val startSlot = slot<LocalDate>()
            val endSlot = slot<LocalDate>()
            every { repository.findTargetActualReport(capture(startSlot), capture(endSlot), any()) } returns emptyList()

            service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31), EffectiveBranchResult.All)

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
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(
                pe("A행사", LocalDate.of(2026, 3, 1), BigDecimal(10), BigDecimal.TEN, BigDecimal(2), BigDecimal.ONE),
            )

            val result = service.exportReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31), EffectiveBranchResult.All)

            assertThat(result.filename).isEqualTo("행사사원목표대비실적_2026-03-01_2026-05-31.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("지점 스코프 (여사원일정 소속 costCenterCode)")
    inner class BranchScope {

        @Test
        @DisplayName("Filtered → 선택 지점 코드를 branchScopeCodes 로 전달")
        fun filtered() {
            val codesSlot = slot<List<String>>()
            every { repository.findTargetActualReport(any(), any(), capture(codesSlot)) } returns emptyList()

            service.getReport(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31),
                EffectiveBranchResult.Filtered(listOf("A001")),
            )

            assertThat(codesSlot.captured).containsExactly("A001")
        }

        @Test
        @DisplayName("All(전사) → 빈 branchScopeCodes 전달")
        fun all() {
            val codesSlot = slot<List<String>>()
            every { repository.findTargetActualReport(any(), any(), capture(codesSlot)) } returns emptyList()

            service.getReport(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31), EffectiveBranchResult.All)

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("NoAccess → repository 미호출 + 빈 결과")
        fun noAccess() {
            val res = service.getReport(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31),
                EffectiveBranchResult.NoAccess,
            )

            assertThat(res.groups).isEmpty()
            io.mockk.verify(exactly = 0) { repository.findTargetActualReport(any(), any(), any()) }
        }
    }
}
