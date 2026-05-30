package com.otoki.powersales.promotion.service

import com.otoki.powersales.promotion.dto.response.PromotionTargetActualChartItem
import com.otoki.powersales.promotion.dto.response.PromotionTargetActualReportGroup
import com.otoki.powersales.promotion.dto.response.PromotionTargetActualReportResponse
import com.otoki.powersales.promotion.dto.response.PromotionTargetActualReportRow
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 행사사원 목표 대비 실적 보고서 조회 + 엑셀 export (Spec #845).
 *
 * 레거시 매핑: SF Report `new_report_AtQ` (영업지원실용·Summary·도넛 차트·INTERVAL_CUSTOM·scope=organization).
 * 동작: ScheduleDate 기간 내 PromotionEmployee 를 전사 조회 (promotion/account/product/employee/teamMemberSchedule 조인).
 *       행사명(promotion.name) 그룹 + 그룹별 소계(목표/실적/수량 Sum) + 전체 합계 + 행사명별 실적금액 차트 데이터 산출.
 *       실적금액 = PromotionEmployee.dkDailyActualSalesAmount (SF formula 재현).
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

    /**
     * 행사사원 목표/실적 조회 — 행사명 그룹 + 소계 + 전체 합계 + 차트.
     *
     * startDate/endDate 필수 (미입력 시 IllegalArgumentException). 전사 조회.
     */
    fun getReport(startDate: LocalDate?, endDate: LocalDate?): PromotionTargetActualReportResponse {
        require(startDate != null && endDate != null) {
            "조회 기간(startDate, endDate)은 필수입니다"
        }

        val rows = promotionEmployeeRepository.findTargetActualReport(startDate, endDate)

        // 행사명 그룹핑 (SF Promotion.Name = promotionNumber. 조회 정렬이 promotionNumber asc 이므로 순서 보존)
        val grouped = rows.groupBy { it.promotion?.promotionNumber }
        val groups = grouped.map { (promotionName, pes) ->
            val mappedRows = pes.map { toRow(it) }
            PromotionTargetActualReportGroup(
                promotionName = promotionName,
                subtotalTargetAmount = pes.sumOf { it.targetAmount ?: 0L },
                subtotalActualAmount = pes.sumOf { it.dkDailyActualSalesAmount ?: BigDecimal.ZERO },
                subtotalPrimaryQuantity = pes.sumOf { it.primarySalesQuantity ?: BigDecimal.ZERO },
                subtotalOtherQuantity = pes.sumOf { it.otherSalesQuantity ?: BigDecimal.ZERO },
                rows = mappedRows,
            )
        }

        val chart = groups.map { PromotionTargetActualChartItem(it.promotionName, it.subtotalActualAmount) }

        return PromotionTargetActualReportResponse(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            groups = groups,
            totalTargetAmount = groups.sumOf { it.subtotalTargetAmount },
            totalActualAmount = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalActualAmount },
            totalPrimaryQuantity = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalPrimaryQuantity },
            totalOtherQuantity = groups.fold(BigDecimal.ZERO) { acc, g -> acc + g.subtotalOtherQuantity },
            chart = chart,
        )
    }

    /**
     * 목표/실적 엑셀 export — 행사명 그룹 헤더/소계 행 포함 23컬럼 + 전체 합계 행 (Summary 재현).
     */
    fun exportReport(startDate: LocalDate?, endDate: LocalDate?): ExcelResult {
        val response = getReport(startDate, endDate)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("행사사원목표대비실적")
        val headerStyle = createHeaderStyle(workbook)

        val headers = listOf(
            "행사명", "지점명", "거래처명", "거래처코드", "대표제품", "매대조건", "기타제품",
            "사번", "소속", "사원명", "전문행사조", "행사일자",
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
            // 그룹 소계 행
            val subtotalRow = sheet.createRow(rowIdx++)
            subtotalRow.createCell(0).setCellValue("[소계] ${group.promotionName ?: ""}")
            subtotalRow.createCell(12).setCellValue(group.subtotalTargetAmount.toDouble())
            subtotalRow.createCell(13).setCellValue(group.subtotalActualAmount.toDouble())
            subtotalRow.createCell(15).setCellValue(group.subtotalPrimaryQuantity.toDouble())
            subtotalRow.createCell(17).setCellValue(group.subtotalOtherQuantity.toDouble())
        }

        // 전체 합계 행
        val totalRow = sheet.createRow(rowIdx)
        totalRow.createCell(0).setCellValue("합계")
        totalRow.createCell(12).setCellValue(response.totalTargetAmount.toDouble())
        totalRow.createCell(13).setCellValue(response.totalActualAmount.toDouble())
        totalRow.createCell(15).setCellValue(response.totalPrimaryQuantity.toDouble())
        totalRow.createCell(17).setCellValue(response.totalOtherQuantity.toDouble())

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        val filename = "행사사원목표대비실적_%s_%s.xlsx".format(response.startDate, response.endDate)
        return ExcelResult(bytes, filename)
    }

    private fun writeRow(row: org.apache.poi.ss.usermodel.Row, item: PromotionTargetActualReportRow) {
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
        row.createCell(10).setCellValue(item.professionalPromotionTeam ?: "")
        row.createCell(11).setCellValue(item.scheduleDate ?: "")
        row.createCell(12).setCellValue((item.targetAmount ?: 0L).toDouble())
        row.createCell(13).setCellValue((item.actualAmount ?: BigDecimal.ZERO).toDouble())
        row.createCell(14).setCellValue(item.standLocation ?: "")
        row.createCell(15).setCellValue((item.primarySalesQuantity ?: BigDecimal.ZERO).toDouble())
        row.createCell(16).setCellValue((item.primaryProductAmount ?: BigDecimal.ZERO).toDouble())
        row.createCell(17).setCellValue((item.otherSalesQuantity ?: BigDecimal.ZERO).toDouble())
        row.createCell(18).setCellValue((item.otherSalesAmount ?: BigDecimal.ZERO).toDouble())
        row.createCell(19).setCellValue(item.workType2 ?: "")
        row.createCell(20).setCellValue(item.workType3 ?: "")
        row.createCell(21).setCellValue(item.isWorkReport ?: "")
        row.createCell(22).setCellValue(item.commuteDate ?: "")
    }

    /** PromotionEmployee 1건 → 23컬럼 행. enum 은 displayName, 실적금액은 SF formula 파생. */
    private fun toRow(pe: PromotionEmployee): PromotionTargetActualReportRow {
        val promo = pe.promotion
        val acc = promo?.account
        val emp = pe.employee
        val sch = pe.teamMemberSchedule
        return PromotionTargetActualReportRow(
            promotionName = promo?.promotionNumber,
            branchName = acc?.branchName,
            accountName = acc?.name,
            accountCode = acc?.branchCode,
            primaryProductName = promo?.primaryProduct?.name,
            category1 = promo?.category1,
            otherProduct = promo?.otherProduct,
            employeeCode = emp?.employeeCode,
            employeeOrgName = emp?.orgName,
            employeeName = emp?.name,
            professionalPromotionTeam = emp?.professionalPromotionTeam?.displayName,
            scheduleDate = pe.scheduleDate?.toString(),
            targetAmount = pe.targetAmount,
            // SF Formula DailyActualSalesAmount__c 재현 (의미 오류 포함 보존)
            actualAmount = pe.dkDailyActualSalesAmount,
            standLocation = promo?.standLocation?.displayName,
            primarySalesQuantity = pe.primarySalesQuantity,
            primaryProductAmount = pe.primaryProductAmount,
            otherSalesQuantity = pe.otherSalesQuantity,
            otherSalesAmount = pe.otherSalesAmount,
            workType2 = pe.dkWorkType2?.displayName,
            workType3 = pe.workType3?.displayName,
            // isWorkReport / commuteDate 는 TeamMemberSchedule 소유
            isWorkReport = sch?.isWorkReport,
            commuteDate = sch?.commuteDate?.toString(),
        )
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        setFillForegroundColor(XSSFColor(byteArrayOf(0x1E, 0x2F, 0x97.toByte()), null))
        fillPattern = FillPatternType.SOLID_FOREGROUND
        alignment = HorizontalAlignment.CENTER
        setFont(workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        })
    }

    private fun workbookToBytes(workbook: XSSFWorkbook): ByteArray {
        return ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }
    }

    data class ExcelResult(
        val bytes: ByteArray,
        val filename: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ExcelResult
            return bytes.contentEquals(other.bytes) && filename == other.filename
        }

        override fun hashCode(): Int = bytes.contentHashCode() * 31 + filename.hashCode()
    }
}
