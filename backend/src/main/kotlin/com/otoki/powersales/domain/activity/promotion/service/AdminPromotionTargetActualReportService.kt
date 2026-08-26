package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualChartItem
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualReportGroup
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualReportResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualReportRow
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionTargetActualReportRecord
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 행사사원 목표 대비 실적 보고서 조회 + 엑셀 export (Spec #845).
 *
 * 레거시 매핑: SF Report `new_report_AtQ` (영업지원실용·Summary·도넛 차트·INTERVAL_CUSTOM·scope=organization).
 * 동작: ScheduleDate 기간 내 PromotionEmployee 를 전사 조회 (promotion/account/product/employee/teamMemberSchedule 조인).
 *       행사명(promotion.name) 그룹 + 그룹별 소계(목표/실적/수량 Sum) + 전체 합계 + 행사명별 실적금액 차트 데이터 산출.
 *       목표금액 = 목표갯수×기준단가, 실적금액 = 총 실적(대표금액+기타금액)
 *       — SF Report 컬럼(DailyTargetAmount__c/DailyActualSalesAmount__c) formula 재현.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 차이: 기존 행사마스터 화면(PromotionController CRUD)과 별개 보고서 — ScheduleDate 기간 + 전량 추출 +
 *   Summary 그룹/소계/차트 + 엑셀. SF scope=organization = 전사(영업지원실용, DataScope 미적용).
 */
@Service
@Transactional(readOnly = true)
class AdminPromotionTargetActualReportService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
) {

    companion object {
        /**
         * 화면 표시 상세 행 상한 — SF 리포트 실행 화면의 플랫폼 고정 표시 제한(2,000행) 정합.
         * 소계/합계/차트는 전량 기준으로 산출하고 상세 행만 앞에서부터 상한까지 내려준다. 전량은 엑셀 export.
         */
        const val WEB_DISPLAY_ROW_LIMIT = 2_000
    }

    /**
     * 행사사원 목표/실적 조회 — 행사명 그룹 + 소계 + 전체 합계 + 차트.
     *
     * startDate/endDate 필수 (미입력 시 IllegalArgumentException).
     * 지점 스코프: branchScope(여사원일정 소속 지점 costCenterCode 기준)로 좁힘 — 전사 권한자 선택 지점/전건,
     * 지점 사용자 본인 지점(선택값 밖이면 IDOR 차단 = NoAccess → 빈 결과).
     * 상세 행은 [WEB_DISPLAY_ROW_LIMIT] 까지만 응답에 포함 (SF 리포트 화면 2,000행 표시 제한 정합).
     */
    fun getReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
    ): PromotionTargetActualReportResponse = buildReport(startDate, endDate, branchScope, WEB_DISPLAY_ROW_LIMIT)

    private fun buildReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
        displayRowLimit: Int?,
    ): PromotionTargetActualReportResponse {
        require(startDate != null && endDate != null) {
            "조회 기간(startDate, endDate)은 필수입니다"
        }

        val records = when (branchScope) {
            is EffectiveBranchResult.All -> promotionEmployeeRepository.findTargetActualReport(startDate, endDate, emptyList())
            is EffectiveBranchResult.Filtered ->
                promotionEmployeeRepository.findTargetActualReport(startDate, endDate, branchScope.codes)
            is EffectiveBranchResult.NoAccess -> emptyList()
        }

        // 행사명 그룹핑 (SF Promotion.Name = promotionNumber. 조회 정렬이 promotionNumber asc 이므로 순서 보존)
        // 소계/합계/차트는 전량 기준, 상세 행만 표시 상한까지 그룹 순서대로 채운다 (SF 리포트 표시 제한 동작 정합).
        var remaining = displayRowLimit ?: Int.MAX_VALUE
        val grouped = records.groupBy { it.promotionName }
        val groups = grouped.map { (promotionName, recs) ->
            val visible = if (recs.size <= remaining) recs else recs.subList(0, remaining)
            remaining -= visible.size
            PromotionTargetActualReportGroup(
                promotionName = promotionName,
                subtotalTargetAmount = recs.sumOf { it.targetAmount ?: BigDecimal.ZERO },
                subtotalActualAmount = recs.sumOf { it.actualAmount ?: BigDecimal.ZERO },
                subtotalPrimaryQuantity = recs.sumOf { it.primarySalesQuantity ?: BigDecimal.ZERO },
                subtotalPrimaryAmount = recs.sumOf { it.primaryProductAmount ?: BigDecimal.ZERO },
                subtotalOtherQuantity = recs.sumOf { it.otherSalesQuantity ?: BigDecimal.ZERO },
                subtotalOtherAmount = recs.sumOf { it.otherSalesAmount ?: BigDecimal.ZERO },
                rows = visible.map { toRow(it) },
            )
        }

        val chart = groups.map { PromotionTargetActualChartItem(it.promotionName, it.subtotalActualAmount) }
        val displayedRowCount = groups.sumOf { it.rows.size }

        return PromotionTargetActualReportResponse(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            groups = groups,
            totalTargetAmount = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalTargetAmount },
            totalActualAmount = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalActualAmount },
            totalPrimaryQuantity = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalPrimaryQuantity },
            totalPrimaryAmount = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalPrimaryAmount },
            totalOtherQuantity = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalOtherQuantity },
            totalOtherAmount = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalOtherAmount },
            chart = chart,
            totalRowCount = records.size,
            displayedRowCount = displayedRowCount,
            truncated = displayedRowCount < records.size,
        )
    }

    /**
     * 목표/실적 엑셀 export — 행사명 그룹 헤더/소계 행 포함 24컬럼 + 전체 합계 행 (Summary 재현).
     * 화면 표시 상한과 무관하게 전량 추출.
     */
    fun exportReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
    ): ExcelResult {
        val response = buildReport(startDate, endDate, branchScope, null)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("행사사원목표대비실적")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headers = listOf(
            "행사명", "지점명", "거래처명", "거래처코드", "대표제품", "제품유형", "기타제품",
            "사번", "소속", "사원명", "전문행사조(현재)", "전문행사조(투입당시)", "행사일자",
            "목표금액", "실적금액", "매대위치", "대표수량", "대표금액", "기타수량", "기타금액",
            "근무구분2", "근무구분3", "근무보고여부", "출근일자",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        var rowIdx = 1
        response.groups.forEach { group ->
            group.rows.forEach { item ->
                val row = sheet.createRow(rowIdx++)
                writeRow(row, item)
            }
            // 그룹 소계 행 (SF Summary Sum 6종)
            val subtotalRow = sheet.createRow(rowIdx++)
            subtotalRow.createCell(0).setCellValue("[소계] ${group.promotionName ?: ""}")
            subtotalRow.createCell(13).setCellValue(group.subtotalTargetAmount.toDouble())
            subtotalRow.createCell(14).setCellValue(group.subtotalActualAmount.toDouble())
            subtotalRow.createCell(16).setCellValue(group.subtotalPrimaryQuantity.toDouble())
            subtotalRow.createCell(17).setCellValue(group.subtotalPrimaryAmount.toDouble())
            subtotalRow.createCell(18).setCellValue(group.subtotalOtherQuantity.toDouble())
            subtotalRow.createCell(19).setCellValue(group.subtotalOtherAmount.toDouble())
        }

        // 전체 합계 행
        val totalRow = sheet.createRow(rowIdx)
        totalRow.createCell(0).setCellValue("합계")
        totalRow.createCell(13).setCellValue(response.totalTargetAmount.toDouble())
        totalRow.createCell(14).setCellValue(response.totalActualAmount.toDouble())
        totalRow.createCell(16).setCellValue(response.totalPrimaryQuantity.toDouble())
        totalRow.createCell(17).setCellValue(response.totalPrimaryAmount.toDouble())
        totalRow.createCell(18).setCellValue(response.totalOtherQuantity.toDouble())
        totalRow.createCell(19).setCellValue(response.totalOtherAmount.toDouble())

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val filename = "행사사원목표대비실적_%s_%s.xlsx".format(response.startDate, response.endDate)
        return ExcelResult(bytes, filename)
    }

    private fun writeRow(row: Row, item: PromotionTargetActualReportRow) {
        row.createCell(0).setCellValue(item.promotionName ?: "")
        row.createCell(1).setCellValue(item.branchName ?: "")
        row.createCell(2).setCellValue(item.accountName ?: "")
        row.createCell(3).setCellValue(item.accountCode ?: "")
        row.createCell(4).setCellValue(item.primaryProductName ?: "")
        row.createCell(5).setCellValue(item.category1 ?: "")
        row.createCell(6).setCellValue(item.otherProduct ?: "")
        row.createCell(7).setCellValue(item.employeeCode ?: "")
        row.createCell(8).setCellValue(item.employeeOrgName ?: "")
        row.createCell(9).setCellValue(item.employeeName ?: "")
        row.createCell(10).setCellValue(item.professionalPromotionTeamCurrent ?: "")
        row.createCell(11).setCellValue(item.professionalPromotionTeam ?: "")
        row.createCell(12).setCellValue(item.scheduleDate ?: "")
        row.createCell(13).setCellValue((item.targetAmount ?: BigDecimal.ZERO).toDouble())
        row.createCell(14).setCellValue((item.actualAmount ?: BigDecimal.ZERO).toDouble())
        row.createCell(15).setCellValue(item.standLocation ?: "")
        row.createCell(16).setCellValue((item.primarySalesQuantity ?: BigDecimal.ZERO).toDouble())
        row.createCell(17).setCellValue((item.primaryProductAmount ?: BigDecimal.ZERO).toDouble())
        row.createCell(18).setCellValue((item.otherSalesQuantity ?: BigDecimal.ZERO).toDouble())
        row.createCell(19).setCellValue((item.otherSalesAmount ?: BigDecimal.ZERO).toDouble())
        row.createCell(20).setCellValue(item.workType2 ?: "")
        row.createCell(21).setCellValue(item.workType3 ?: "")
        row.createCell(22).setCellValue(item.isWorkReport ?: "")
        row.createCell(23).setCellValue(item.commuteDate ?: "")
    }

    /** projection record 1건 → 23컬럼 행. enum 은 displayName, 목표/실적 금액은 SF Report 컬럼 formula 파생. */
    private fun toRow(rec: PromotionTargetActualReportRecord): PromotionTargetActualReportRow {
        return PromotionTargetActualReportRow(
            promotionName = rec.promotionName,
            branchName = rec.branchName,
            accountName = rec.accountName,
            accountCode = rec.accountCode,
            primaryProductName = rec.primaryProductName,
            category1 = rec.category1,
            otherProduct = rec.otherProduct,
            employeeCode = rec.employeeCode,
            employeeOrgName = rec.employeeOrgName,
            employeeName = rec.employeeName,
            professionalPromotionTeamCurrent = rec.professionalPromotionTeamCurrent?.displayName,
            professionalPromotionTeam = rec.professionalPromotionTeam,
            scheduleDate = rec.scheduleDate?.toString(),
            targetAmount = rec.targetAmount,
            actualAmount = rec.actualAmount,
            standLocation = rec.standLocation?.displayName,
            primarySalesQuantity = rec.primarySalesQuantity,
            primaryProductAmount = rec.primaryProductAmount,
            otherSalesQuantity = rec.otherSalesQuantity,
            otherSalesAmount = rec.otherSalesAmount,
            workType2 = rec.workType2?.displayName,
            workType3 = rec.workType3?.displayName,
            isWorkReport = rec.isWorkReport,
            commuteDate = rec.commuteDate?.toString(),
        )
    }
}
