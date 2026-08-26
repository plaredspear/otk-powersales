package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionTargetActualReportRecord
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

    private fun record(
        promotionName: String,
        scheduleDate: LocalDate,
        targetCount: BigDecimal? = null,
        basePrice: BigDecimal? = null,
        primaryQty: BigDecimal? = null,
        otherQty: BigDecimal? = null,
        primaryAmount: BigDecimal? = null,
        otherAmount: BigDecimal? = null,
        professionalPromotionTeam: String? = null,
    ): PromotionTargetActualReportRecord {
        return PromotionTargetActualReportRecord(
            promotionName = promotionName,
            branchName = "서울지점",
            accountName = "○○점",
            accountCode = "B001",
            primaryProductName = null,
            category1 = null,
            otherProduct = null,
            standLocation = null,
            employeeCode = "12345",
            employeeOrgName = "영업1팀",
            employeeName = "홍길동",
            professionalPromotionTeamCurrent = null,
            professionalPromotionTeam = professionalPromotionTeam,
            scheduleDate = scheduleDate,
            dailyTargetCount = targetCount,
            basePrice = basePrice,
            primarySalesQuantity = primaryQty,
            primaryProductAmount = primaryAmount,
            otherSalesQuantity = otherQty,
            otherSalesAmount = otherAmount,
            workType2 = null,
            workType3 = null,
            attendanceLogId = null,
            commuteDate = null,
        )
    }

    @Nested
    @DisplayName("조회 — 그룹/소계/합계/차트")
    inner class GetReport {

        @Test
        @DisplayName("행사명별로 그룹핑하고 소계/합계를 산출한다")
        fun groupsAndSubtotals() {
            // A행사: 목표 (10×10)+(20×10), 대표수량 2+3, 기타수량 1+1 / B행사: 목표 5×10
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(
                record("A행사", LocalDate.of(2026, 3, 1), BigDecimal(10), BigDecimal.TEN, BigDecimal(2), BigDecimal.ONE),
                record("A행사", LocalDate.of(2026, 3, 2), BigDecimal(20), BigDecimal.TEN, BigDecimal(3), BigDecimal.ONE),
                record("B행사", LocalDate.of(2026, 3, 3), BigDecimal(5), BigDecimal.TEN, BigDecimal(1), BigDecimal.ZERO),
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
            // 표시 상한 미달 — 잘림 없음
            assertThat(res.totalRowCount).isEqualTo(3)
            assertThat(res.displayedRowCount).isEqualTo(3)
            assertThat(res.truncated).isFalse()
        }

        @Test
        @DisplayName("목표금액 = 목표갯수×기준단가, 실적금액 = 대표금액+기타금액 (SF Report 컬럼 formula 재현)")
        fun targetAndActualAmountFormula() {
            // 목표 = 10×100 = 1000, 실적(총 실적) = 300 + 70 = 370
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(
                record(
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
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(
                record(
                    "A행사", LocalDate.of(2026, 3, 1),
                    targetCount = BigDecimal.ONE, basePrice = BigDecimal.TEN,
                    primaryQty = BigDecimal.ONE, otherQty = BigDecimal.ZERO,
                    professionalPromotionTeam = "라면세일조",
                ),
            )

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
    @DisplayName("화면 표시 상한 (SF 리포트 2,000행 표시 제한 정합)")
    inner class DisplayRowLimit {

        @Test
        @DisplayName("상세 행은 상한까지만 포함 — 소계/합계는 전량 기준 유지")
        fun truncatesRowsButKeepsFullSubtotals() {
            // A행사 1,500행 + B행사 1,000행 = 2,500행 > 상한 2,000행
            val records =
                (1..1500).map { record("A행사", LocalDate.of(2026, 6, 1), BigDecimal.ONE, BigDecimal.TEN) } +
                    (1..1000).map { record("B행사", LocalDate.of(2026, 6, 2), BigDecimal.ONE, BigDecimal.TEN) }
            every { repository.findTargetActualReport(any(), any(), any()) } returns records

            val res = service.getReport(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), EffectiveBranchResult.All)

            assertThat(res.totalRowCount).isEqualTo(2500)
            assertThat(res.displayedRowCount).isEqualTo(AdminPromotionTargetActualReportService.WEB_DISPLAY_ROW_LIMIT)
            assertThat(res.truncated).isTrue()
            // 그룹 순서대로 앞에서부터 채움 — A행사 전량 + B행사 잔여분
            assertThat(res.groups[0].rows).hasSize(1500)
            assertThat(res.groups[1].rows).hasSize(500)
            // 소계/합계는 잘림과 무관하게 전량 기준
            assertThat(res.groups[1].subtotalTargetAmount).isEqualByComparingTo(BigDecimal(10_000))
            assertThat(res.totalTargetAmount).isEqualByComparingTo(BigDecimal(25_000))
        }

        @Test
        @DisplayName("엑셀 export 는 상한 없이 전량 추출")
        fun exportIsNotTruncated() {
            val records = (1..2500).map { record("A행사", LocalDate.of(2026, 6, 1), BigDecimal.ONE, BigDecimal.TEN) }
            every { repository.findTargetActualReport(any(), any(), any()) } returns records

            val result = service.exportReport(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), EffectiveBranchResult.All)

            // 헤더 1 + 상세 2,500 + 소계 1 + 합계 1 = 2,503행
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook(result.bytes.inputStream())
            assertThat(workbook.getSheetAt(0).lastRowNum + 1).isEqualTo(2503)
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("그룹/소계/합계 행 포함 xlsx + 파일명")
        fun exportsXlsx() {
            every { repository.findTargetActualReport(any(), any(), any()) } returns listOf(
                record("A행사", LocalDate.of(2026, 3, 1), BigDecimal(10), BigDecimal.TEN, BigDecimal(2), BigDecimal.ONE),
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
