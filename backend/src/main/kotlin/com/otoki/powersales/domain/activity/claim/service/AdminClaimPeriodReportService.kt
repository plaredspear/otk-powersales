package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.response.ClaimPeriodReportItem
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimPeriodReportResponse
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.repository.AdminClaimRepository
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/** 기간별 클레임 보고서 대상 분류 — PACKAGING(포장불량만, claimType1=A) / ALL(모든 클레임). */
enum class ClaimPeriodReportType {
    PACKAGING,
    ALL,
}

/**
 * 기간별 클레임 보고서 조회 + 엑셀 export (Spec #843).
 *
 * 레거시 매핑: SF Report `X3_ONLY_veg` (포장불량만) + `X4_3xv` (모든 클레임).
 * 동작: ClaimDate 기간 + status='전송완료'(SENT) 클레임을 전사 조회. type=PACKAGING 면 claimType1=A,
 *       ALL 이면 전체(A/B/C). claim × employee × account × product 조인 결과를 행으로 매핑. 수량 합계 산출.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 차이: 기존 [AdminClaimService] 목록(createdAt 기준·페이지네이션·DataScope 없음)과 별개 보고서 —
 *   ClaimDate 기준 + 전량 추출 + claimType1 분기 + 엑셀. SF scope=organization = 전사 (기존 목록도 전사 정합).
 */
@Service
@Transactional(readOnly = true)
class AdminClaimPeriodReportService(
    private val adminClaimRepository: AdminClaimRepository,
) {

    /**
     * 기간별 클레임 조회.
     *
     * 기간 미지정 시 당월 1일~오늘. type 에 따라 claimType1 필터(PACKAGING=A / ALL=전체). 전사.
     */
    fun getReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        type: ClaimPeriodReportType,
    ): ClaimPeriodReportResponse {
        val today = LocalDate.now(TimeZones.SEOUL_ZONE)
        val start = startDate ?: today.withDayOfMonth(1)
        val end = endDate ?: today
        val claimType1 = if (type == ClaimPeriodReportType.PACKAGING) ClaimType1.A else null

        val claims = adminClaimRepository.findPeriodReport(start, end, claimType1)
        val items = claims.map { toItem(it) }
        val totalQuantity = claims.fold(BigDecimal.ZERO) { acc, c -> acc + (c.defectQuantity ?: BigDecimal.ZERO) }

        return ClaimPeriodReportResponse(
            startDate = start.toString(),
            endDate = end.toString(),
            type = type.name,
            totalQuantity = totalQuantity,
            items = items,
        )
    }

    /**
     * 기간별 클레임 엑셀 export — type 별 21/23컬럼 + 수량 합계 행.
     */
    fun exportReport(
        startDate: LocalDate?,
        endDate: LocalDate?,
        type: ClaimPeriodReportType,
    ): ExcelResult {
        val response = getReport(startDate, endDate, type)
        val isAll = type == ClaimPeriodReportType.ALL

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("기간별클레임")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        // 컬럼 순서: SF 보고서 순서 — 모든 클레임은 claimType1(4번째)/detailSnsName(10번째) 추가
        val headers = buildList {
            add("클레임번호"); add("인터페이스일시"); add("클레임일자")
            if (isAll) add("클레임대분류")
            add("지점명"); add("사번"); add("사원명"); add("연락처"); add("거래처명")
            if (isAll) add("상세SNS명")
            add("거래처외부키"); add("제품명"); add("제품코드"); add("제조일자"); add("유통기한")
            add("수량"); add("클레임소분류"); add("클레임내용"); add("상담번호")
            add("처리상태"); add("처리코드"); add("사유유형"); add("처리내용")
        }
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
            var c = 0
            row.createCell(c++).setCellValue(item.claimName ?: "")
            row.createCell(c++).setCellValue(item.interfaceDate ?: "")
            row.createCell(c++).setCellValue(item.claimDate ?: "")
            if (isAll) row.createCell(c++).setCellValue(item.claimType1 ?: "")
            row.createCell(c++).setCellValue(item.branchName ?: "")
            row.createCell(c++).setCellValue(item.employeeCode ?: "")
            row.createCell(c++).setCellValue(item.employeeName ?: "")
            row.createCell(c++).setCellValue(item.mobilePhone ?: "")
            row.createCell(c++).setCellValue(item.accountName ?: "")
            if (isAll) row.createCell(c++).setCellValue(item.detailSnsName ?: "")
            row.createCell(c++).setCellValue(item.externalKey ?: "")
            row.createCell(c++).setCellValue(item.productName ?: "")
            row.createCell(c++).setCellValue(item.productCode ?: "")
            row.createCell(c++).setCellValue(item.manufacturingDate ?: "")
            row.createCell(c++).setCellValue(item.expirationDate ?: "")
            row.createCell(c++).setCellValue(item.quantity?.toDouble() ?: 0.0)
            row.createCell(c++).setCellValue(item.claimType2 ?: "")
            row.createCell(c++).setCellValue(item.defectDescription ?: "")
            row.createCell(c++).setCellValue(item.counselNumber ?: "")
            row.createCell(c++).setCellValue(item.actionStatus ?: "")
            row.createCell(c++).setCellValue(item.actionCode ?: "")
            row.createCell(c++).setCellValue(item.reasonType ?: "")
            row.createCell(c).setCellValue(item.actContent ?: "")
        }

        // 합계 행 (수량 컬럼 총합)
        val totalRow = sheet.createRow(response.items.size + 1)
        totalRow.createCell(0).setCellValue("합계")
        val qtyColIndex = headers.indexOf("수량")
        totalRow.createCell(qtyColIndex).setCellValue(response.totalQuantity.toDouble())

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val typeLabel = if (isAll) "모든클레임" else "포장불량"
        val filename = "기간별클레임_%s_%s_%s.xlsx".format(typeLabel, response.startDate, response.endDate)
        return ExcelResult(bytes, filename)
    }

    /** 클레임 1건 → 보고서 행. enum 필드는 한국어 label/displayName. */
    private fun toItem(c: Claim): ClaimPeriodReportItem {
        val emp = c.employee
        val acc = c.account
        val prod = c.product
        return ClaimPeriodReportItem(
            claimName = c.name,
            interfaceDate = c.interfaceDate?.toString(),
            claimDate = c.date?.toString(),
            claimType1 = c.claimType1?.label,
            branchName = acc?.branchName,
            employeeCode = emp?.employeeCode,
            employeeName = emp?.name,
            mobilePhone = emp?.phone,
            accountName = acc?.name,
            detailSnsName = c.detailSnsName,
            externalKey = acc?.externalKey,
            productName = prod?.name,
            productCode = prod?.productCode,
            manufacturingDate = c.manufacturingDate?.toString(),
            expirationDate = c.expirationDate?.toString(),
            quantity = c.defectQuantity,
            claimType2 = c.claimType2?.label,
            defectDescription = c.defectDescription,
            counselNumber = c.counselNumber,
            actionStatus = c.actionStatus,
            actionCode = c.actionCode,
            reasonType = c.reasonType,
            actContent = c.actContent,
        )
    }
}
