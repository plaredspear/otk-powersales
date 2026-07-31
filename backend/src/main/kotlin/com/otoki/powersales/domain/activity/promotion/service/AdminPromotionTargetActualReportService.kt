package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualChartItem
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualReportGroup
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualReportResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualReportRow
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
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
 *       목표금액 = dkDailyTargetAmount(목표갯수×기준단가), 실적금액 = dailyTotalActualSalesAmount(총 실적 = 대표금액+기타금액)
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

    /**
     * 행사사원 목표/실적 조회 — 행사명 그룹 + 소계 + 전체 합계 + 차트.
     *
     * startDate/endDate 필수 (미입력 시 IllegalArgumentException).
     * 지점 스코프: branchScope(여사원일정 소속 지점 costCenterCode 기준)로 좁힘 — 전사 권한자 선택 지점/전건,
     * 지점 사용자 본인 지점(선택값 밖이면 IDOR 차단 = NoAccess → 빈 결과).
     */
    fun getReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
    ): PromotionTargetActualReportResponse {
        require(startDate != null && endDate != null) {
            "조회 기간(startDate, endDate)은 필수입니다"
        }

        val rows = when (branchScope) {
            is EffectiveBranchResult.All -> promotionEmployeeRepository.findTargetActualReport(startDate, endDate, emptyList())
            is EffectiveBranchResult.Filtered ->
                promotionEmployeeRepository.findTargetActualReport(startDate, endDate, branchScope.codes)
            is EffectiveBranchResult.NoAccess -> emptyList()
        }

        // 행사명 그룹핑 (SF Promotion.Name = promotionNumber. 조회 정렬이 promotionNumber asc 이므로 순서 보존)
        val grouped = rows.groupBy { it.promotion?.promotionNumber }
        val groups = grouped.map { (promotionName, pes) ->
            val mappedRows = pes.map { toRow(it) }
            PromotionTargetActualReportGroup(
                promotionName = promotionName,
                subtotalTargetAmount = pes.sumOf { it.dkDailyTargetAmount ?: BigDecimal.ZERO },
                subtotalActualAmount = pes.sumOf { it.dailyTotalActualSalesAmount ?: BigDecimal.ZERO },
                subtotalPrimaryQuantity = pes.sumOf { it.primarySalesQuantity ?: BigDecimal.ZERO },
                subtotalPrimaryAmount = pes.sumOf { it.primaryProductAmount ?: BigDecimal.ZERO },
                subtotalOtherQuantity = pes.sumOf { it.otherSalesQuantity ?: BigDecimal.ZERO },
                subtotalOtherAmount = pes.sumOf { it.otherSalesAmount ?: BigDecimal.ZERO },
                rows = mappedRows,
            )
        }

        val chart = groups.map { PromotionTargetActualChartItem(it.promotionName, it.subtotalActualAmount) }

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
        )
    }

    /**
     * 목표/실적 엑셀 export — 행사명 그룹 헤더/소계 행 포함 24컬럼 + 전체 합계 행 (Summary 재현).
     */
    fun exportReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
    ): ExcelResult {
        val response = getReport(startDate, endDate, branchScope)

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

    /** PromotionEmployee 1건 → 23컬럼 행. enum 은 displayName, 목표/실적 금액은 SF Report 컬럼 formula 파생. */
    private fun toRow(pe: PromotionEmployee): PromotionTargetActualReportRow {
        val promo = pe.promotion
        val acc = promo?.account
        val emp = pe.employee
        val sch = pe.teamMemberSchedule
        return PromotionTargetActualReportRow(
            promotionName = promo?.promotionNumber,
            branchName = acc?.branchName,
            accountName = acc?.name,
            // SF AccCode__c = AccId__r.ExternalKey__c (SAP 거래처코드)
            accountCode = acc?.externalKey,
            primaryProductName = promo?.primaryProduct?.name,
            category1 = promo?.category1,
            otherProduct = promo?.otherProduct,
            employeeCode = emp?.employeeCode,
            employeeOrgName = emp?.orgName,
            employeeName = emp?.name,
            // SF(임철민팀장용 변형) 전문행사조(현재) = 사원 마스터의 현재 소속 조
            professionalPromotionTeamCurrent = emp?.professionalPromotionTeam?.displayName,
            // SF(영업지원실용) 전문행사조 컬럼 = 조원일정에 기록된 투입 당시 값
            professionalPromotionTeam = sch?.professionalPromotionTeam,
            scheduleDate = pe.scheduleDate?.toString(),
            // SF Report 목표금액 컬럼 = DKRetail__DailyTargetAmount__c formula (목표갯수×기준단가)
            targetAmount = pe.dkDailyTargetAmount,
            // SF Report 총 실적 컬럼 = DailyActualSalesAmount__c formula (대표금액+기타금액)
            actualAmount = pe.dailyTotalActualSalesAmount,
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
}
