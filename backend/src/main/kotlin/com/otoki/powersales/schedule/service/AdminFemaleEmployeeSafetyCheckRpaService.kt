package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.util.TimeZones
import com.otoki.powersales.schedule.dto.response.FemaleEmployeeSafetyCheckRpaItem
import com.otoki.powersales.schedule.dto.response.FemaleEmployeeSafetyCheckRpaResponse
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * 판매여사원 일일 안전점검 현황 (RPA용) 보고서 조회 + 엑셀 export.
 *
 * 레거시 매핑: SF Report `X00/new_report_xdB` (RPA용·scope=organization·24컬럼 CUST_NAME).
 * 동작: 지정 일자(미지정 시 어제 KST)의 `TeamMemberSchedule` 안전점검 완료 건을 전사 조회
 *       (traversalFlag='O' + yesChkCnt≠null). employee/account/owner 조인 결과를 24컬럼 행으로 매핑. 근무구분1 오름차순.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 차이: [AdminFemaleEmployeeSafetyCheckReportService] (#841) 와 데이터 소스/필터 동일하되,
 *   (a) 전사 고정 (DataScope 미적용 — SF scope=organization, RPA = 전사 전용),
 *   (b) 마지막 컬럼 CommuteDate 대신 CUST_NAME = 레코드 Owner User 이름.
 * checkTime 은 startTime 직접 (#841 동일 — 레거시 `StartTime - 9/24` KST 보정 신규 미적용).
 */
@Service
@Transactional(readOnly = true)
class AdminFemaleEmployeeSafetyCheckRpaService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    /**
     * RPA용 일일 안전점검 현황 조회 (전사 고정).
     *
     * date 미지정 시 어제(KST). traversalFlag='O' + yesChkCnt≠null (점검 완료) 필터. 지점 스코프 없음.
     */
    fun getReport(date: LocalDate?): FemaleEmployeeSafetyCheckRpaResponse {
        val targetDate = date ?: LocalDate.now(TimeZones.SEOUL_ZONE).minusDays(1)
        val schedules = teamMemberScheduleRepository.findSafetyCheckReportRpa(targetDate)
        val items = schedules.map { toItem(it) }
        return FemaleEmployeeSafetyCheckRpaResponse(targetDate.toString(), items)
    }

    /**
     * RPA용 안전점검 현황 엑셀 export — 24컬럼 시트 (조회와 동일 필터).
     */
    fun exportReport(date: LocalDate?): ExcelResult {
        val response = getReport(date)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("판매여사원안전점검RPA")
        val headerStyle = createHeaderStyle(workbook)

        val headers = listOf(
            "사번", "여사원명", "소속", "거래처유형", "거래처코드", "거래처명", "근무구분1",
            "점검시간", "근무보고여부", "HR코드",
            "점검1", "점검2", "점검3", "점검4", "점검5", "점검6", "점검7", "점검8", "점검9",
            "주의사항", "주의확인", "근무구분2", "근무구분3", "부근무유형", "소유자명",
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
            row.createCell(0).setCellValue(item.employeeCode)
            row.createCell(1).setCellValue(item.ladyName)
            row.createCell(2).setCellValue(item.employeeOrgName ?: "")
            row.createCell(3).setCellValue(item.accountType ?: "")
            row.createCell(4).setCellValue(item.accountBranchCode ?: "")
            row.createCell(5).setCellValue(item.accountName ?: "")
            row.createCell(6).setCellValue(item.workingCategory1 ?: "")
            row.createCell(7).setCellValue(item.checkTime ?: "")
            row.createCell(8).setCellValue(item.isWorkReport ?: "")
            row.createCell(9).setCellValue(item.hrCode ?: "")
            row.createCell(10).setCellValue(item.equipment1 ?: "")
            row.createCell(11).setCellValue(item.equipment2 ?: "")
            row.createCell(12).setCellValue(item.equipment3 ?: "")
            row.createCell(13).setCellValue(item.equipment4 ?: "")
            row.createCell(14).setCellValue(item.equipment5 ?: "")
            row.createCell(15).setCellValue(item.equipment6 ?: "")
            row.createCell(16).setCellValue(item.equipment7 ?: "")
            row.createCell(17).setCellValue(item.equipment8 ?: "")
            row.createCell(18).setCellValue(item.equipment9 ?: "")
            row.createCell(19).setCellValue(item.precaution ?: "")
            row.createCell(20).setCellValue(item.precautionChk?.toString() ?: "")
            row.createCell(21).setCellValue(item.workingCategory2 ?: "")
            row.createCell(22).setCellValue(item.workingCategory3 ?: "")
            row.createCell(23).setCellValue(item.secondWorkType ?: "")
            row.createCell(24).setCellValue(item.custName ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        val filename = "판매여사원안전점검RPA_%s.xlsx".format(response.date)
        return ExcelResult(bytes, filename)
    }

    /** 여사원일정 1건 → 24컬럼 행 (CUST_NAME = owner User 이름). enum 필드는 displayName 직렬화. */
    private fun toItem(s: TeamMemberSchedule): FemaleEmployeeSafetyCheckRpaItem {
        val emp = s.employee
        val acc = s.account
        return FemaleEmployeeSafetyCheckRpaItem(
            employeeCode = emp?.employeeCode ?: "",
            ladyName = emp?.name ?: "",
            employeeOrgName = emp?.orgName,
            accountType = acc?.accountType?.displayName,
            accountBranchCode = acc?.branchCode,
            accountName = acc?.name,
            workingCategory1 = s.workingCategory1?.displayName,
            // 레거시 StartTime - 9/24 의 KST 보정은 신규 미적용 (#841 동일)
            checkTime = s.startTime?.toString(),
            isWorkReport = s.isWorkReport,
            hrCode = s.hrCode,
            equipment1 = s.equipment1,
            equipment2 = s.equipment2,
            equipment3 = s.equipment3,
            equipment4 = s.equipment4,
            equipment5 = s.equipment5,
            equipment6 = s.equipment6,
            equipment7 = s.equipment7,
            equipment8 = s.equipment8,
            equipment9 = s.equipment9,
            precaution = s.precaution,
            precautionChk = s.precautionChk,
            workingCategory2 = s.workingCategory2?.displayName,
            workingCategory3 = s.workingCategory3?.displayName,
            secondWorkType = s.secondWorkType,
            // SF CUST_NAME 의사 컬럼 = 레코드 Owner. ownerUser 이름.
            custName = s.ownerUser?.name,
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
