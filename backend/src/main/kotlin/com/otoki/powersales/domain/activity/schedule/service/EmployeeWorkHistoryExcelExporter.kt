package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryItem
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * 근무기간 조회 월별 근무내역(개인) 엑셀 export — 목록 탭 컬럼 순서/라벨 정합.
 *
 * "유통형태" 는 SF 메타에 원천 필드가 없어 web 목록과 동일하게 "(TODO)" placeholder 로 둔다.
 */
@Component
class EmployeeWorkHistoryExcelExporter : BaseExcelExporter<EmployeeWorkHistoryItem>() {

    override val sheetName = "월별근무내역"
    override val defaultFilename = "월별근무내역.xlsx"

    override val headers = listOf(
        "근무일자", "구분", "근무지", "거래처코드", "유통형태", "거래처유형",
        "근무유형", "전문행사조", "근무형태", "상세유형", "출근", "근무시간",
    )

    override fun writeRow(row: Row, item: EmployeeWorkHistoryItem) {
        var col = 0
        row.createCell(col++).setCellValue(item.workingDate?.format(DATE_FMT) ?: "")
        row.createCell(col++).setCellValue(item.workingType ?: "")
        row.createCell(col++).setCellValue(resolveWorkplace(item))
        row.createCell(col++).setCellValue(item.accountExternalKey ?: "")
        row.createCell(col++).setCellValue("(TODO)")
        row.createCell(col++).setCellValue(item.accountType ?: "")
        row.createCell(col++).setCellValue(item.workingCategory1 ?: "")
        row.createCell(col++).setCellValue(item.professionalPromotionTeam ?: "")
        row.createCell(col++).setCellValue(item.workingCategory3 ?: "")
        row.createCell(col++).setCellValue(item.secondWorkType ?: "")
        row.createCell(col++).setCellValue(if (item.isClockIn) "출근" else "미등록")
        row.createCell(col).setCellValue(timeRange(item))
    }

    /** 근무지: 거래처명 → ref 텍스트 → 소속지점코드 순 폴백 (web resolveWorkplace 정합). */
    private fun resolveWorkplace(item: EmployeeWorkHistoryItem): String =
        item.accountName ?: item.refAccountName ?: item.costCenterCode ?: ""

    private fun timeRange(item: EmployeeWorkHistoryItem): String {
        if (item.startTime == null && item.completeTime == null) return ""
        val s = item.startTime?.format(TIME_FMT) ?: "··"
        val e = item.completeTime?.format(TIME_FMT) ?: "··"
        return "$s ~ $e"
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
