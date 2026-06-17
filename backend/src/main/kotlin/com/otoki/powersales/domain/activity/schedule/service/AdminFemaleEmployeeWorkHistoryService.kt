package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.schedule.dto.response.FemaleEmployeeWorkHistoryItem
import com.otoki.powersales.domain.activity.schedule.dto.response.FemaleEmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 여사원 근무내역 (개인별 조회) — 영업지원실용 보고서 조회 + 엑셀 export.
 *
 * 레거시 매핑: SF Report `InternalSalesReportFolder/new_report_nEX` (여사원 근무내역).
 * 동작: 특정 사번 1명 + year/month 를 해당 월 1일~말일로 환산하여 `TeamMemberSchedule` 을 조회.
 *       employee/account 조인 결과를 15컬럼 행으로 매핑. 근무일자 오름차순.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 도입 — 레거시 SF Report 의 web admin 이식. 레거시 하드코딩 사번은 검색 조건으로, 기간 없음은 년·월로 전환 (Spec #840 Q1/Q2).
 * 지점 스코프는 사원 소속 지점(costCenterCode) 기준 — SF `CurrentUserBranchNameList` 정합 (Q3).
 * 나이(`Age__c`) 는 SF formula 필드이므로 birthDate 기반 계산으로 대체. 기존 [EmployeeWorkHistoryService] (상세 화면용 최근 N건) 와 별개.
 */
@Service
@Transactional(readOnly = true)
class AdminFemaleEmployeeWorkHistoryService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    /**
     * 개인별 근무내역 조회.
     *
     * employeeCode 필수 + year/month 검증 (2020~2099 / 1~12) 후 해당 월 [1일, 말일] 환산하여 조회.
     * 지점 스코프: scope.isAllBranches 면 무제한, 아니면 scope.branchCodes (costCenterCode) 로 제한.
     * 사번 미존재/해당 월 일정 없음/스코프 밖이면 빈 결과 (예외 아님).
     */
    fun getWorkHistory(
        scope: DataScope,
        employeeCode: String,
        year: Int,
        month: Int,
    ): FemaleEmployeeWorkHistoryResponse {
        validateParams(employeeCode, year, month)
        val yearMonth = YearMonth.of(year, month)
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()
        val branchCodes = if (scope.isAllBranches) emptyList() else scope.branchCodes

        val schedules = teamMemberScheduleRepository.findWorkHistory(
            employeeCode = employeeCode.trim(),
            from = from,
            to = to,
            branchCodes = branchCodes,
        )

        val items = schedules.map { toItem(it, to) }
        return FemaleEmployeeWorkHistoryResponse(employeeCode.trim(), year, month, items)
    }

    /**
     * 근무내역 엑셀 export — 15컬럼 시트 (조회와 동일 필터/스코프).
     */
    fun exportWorkHistory(
        scope: DataScope,
        employeeCode: String,
        year: Int,
        month: Int,
    ): ExcelResult {
        val response = getWorkHistory(scope, employeeCode, year, month)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("여사원근무내역")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headers = listOf(
            "일정명", "성명", "사번", "나이", "근무일자",
            "거래처지점명", "거래처코드", "거래처명", "근무유형",
            "근무구분1", "근무구분2", "근무구분3", "부근무유형", "근무보고여부", "출근일자",
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
            row.createCell(0).setCellValue(item.scheduleName ?: "")
            row.createCell(1).setCellValue(item.name)
            row.createCell(2).setCellValue(item.employeeCode)
            row.createCell(3).setCellValue(item.age?.toString() ?: "")
            row.createCell(4).setCellValue(item.workingDate ?: "")
            row.createCell(5).setCellValue(item.accountBranchName ?: "")
            row.createCell(6).setCellValue(item.accountBranchCode ?: "")
            row.createCell(7).setCellValue(item.accountName ?: "")
            row.createCell(8).setCellValue(item.workingType ?: "")
            row.createCell(9).setCellValue(item.workingCategory1 ?: "")
            row.createCell(10).setCellValue(item.workingCategory2 ?: "")
            row.createCell(11).setCellValue(item.workingCategory3 ?: "")
            row.createCell(12).setCellValue(item.secondWorkType ?: "")
            row.createCell(13).setCellValue(item.isWorkReport ?: "")
            row.createCell(14).setCellValue(item.commuteDate ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val filename = "여사원근무내역_%s_%04d%02d.xlsx".format(employeeCode.trim(), year, month)
        return ExcelResult(bytes, filename)
    }

    /** 여사원일정 1건 → 15컬럼 행. enum 필드는 displayName (`@JsonValue` 동일값) 으로 직렬화. */
    private fun toItem(schedule: TeamMemberSchedule, asOf: LocalDate): FemaleEmployeeWorkHistoryItem {
        val emp = schedule.employee
        val acc = schedule.account
        return FemaleEmployeeWorkHistoryItem(
            scheduleName = schedule.name,
            name = emp?.name ?: "",
            employeeCode = emp?.employeeCode ?: "",
            age = calculateAge(emp?.birthDate, asOf),
            workingDate = schedule.workingDate?.toString(),
            accountBranchName = acc?.branchName,
            accountBranchCode = acc?.branchCode,
            accountName = acc?.name,
            workingType = schedule.workingType?.displayName,
            workingCategory1 = schedule.workingCategory1?.displayName,
            workingCategory2 = schedule.workingCategory2?.displayName,
            workingCategory3 = schedule.workingCategory3?.displayName,
            secondWorkType = schedule.secondWorkType,
            isWorkReport = schedule.isWorkReport,
            commuteDate = schedule.commuteDate?.toString(),
        )
    }

    /** birthDate (String) 파싱 후 asOf 기준 만 나이. 파싱 불가/null → null. (#839 동일 헬퍼) */
    private fun calculateAge(birthDate: String?, asOf: LocalDate): Int? {
        val date = parseDate(birthDate) ?: return null
        return Period.between(date, asOf).years.takeIf { it >= 0 }
    }

    /** `yyyy-MM-dd` 또는 `yyyyMMdd` 형식 파싱. 둘 다 실패 시 null. */
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

    private fun validateParams(employeeCode: String, year: Int, month: Int) {
        if (employeeCode.isBlank()) {
            throw InvalidParameterException("employee_code는 필수입니다")
        }
        if (year !in 2020..2099) {
            throw InvalidParameterException("year는 2020~2099 범위여야 합니다")
        }
        if (month !in 1..12) {
            throw InvalidParameterException("month는 1~12 범위여야 합니다")
        }
    }

    companion object {
        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
        )
    }
}
