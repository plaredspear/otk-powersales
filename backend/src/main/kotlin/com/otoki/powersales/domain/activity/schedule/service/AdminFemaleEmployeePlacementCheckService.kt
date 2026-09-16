package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.activity.schedule.dto.response.FemaleEmployeePlacementCheckItem
import com.otoki.powersales.domain.activity.schedule.dto.response.FemaleEmployeePlacementCheckResponse
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 여사원 배치 점검 현황 — 영업지원실용 기간(시작일~종료일) 배치 점검 조회 + 엑셀 export.
 *
 * 레거시 매핑: SF Report `InternalSalesReportFolder/new_report_4Ic`
 * (여사원 배치 점검 퇴직자 포함 (영업지원실 용) 상시_임시(조장포함)). Tabular — 여사원일정 행 단위 나열.
 * 동작: 조회기간(시작일~종료일) 으로 `TeamMemberSchedule` 을 조회 (근무유형='근무', 앱권한 여사원/조장,
 *       더미 사원명 제외, 퇴직자 포함 = status 필터 없음). employee/account 조인 결과를 21컬럼 행으로 매핑.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 도입 — 레거시 SF Report 의 web admin 이식. 레거시 하드코딩 날짜는 기간(시작일~종료일) 검색 조건으로 전환
 * (Spec #839 Q1 은 년·월 단위였으나, 월 경계를 걸친 조회 요구로 일 단위 기간으로 확장).
 * 나이(`Age__c`) / 근속연수(`yearsOfService__c`) 는 SF formula 필드이므로 [Employee.calculateAge] /
 * [Employee.calculateYearsOfService] (SF 계산식 정합 구현) 로 대체한다. SF formula 가 `TODAY()` 기준이고
 * `AppAuthority='여사원'` 일 때만 값을 내므로, 기준일은 조회기간이 아닌 오늘, 조장 행은 공백이다.
 */
@Service
@Transactional(readOnly = true)
class AdminFemaleEmployeePlacementCheckService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val branchCodeExpander: BranchCodeExpander,
) {

    /**
     * 여사원 배치 점검 조회.
     *
     * 기간 검증 (2020~2099 / 시작일 ≤ 종료일 / 최대 [MAX_RANGE_DAYS]일) 후 [startDate, endDate] 로 조회.
     * 권한: scope.isAllBranches 면 사용자 입력 그대로, 아니면 scope.branchCodes 와 교집합
     * (교집합 없으면 빈 결과 — 안전점검 보고서와 동일).
     * 지점 코드는 조회 직전 `BranchMapping` 확장 적용 (레거시/별칭 조직코드 적재 일정 누락 방지).
     * 정렬: 입사일(StartDate) 오름차순 1차 (SF new_report_4Ic sortColumn 정합) + 소속/사번/근무일자 후순위.
     *
     * 나이/근속연수 기준일은 조회기간이 아니라 **오늘** — SF formula 가 `TODAY()` 이므로 과거 기간을 조회해도
     * 현재 시점 값이 나온다 (레거시 리포트 정합).
     */
    fun getPlacementCheck(
        scope: DataScope,
        startDate: LocalDate,
        endDate: LocalDate,
        costCenterCodes: List<String>,
    ): FemaleEmployeePlacementCheckResponse {
        validateRange(startDate, endDate)
        val effectiveCodes = applyScope(scope, costCenterCodes)
            ?: return FemaleEmployeePlacementCheckResponse(startDate.toString(), endDate.toString(), emptyList())

        val schedules = teamMemberScheduleRepository.findPlacementCheck(
            from = startDate,
            to = endDate,
            roles = listOf(AppAuthority.WOMAN, AppAuthority.LEADER),
            branchCodes = effectiveCodes.takeIf { it.isNotEmpty() }
                ?.let { branchCodeExpander.expand(it).toList() }
                ?: effectiveCodes,
        )

        val items = schedules
            .map { toItem(it, LocalDate.now()) }
            .sortedWith(
                // 입사일 Asc 1차 (SF sortColumn StartDate Asc 정합, null 후순위) — 이하 안정 정렬용 후순위 키
                compareBy<FemaleEmployeePlacementCheckItem, String?>(nullsLast()) { it.startDate }
                    .thenBy { it.orgName ?: "" }
                    .thenBy { it.employeeCode }
                    .thenBy { it.workingDate ?: "" }
            )
        return FemaleEmployeePlacementCheckResponse(startDate.toString(), endDate.toString(), items)
    }

    /**
     * 배치 점검 엑셀 export — 21컬럼 시트 (조회와 동일 필터/스코프).
     */
    fun exportPlacementCheck(
        scope: DataScope,
        startDate: LocalDate,
        endDate: LocalDate,
        costCenterCodes: List<String>,
    ): ExcelResult {
        val response = getPlacementCheck(scope, startDate, endDate, costCenterCodes)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("여사원배치점검")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headers = listOf(
            "근무일자", "소속", "사번", "직위", "성명", "전문행사조", "재직상태",
            "거래처유형", "거래처명", "SAP거래처코드", "거래처지점명",
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
            row.createCell(9).setCellValue(item.accountSapCode ?: "")
            row.createCell(10).setCellValue(item.accountBranchName ?: "")
            row.createCell(11).setCellValue(item.workingCategory1 ?: "")
            row.createCell(12).setCellValue(item.workingCategory2 ?: "")
            row.createCell(13).setCellValue(item.workingCategory3 ?: "")
            row.createCell(14).setCellValue(item.secondWorkType ?: "")
            row.createCell(15).setCellValue(item.workingCategory5 ?: "")
            row.createCell(16).setCellValue(item.commuteDate ?: "")
            row.createCell(17).setCellValue(item.isWorkReport ?: "")
            row.createCell(18).setCellValue(item.startDate ?: "")
            row.createCell(19).setCellValue(item.age ?: "")
            row.createCell(20).setCellValue(item.yearsOfService ?: "")
        }
        // 총 건수 행 — SF Report GrandTotal(레코드 수) 정합
        sheet.createRow(response.items.size + 1).createCell(0).setCellValue("총 ${response.items.size}건")
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val filename = "여사원배치점검_%s_%s.xlsx".format(compact(startDate), compact(endDate))
        return ExcelResult(bytes, filename)
    }

    /**
     * 여사원일정 1건 → 21컬럼 행. enum 필드는 displayName (`@JsonValue` 동일값) 으로 직렬화.
     *
     * `secondWorkType` 은 일정 자체 컬럼이 아니라 **출근로그** 파생값([TeamMemberSchedule.secondWorkTypeText]) —
     * SF formula `TEXT(CommuteLogId__r.SecondWorkType__c)` 정합.
     * `age` / `yearsOfService` 는 SF formula 정합 계산기를 쓰며 `여사원` 게이트를 켠다 (조장 행은 null).
     */
    private fun toItem(schedule: TeamMemberSchedule, asOf: LocalDate): FemaleEmployeePlacementCheckItem {
        val emp = schedule.employee
        val acc = schedule.account
        return FemaleEmployeePlacementCheckItem(
            workingDate = schedule.workingDate?.toString(),
            orgName = emp?.orgName,
            employeeCode = emp?.employeeCode ?: "",
            jikwee = emp?.jikwee,
            name = emp?.name ?: "",
            // SF new_report_4Ic description: "전문행사조는 투입당시(여사원일정)를 보여줌" — 사원 마스터 현재 값 아님
            professionalPromotionTeam = schedule.professionalPromotionTeam,
            employmentStatus = emp?.status,
            accountType = acc?.accountType,
            accountName = acc?.name,
            accountSapCode = acc?.externalKey,
            accountBranchName = acc?.branchName,
            workingCategory1 = schedule.workingCategory1?.displayName,
            workingCategory2 = schedule.workingCategory2?.displayName,
            workingCategory3 = schedule.workingCategory3?.displayName,
            secondWorkType = schedule.secondWorkTypeText,
            workingCategory5 = schedule.workingCategory5?.displayName,
            commuteDate = schedule.commuteDate?.toString(),
            isWorkReport = schedule.isWorkReport,
            startDate = emp?.startDate?.toString(),
            age = emp?.calculateAge(asOf, womanOnly = true),
            yearsOfService = emp?.calculateYearsOfService(asOf, womanOnly = true),
        )
    }

    /** 화면 선택 지점을 권한 스코프로 해석. 교집합 없음(권한 밖 요청) → null = 빈 결과 (안전점검 NoAccess 정합). */
    private fun applyScope(scope: DataScope, costCenterCodes: List<String>): List<String>? {
        if (scope.isAllBranches) return costCenterCodes
        val allowed = scope.branchCodes.toSet()
        if (costCenterCodes.isEmpty()) {
            // 미지정 → 권한 범위 전체로 한정
            return scope.branchCodes
        }
        val intersect = costCenterCodes.filter { it in allowed }
        return intersect.ifEmpty { null }
    }

    /**
     * 조회기간 검증 — 연도 2020~2099, 시작일 ≤ 종료일, 최대 [MAX_RANGE_DAYS]일.
     *
     * 상한은 지점 전건 × 일별 행이라는 결과 규모 때문 — 전사 권한자가 장기간을 걸면 export 가 수십만 행이 된다.
     * 개인 1명만 나열하는 근무내역([AdminFemaleEmployeeWorkHistoryService]) 보다 보수적으로 잡는다.
     */
    private fun validateRange(startDate: LocalDate, endDate: LocalDate) {
        if (startDate.year !in 2020..2099 || endDate.year !in 2020..2099) {
            throw InvalidParameterException("조회 기간은 2020~2099 범위여야 합니다")
        }
        if (startDate.isAfter(endDate)) {
            throw InvalidParameterException("시작일은 종료일보다 이후일 수 없습니다")
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_RANGE_DAYS) {
            throw InvalidParameterException("조회 기간은 최대 ${MAX_RANGE_DAYS}일까지 가능합니다")
        }
    }

    /** 파일명용 날짜 압축 표기 (2026-05-01 → 20260501). */
    private fun compact(date: LocalDate): String = "%04d%02d%02d".format(date.year, date.monthValue, date.dayOfMonth)

    companion object {
        /** 조회 기간 상한 (일). 여사원일정 기간 조회 상한(`AdminTeamScheduleService`) 과 동일한 3개월 수준. */
        private const val MAX_RANGE_DAYS = 92L
    }
}
