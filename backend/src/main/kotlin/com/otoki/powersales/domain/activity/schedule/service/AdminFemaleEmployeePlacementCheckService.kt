package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.activity.schedule.dto.response.FemaleEmployeePlacementCheckItem
import com.otoki.powersales.domain.activity.schedule.dto.response.FemaleEmployeePlacementCheckResponse
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 여사원 배치 점검 현황 — 영업지원실용 월간 배치 점검 조회 + 엑셀 export.
 *
 * 레거시 매핑: SF Report `InternalSalesReportFolder/new_report_4Ic`
 * (여사원 배치 점검 퇴직자 포함 (영업지원실 용) 상시_임시(조장포함)). Tabular — 여사원일정 행 단위 나열.
 * 동작: year/month 를 해당 월 1일~말일로 환산하여 `TeamMemberSchedule` 을 조회 (근무유형='근무', 앱권한 여사원/조장,
 *       더미 사원명 제외, 퇴직자 포함 = status 필터 없음). employee/account 조인 결과를 21컬럼 행으로 매핑.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 도입 — 레거시 SF Report 의 web admin 이식. 레거시 하드코딩 날짜는 year/month 검색 조건으로 전환 (Spec #839 Q1).
 * 나이(`Age__c`) / 근속연수(`yearsOfService__c`) 는 SF formula 필드이므로 birthDate/startDate 기반 계산으로 대체.
 */
@Service
@Transactional(readOnly = true)
class AdminFemaleEmployeePlacementCheckService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    /**
     * 여사원 배치 점검 조회.
     *
     * year/month 검증 (2020~2099 / 1~12) 후 해당 월 [1일, 말일] 로 환산하여 조회. 권한: scope.isAllBranches 면
     * 사용자 입력 그대로, 아니면 scope.branchCodes 와 교집합 (empty → 403). 조회 결과를 orgName/employeeCode/workingDate 정렬.
     */
    fun getPlacementCheck(
        scope: DataScope,
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
    ): FemaleEmployeePlacementCheckResponse {
        validateParams(year, month)
        val yearMonth = YearMonth.of(year, month)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()
        val effectiveCodes = applyScope(scope, costCenterCodes)

        val schedules = teamMemberScheduleRepository.findPlacementCheck(
            from = from,
            to = to,
            roles = listOf(AppAuthority.WOMAN, AppAuthority.LEADER),
            branchCodes = effectiveCodes,
        )

        val items = schedules
            .map { toItem(it, to) }
            .sortedWith(
                compareBy(
                    { it.orgName ?: "" },
                    { it.employeeCode },
                    { it.workingDate ?: "" },
                )
            )
        return FemaleEmployeePlacementCheckResponse(year, month, items)
    }

    /**
     * 배치 점검 엑셀 export — 21컬럼 시트 (조회와 동일 필터/스코프).
     */
    fun exportPlacementCheck(
        scope: DataScope,
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
    ): ExcelResult {
        val response = getPlacementCheck(scope, year, month, costCenterCodes)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("여사원배치점검")
        val headerStyle = createHeaderStyle(workbook)

        val headers = listOf(
            "근무일자", "소속", "사번", "직위", "성명", "전문행사조", "재직상태",
            "거래처유형", "거래처명", "거래처코드", "거래처지점명",
            "근무구분1", "근무구분2", "근무구분3", "부근무유형", "근무구분5",
            "출근일자", "근무보고여부", "입사일", "나이", "근속연수",
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
            row.createCell(0).setCellValue(item.workingDate ?: "")
            row.createCell(1).setCellValue(item.orgName ?: "")
            row.createCell(2).setCellValue(item.employeeCode)
            row.createCell(3).setCellValue(item.jikwee ?: "")
            row.createCell(4).setCellValue(item.name)
            row.createCell(5).setCellValue(item.professionalPromotionTeam ?: "")
            row.createCell(6).setCellValue(item.employmentStatus ?: "")
            row.createCell(7).setCellValue(item.accountType ?: "")
            row.createCell(8).setCellValue(item.accountName ?: "")
            row.createCell(9).setCellValue(item.accountBranchCode ?: "")
            row.createCell(10).setCellValue(item.accountBranchName ?: "")
            row.createCell(11).setCellValue(item.workingCategory1 ?: "")
            row.createCell(12).setCellValue(item.workingCategory2 ?: "")
            row.createCell(13).setCellValue(item.workingCategory3 ?: "")
            row.createCell(14).setCellValue(item.secondWorkType ?: "")
            row.createCell(15).setCellValue(item.workingCategory5 ?: "")
            row.createCell(16).setCellValue(item.commuteDate ?: "")
            row.createCell(17).setCellValue(item.isWorkReport ?: "")
            row.createCell(18).setCellValue(item.startDate ?: "")
            row.createCell(19).setCellValue(item.age?.toString() ?: "")
            row.createCell(20).setCellValue(item.yearsOfService?.toString() ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        val filename = "여사원배치점검_%04d%02d.xlsx".format(year, month)
        return ExcelResult(bytes, filename)
    }

    /** 여사원일정 1건 → 21컬럼 행. enum 필드는 displayName (`@JsonValue` 동일값) 으로 직렬화. */
    private fun toItem(schedule: TeamMemberSchedule, asOf: LocalDate): FemaleEmployeePlacementCheckItem {
        val emp = schedule.employee
        val acc = schedule.account
        return FemaleEmployeePlacementCheckItem(
            workingDate = schedule.workingDate?.toString(),
            orgName = emp?.orgName,
            employeeCode = emp?.employeeCode ?: "",
            jikwee = emp?.jikwee,
            name = emp?.name ?: "",
            professionalPromotionTeam = emp?.professionalPromotionTeam?.displayName,
            employmentStatus = emp?.status,
            accountType = acc?.accountType?.displayName,
            accountName = acc?.name,
            accountBranchCode = acc?.branchCode,
            accountBranchName = acc?.branchName,
            workingCategory1 = schedule.workingCategory1?.displayName,
            workingCategory2 = schedule.workingCategory2?.displayName,
            workingCategory3 = schedule.workingCategory3?.displayName,
            secondWorkType = schedule.secondWorkType,
            workingCategory5 = schedule.workingCategory5?.displayName,
            commuteDate = schedule.commuteDate?.toString(),
            isWorkReport = schedule.isWorkReport,
            startDate = emp?.startDate?.toString(),
            age = calculateAge(emp?.birthDate, asOf),
            yearsOfService = calculateYearsOfService(emp?.startDate, asOf),
        )
    }

    /** birthDate (String) 파싱 후 asOf 기준 만 나이. 파싱 불가/null → null. */
    private fun calculateAge(birthDate: String?, asOf: LocalDate): Int? {
        val date = parseDate(birthDate) ?: return null
        return Period.between(date, asOf).years.takeIf { it >= 0 }
    }

    /** startDate 기준 asOf 까지 만 근속연수. null → null. */
    private fun calculateYearsOfService(startDate: LocalDate?, asOf: LocalDate): Int? {
        startDate ?: return null
        return Period.between(startDate, asOf).years.takeIf { it >= 0 }
    }

    /** `yyyy-MM-dd` 또는 `yyyyMMdd` 형식 birthDate 파싱. 둘 다 실패 시 null. */
    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter)
            } catch (_: DateTimeParseException) {
                // 다음 포맷 시도
            }
        }
        return null
    }

    private fun applyScope(scope: DataScope, costCenterCodes: List<String>): List<String> {
        if (scope.isAllBranches) return costCenterCodes
        val allowed = scope.branchCodes.toSet()
        if (costCenterCodes.isEmpty()) {
            // 미지정 → 권한 범위 전체로 한정
            return scope.branchCodes
        }
        val intersect = costCenterCodes.filter { it in allowed }
        if (intersect.isEmpty()) throw AdminForbiddenException()
        return intersect
    }

    private fun validateParams(year: Int, month: Int) {
        if (year !in 2020..2099) {
            throw InvalidParameterException("year는 2020~2099 범위여야 합니다")
        }
        if (month !in 1..12) {
            throw InvalidParameterException("month는 1~12 범위여야 합니다")
        }
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

    companion object {
        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
        )
    }
}
