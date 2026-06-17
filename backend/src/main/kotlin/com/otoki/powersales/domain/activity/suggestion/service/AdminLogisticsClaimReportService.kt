package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.dto.response.LogisticsClaimReportItem
import com.otoki.powersales.domain.activity.suggestion.dto.response.LogisticsClaimReportResponse
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 물류 클레임 보고서 기간 프리셋 — THIS_MONTH(당월) / LAST_MONTH(전월) / CUSTOM(기간별 직접 입력). */
enum class LogisticsClaimReportPeriod {
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM,
}

/**
 * (영업본부) 물류 클레임 보고서 조회 + 엑셀 export (Spec #844).
 *
 * 레거시 매핑: SF Report `OLS_dmK`(기간별) + `new_report_6dy`(당월) + `OLS_NDx`(전월).
 * 동작: period 별 기간 산출(당월/전월/사용자 지정) 후 category='물류 클레임' + claimDate 범위 Suggestion 을 전사 조회.
 *       suggestion × employee × account × product × owner 조인 결과를 22컬럼 행으로 매핑. claimDate 내림차순.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 차이: 기존 [AdminSuggestionService] 목록(DataScope·페이지네이션)과 별개 보고서 —
 *   전사 고정(SF scope=organization, "(영업본부)") + category 고정 + 기간 프리셋 + 엑셀.
 *   SF WERK1_TX/WERK3_TX 의 'contains 빈값' 필터는 항상 참(no-op)이라 미구현.
 */
@Service
@Transactional(readOnly = true)
class AdminLogisticsClaimReportService(
    private val suggestionRepository: SuggestionRepository,
) {

    /**
     * 물류 클레임 조회.
     *
     * period=THIS_MONTH/LAST_MONTH 면 서버가 기간 산출(start/end 인자 무시), CUSTOM 이면 start/end 필수(미입력 시 예외).
     */
    fun getReport(
        period: LogisticsClaimReportPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): LogisticsClaimReportResponse {
        val (start, end) = resolvePeriod(period, startDate, endDate)
        val suggestions = suggestionRepository.findLogisticsClaimReport(start, end)
        val items = suggestions.map { toItem(it) }
        return LogisticsClaimReportResponse(period.name, start.toString(), end.toString(), items)
    }

    /**
     * 물류 클레임 엑셀 export — 22컬럼 시트 (조회와 동일 필터).
     */
    fun exportReport(
        period: LogisticsClaimReportPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): ExcelResult {
        val response = getReport(period, startDate, endDate)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("물류클레임")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headers = listOf(
            "소유자명", "생성일시", "클레임일자", "책임물류센터", "물류책임", "클레임유형",
            "제목", "내용", "제품코드", "제품명", "제품카테고리", "지점명", "거래처코드", "거래처명",
            "조직명", "사번", "사원명", "직위", "직무코드", "차량번호", "처리상태", "처리내용", "중복제안번호",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        response.items.forEachIndexed { idx, item ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(item.custName ?: "")
            row.createCell(1).setCellValue(item.createdDate ?: "")
            row.createCell(2).setCellValue(item.claimDate ?: "")
            row.createCell(3).setCellValue(item.responsibleLogisticsCenter ?: "")
            row.createCell(4).setCellValue(item.logisticsResponsibility ?: "")
            row.createCell(5).setCellValue(item.claimType ?: "")
            row.createCell(6).setCellValue(item.title ?: "")
            row.createCell(7).setCellValue(item.content ?: "")
            row.createCell(8).setCellValue(item.productCode ?: "")
            row.createCell(9).setCellValue(item.productName ?: "")
            row.createCell(10).setCellValue(item.productCategory ?: "")
            row.createCell(11).setCellValue(item.branchName ?: "")
            row.createCell(12).setCellValue(item.accountCode ?: "")
            row.createCell(13).setCellValue(item.accountName ?: "")
            row.createCell(14).setCellValue(item.orgName ?: "")
            row.createCell(15).setCellValue(item.employeeCode ?: "")
            row.createCell(16).setCellValue(item.employeeName ?: "")
            row.createCell(17).setCellValue(item.jikwee ?: "")
            row.createCell(18).setCellValue(item.jobCode ?: "")
            row.createCell(19).setCellValue(item.carNumber ?: "")
            row.createCell(20).setCellValue(item.actionStatus ?: "")
            row.createCell(21).setCellValue(item.actionContent ?: "")
            row.createCell(22).setCellValue(item.duplicateProposalNum ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val periodLabel = when (response.period) {
            "THIS_MONTH" -> "당월"
            "LAST_MONTH" -> "전월"
            else -> "기간별"
        }
        val filename = "물류클레임보고서_%s_%s_%s.xlsx".format(periodLabel, response.startDate, response.endDate)
        return ExcelResult(bytes, filename)
    }

    /** period → [start, end] 산출. CUSTOM 은 입력 필수, THIS/LAST_MONTH 는 KST 기준 월 경계. */
    private fun resolvePeriod(
        period: LogisticsClaimReportPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now(TimeZones.SEOUL_ZONE)
        return when (period) {
            LogisticsClaimReportPeriod.THIS_MONTH ->
                today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
            LogisticsClaimReportPeriod.LAST_MONTH -> {
                val lastMonth = today.minusMonths(1)
                lastMonth.withDayOfMonth(1) to lastMonth.with(TemporalAdjusters.lastDayOfMonth())
            }
            LogisticsClaimReportPeriod.CUSTOM -> {
                require(startDate != null && endDate != null) {
                    "기간별(CUSTOM) 조회는 startDate, endDate 가 필수입니다"
                }
                startDate to endDate
            }
        }
    }

    /** 제안 1건 → 22컬럼 행 (CUST_NAME = owner User 이름). */
    private fun toItem(s: Suggestion): LogisticsClaimReportItem {
        val emp = s.employee
        val acc = s.account
        val prod = s.product
        return LogisticsClaimReportItem(
            // SF CUST_NAME 의사 컬럼 = 레코드 Owner
            custName = s.ownerUser?.name,
            createdDate = s.createdAt?.toString(),
            claimDate = s.claimDate?.toString(),
            responsibleLogisticsCenter = s.responsibleLogisticsCenter,
            logisticsResponsibility = s.logisticsResponsibility,
            claimType = s.claimType,
            title = s.title,
            content = s.content,
            productCode = prod?.productCode,
            productName = prod?.name,
            productCategory = prod?.productCategory1,
            branchName = acc?.branchName,
            accountCode = acc?.externalKey,
            accountName = acc?.name,
            orgName = emp?.orgName,
            employeeCode = emp?.employeeCode,
            employeeName = emp?.name,
            jikwee = emp?.jikwee,
            jobCode = emp?.jobCode,
            carNumber = s.carNumber,
            actionStatus = s.actionStatus?.displayName,
            actionContent = s.actionContent,
            duplicateProposalNum = s.duplicateProposalNum,
        )
    }
}
