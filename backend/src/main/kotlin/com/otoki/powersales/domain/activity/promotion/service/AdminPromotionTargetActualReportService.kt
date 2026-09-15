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
import org.apache.poi.xssf.streaming.SXSSFWorkbook
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
 * 신규 차이: 기존 행사마스터 화면(PromotionController CRUD)과 별개 보고서 — ScheduleDate 기간 조회 +
 *   Summary 그룹/소계/차트 + 엑셀. SF scope=organization = 전사(영업지원실용, DataScope 미적용).
 *   상세 행 상한: 화면 [WEB_DISPLAY_ROW_LIMIT] / 엑셀 상한 없음(조회 조건 전량). 소계/합계/차트는 양쪽 모두 전량 기준.
 */
@Service
@Transactional(readOnly = true)
class AdminPromotionTargetActualReportService(
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
) {

    companion object {
        /**
         * 화면 표시 상세 행 상한 — 상한까지의 행만 SQL `LIMIT` 으로 조회한다.
         *
         * SF 리포트 화면 제한(2,000행) 을 그대로 이식했었으나, 브라우저가 그 규모의 표(행사명 그룹마다 테이블 1개,
         * 컬럼 23개)를 렌더하다 메인 스레드가 막혀 "페이지가 응답하지 않습니다" 가 뜨는 문제로 500행으로 낮췄다.
         * 소계/합계/차트/전체 행 수는 이 상한과 무관하게 항상 전량 기준 (DB 집계 쿼리로 분리). 전량은 엑셀 export.
         */
        const val WEB_DISPLAY_ROW_LIMIT = 500

        /** export 24컬럼 고정 폭(문자 수) — autoSizeColumn 은 전 행 실측이라 수만 행에서 timeout 주범이 되어 미사용. */
        private val EXPORT_COLUMN_WIDTHS = intArrayOf(
            20, 12, 20, 12, 18, 12, 16, 10, 14, 10, 16, 18, 12,
            14, 14, 12, 10, 14, 10, 14, 10, 10, 12, 20,
        )
    }

    /**
     * 행사사원 목표/실적 조회 — 행사명 그룹 + 소계 + 전체 합계 + 차트.
     *
     * startDate/endDate 필수 (미입력 시 IllegalArgumentException).
     * 지점 스코프: branchScope(행사사원의 사원 마스터 소속 지점 costCenterCode 기준)로 좁힘 — 전사 권한자 선택 지점/전건,
     * 지점 사용자 본인 지점(선택값 밖이면 IDOR 차단 = NoAccess → 빈 결과).
     * 상세 행은 [WEB_DISPLAY_ROW_LIMIT] 까지만 응답에 포함. 소계/합계/차트/전체 행 수는 전량 기준.
     */
    fun getReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
    ): PromotionTargetActualReportResponse = buildReport(startDate, endDate, branchScope, WEB_DISPLAY_ROW_LIMIT)

    /**
     * 조회 2회로 리포트를 조립한다 — 집계(전량, GROUP BY) + 상세(출력에 쓸 [detailRowLimit] 행만).
     *
     * 이전에는 조건에 맞는 전 행을 애플리케이션 메모리로 올려 `sumOf` 로 소계를 구하고 상세만 잘라 버렸다.
     * 소계/합계/차트가 전량 기준이어야 한다는 요구가 곧 "전량 조회" 를 강제한 구조였는데, 그 집계는 DB 가
     * `GROUP BY` 로 대신할 수 있으므로 분리했다. 이제 조회량은 `그룹 수 + detailRowLimit` 으로 묶인다.
     *
     * 상세를 앞에서부터 자르는 결과는 이전 in-memory 절단과 동일하다 — 그룹 키(promotionName = promotionNumber)가
     * 상세 정렬의 첫 키와 같아 "그룹 순서대로 채운 N행" 과 "정렬 순 앞 N행" 이 언제나 일치한다.
     *
     * [detailRowLimit] `null` = 상한 없음 (엑셀 export — 조회 조건 전량).
     */
    private fun buildReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
        detailRowLimit: Int?,
    ): PromotionTargetActualReportResponse {
        require(startDate != null && endDate != null) {
            "조회 기간(startDate, endDate)은 필수입니다"
        }

        // NoAccess(가시 지점 없음)는 repository 를 호출하지 않고 빈 결과 — IDOR 차단.
        val scopeCodes: List<String>? = when (branchScope) {
            is EffectiveBranchResult.All -> emptyList()
            is EffectiveBranchResult.Filtered -> branchScope.codes
            is EffectiveBranchResult.NoAccess -> null
        }
        val subtotals = scopeCodes
            ?.let { promotionEmployeeRepository.findTargetActualReportSubtotals(startDate, endDate, it) }
            .orEmpty()
        val details = scopeCodes
            ?.let { promotionEmployeeRepository.findTargetActualReport(startDate, endDate, it, detailRowLimit) }
            .orEmpty()

        // 상세는 그룹 순서대로 앞에서부터만 존재하므로, 상한에 걸린 뒤의 그룹은 rows 가 빈 채로 소계만 남는다.
        val detailRowsByPromotion = details.groupBy { it.promotionName }
        val groups = subtotals.map { subtotal ->
            PromotionTargetActualReportGroup(
                promotionName = subtotal.promotionName,
                subtotalTargetAmount = subtotal.targetAmount,
                subtotalActualAmount = subtotal.actualAmount,
                subtotalPrimaryQuantity = subtotal.primaryQuantity,
                subtotalPrimaryAmount = subtotal.primaryAmount,
                subtotalOtherQuantity = subtotal.otherQuantity,
                subtotalOtherAmount = subtotal.otherAmount,
                rows = detailRowsByPromotion[subtotal.promotionName].orEmpty().map { toRow(it) },
            )
        }

        val chart = groups.map { PromotionTargetActualChartItem(it.promotionName, it.subtotalActualAmount) }
        val totalRowCount = subtotals.sumOf { it.rowCount }
        val displayedRowCount = details.size

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
            totalRowCount = totalRowCount.toInt(),
            displayedRowCount = displayedRowCount,
            truncated = displayedRowCount < totalRowCount,
        )
    }

    /**
     * 목표/실적 엑셀 export — 행사명 그룹 헤더/소계 행 포함 24컬럼 + 전체 합계 행 (Summary 재현).
     *
     * 상세 행 **상한 없음** — 조회 조건에 해당하는 전량을 내려받는다. 화면은 [WEB_DISPLAY_ROW_LIMIT] 로 잘리므로
     * 전체 내역을 얻는 유일한 경로가 엑셀이고, 여기에 상한을 두면 그 경로가 막히기 때문이다(기간 제한은 레거시에도 없다).
     * 대신 조회 기간을 넓게 잡으면 조회량·생성 시간이 그만큼 늘어난다 — SXSSF 스트리밍 + 고정 컬럼 폭으로
     * 시트 생성 쪽 병목(XSSF 전량 메모리 + autoSizeColumn 전 행 실측)은 피하지만, 상세 행 자체는 메모리에 올라온다.
     */
    fun exportReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        branchScope: EffectiveBranchResult,
    ): ExcelResult {
        val response = buildReport(startDate, endDate, branchScope, detailRowLimit = null)

        val workbook = SXSSFWorkbook()
        val sheet = workbook.createSheet("행사사원목표대비실적")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headers = listOf(
            "행사명", "지점명", "거래처명", "거래처코드", "대표제품", "제품유형", "기타제품",
            "사번", "소속", "사원명", "전문행사조(현재)", "전문행사조(투입당시)", "행사일자",
            "목표금액", "실적금액", "매대위치", "대표수량", "대표금액", "기타수량", "기타금액",
            "근무구분2", "근무구분3", "근무보고여부", "출근일자",
        )
        EXPORT_COLUMN_WIDTHS.forEachIndexed { i, w -> sheet.setColumnWidth(i, w * 256) }

        // 상한이 없으므로 잘림 안내 행도 없다 — 1행이 항상 헤더.
        var rowIdx = 0
        val headerRow = sheet.createRow(rowIdx++)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, rowIdx)
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
