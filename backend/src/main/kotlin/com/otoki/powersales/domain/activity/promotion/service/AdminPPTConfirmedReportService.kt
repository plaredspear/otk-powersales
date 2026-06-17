package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PPTConfirmedReportItem
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTConfirmedReportResponse
import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 전문행사조 확정 인원 보고서 조회 + 엑셀 export (Spec #846).
 *
 * 레거시 매핑: SF Report `new_report_swJ` (전문행사조 확정 인원·Tabular 6컬럼·Confirmed__c=1·scope=organization).
 * 동작: isConfirmed=true 인 ProfessionalPromotionTeamMaster 를 전사 조회. employee/account 조인 결과를 6컬럼 행으로 매핑.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 차이: 기존 [AdminPPTMasterService] 목록(확정/미확정 전체·페이지네이션)과 별개 보고서 —
 *   확정만 + 전량 추출 + 엑셀. SF scope=organization = 전사.
 */
@Service
@Transactional(readOnly = true)
class AdminPPTConfirmedReportService(
    private val pptMasterRepository: PPTMasterRepository,
) {

    /**
     * 전문행사조 확정 인원 조회 (전사, isConfirmed=true).
     */
    fun getReport(): PPTConfirmedReportResponse {
        val masters = pptMasterRepository.findConfirmedReport()
        return PPTConfirmedReportResponse(masters.map { toItem(it) })
    }

    /**
     * 확정 인원 엑셀 export — 6컬럼 시트 (조회와 동일 필터).
     */
    fun exportReport(): ExcelResult {
        val response = getReport()

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("전문행사조확정인원")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headers = listOf("지점명", "성명", "사번", "거래처명", "거래처코드", "전문행사조")
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
            row.createCell(0).setCellValue(item.branchName ?: "")
            row.createCell(1).setCellValue(item.fullName ?: "")
            row.createCell(2).setCellValue(item.employeeNumber ?: "")
            row.createCell(3).setCellValue(item.accountName ?: "")
            row.createCell(4).setCellValue(item.accountCode ?: "")
            row.createCell(5).setCellValue(item.professionalPromotionTeam ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        return ExcelResult(bytes, "전문행사조확정인원.xlsx")
    }

    /** 전문행사조 마스터 1건 → 6컬럼 행. teamType 은 displayName(한국어). */
    private fun toItem(m: ProfessionalPromotionTeamMaster): PPTConfirmedReportItem {
        val emp = m.employee
        val acc = m.account
        return PPTConfirmedReportItem(
            // SF BranchName__c — 거래처 소재 지점명
            branchName = acc?.branchName,
            fullName = emp?.name,
            employeeNumber = emp?.employeeCode,
            accountName = acc?.name,
            // 기존 PPTMasterSearchResult 와 동일 — accountCode = account.externalKey
            accountCode = acc?.externalKey,
            professionalPromotionTeam = m.teamType.displayName,
        )
    }
}
