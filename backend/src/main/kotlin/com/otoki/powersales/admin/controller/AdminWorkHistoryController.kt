package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberDto
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryEmployeeAccountResponse
import com.otoki.powersales.domain.activity.schedule.service.AdminAttendInfoService
import com.otoki.powersales.domain.activity.schedule.service.EmployeeWorkHistoryService
import com.otoki.powersales.domain.activity.schedule.service.WorkHistoryPeriodSummaryService
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import java.time.YearMonth
import java.time.format.DateTimeParseException
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인사/근무 > 근무기간 조회 (조회 전용) — 월별 근무내역(개인) / 기간별 근무기간 두 탭.
 *
 * ## 권한 자원 — `work_history` (가상 자원, [PermissionResource])
 *
 * 근무기간 **조회** 화면과 기준정보 > HR 적재 근무기간 (`attend_info`, [AdminAttendInfoController])
 * 의 권한을 분리한다. 조장 등 "근무 실적은 봐야 하지만 SAP HR 적재 마스터를 편집하면 안 되는"
 * 직책에 조회만 부여하기 위함이다. 분리 전에는 두 화면이 `attend_info` 하나를 공유해
 * `AttendInfo__c` 를 회수하면 조회 화면까지 함께 닫혔다.
 *
 * 조회 대상 테이블은 `attend_info` 지만, 본 화면은 그 테이블의 CRUD 화면이 아니라 근무 실적 집계
 * 조회라 SF object 1:1 대응이 없다 — 레거시 SF 는 AttendInfo__c 객체 권한 하나만 두어 두 영역을
 * 구분하지 않았으므로 본 분리는 신규 deviation 이며, SF Custom Permission (`work_history`) 으로
 * 부여한다. JPA entity 가 없는 가상 자원이라 [PermissionResource] 로 등록한다
 * ([AdminFemaleEmployeeController] 의 `female_employee` 분리와 동일 취지).
 *
 * ## 셀렉터 endpoint 를 본 컨트롤러가 갖는 이유
 *
 * `/branches`, `/members` 는 두 탭의 지점/사원 셀렉터 전용이다. 기준정보 > HR 적재 근무기간
 * (`HrAttendInfoPage`) 은 사원번호/사원명 텍스트 필터만 쓰고 이 셀렉터를 호출하지 않으므로,
 * `attend_info` 가 아닌 본 컨트롤러(`work_history`)에 두어야 게이팅 권한과 정합한다.
 * 반대로 두면 조회 권한만 가진 조장이 메뉴는 보이는데 셀렉터에서만 403 이 난다.
 *
 * 모든 endpoint 는 조회 전용이라 READ 단일 operation 으로 가드한다.
 */
@RestController
@RequestMapping("/api/v1/admin/work-history")
@PermissionResource("work_history")
class AdminWorkHistoryController(
    private val service: AdminAttendInfoService,
    private val workHistoryPeriodSummaryService: WorkHistoryPeriodSummaryService,
    private val employeeWorkHistoryService: EmployeeWorkHistoryService,
) {

    /**
     * 기간별 근무내역(개인) — 특정 여사원 1명의 기간 내 거래처별 근무 집계.
     *
     * 좌측 패널에서 여사원을 선택하면 선택 기간의 근무 행을 거래처 단위로 그룹핑해 반환.
     * 지점 스코프 밖 여사원의 사번을 지정하면 빈 결과.
     */
    @GetMapping("/period-summary/accounts")
    @RequiresSfPermission(entity = "work_history", operation = SfPermissionOperation.READ)
    fun getPeriodAccountSummary(
        @CurrentDataScope scope: DataScope,
        @RequestParam employeeCode: String,
        @RequestParam fromYearMonth: String,
        @RequestParam toYearMonth: String,
    ): ResponseEntity<ApiResponse<WorkHistoryEmployeeAccountResponse>> {
        val response = workHistoryPeriodSummaryService.getAccountSummary(
            scope = scope,
            employeeCode = employeeCode,
            fromYearMonth = fromYearMonth,
            toYearMonth = toYearMonth,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 근무기간 조회 화면 "지점 선택" 드롭다운 옵션 — 권한별 조회 허용 지점.
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "work_history", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(service.getBranches(principal)))
    }

    /**
     * 근무기간 조회 화면 좌측 여사원 선택 목록.
     *
     * 여사원 일정관리의 /team-schedule/form members 를 빌려쓰던 것을 화면 도메인 권한(work_history)으로
     * 분리. 퇴사/휴직 등 비활성 여사원도 포함하여 과거 근무내역 조회를 지원한다.
     *
     * `branchCode` 지정 시 (다중/전사 권한자가 지점 선택) 해당 지점 여사원을 조회 — 권한 화이트리스트 검증.
     */
    @GetMapping("/members")
    @RequiresSfPermission(entity = "work_history", operation = SfPermissionOperation.READ)
    fun getMembers(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ApiResponse<List<TeamMemberDto>>> {
        return ResponseEntity.ok(ApiResponse.success(service.getMembers(principal, branchCode)))
    }

    /**
     * "월별 근무내역(개인)" 탭 — 인원 1명 × 지정 월의 근무내역을 일자 오름차순 조회.
     *
     * [AdminEmployeeController] 의 동일 데이터를 사용하되, 그쪽은 `employee` READ 로 가드되어 근무기간
     * 조회 화면(`work_history` 게이팅) 사용자가 403 이 난다. 화면 도메인 권한(`work_history`)으로 가드한
     * 전용 endpoint 로 분리한다 (`/members`·`/branches` 분리와 동일 취지).
     *
     * @param yearMonth `yyyy-MM` (예: 2026-06).
     */
    @GetMapping("/{employeeId}/work-history/monthly")
    @RequiresSfPermission(entity = "work_history", operation = SfPermissionOperation.READ)
    fun getMonthlyWorkHistory(
        @PathVariable employeeId: Long,
        @RequestParam yearMonth: String,
    ): ResponseEntity<ApiResponse<EmployeeWorkHistoryResponse>> {
        val response = employeeWorkHistoryService.getMonthlyHistory(employeeId, parseYearMonth(yearMonth))
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 월별 근무내역(개인) 엑셀 다운로드 — 탭과 동일 데이터/컬럼을 xlsx 로 추출. */
    @GetMapping("/{employeeId}/work-history/monthly/export")
    @RequiresSfPermission(entity = "work_history", operation = SfPermissionOperation.READ)
    fun exportMonthlyWorkHistory(
        @PathVariable employeeId: Long,
        @RequestParam yearMonth: String,
    ): ResponseEntity<ByteArray> {
        val result = employeeWorkHistoryService.exportMonthlyHistory(employeeId, parseYearMonth(yearMonth))
        return ExcelResponseUtils.build(result)
    }

    private fun parseYearMonth(yearMonth: String): YearMonth =
        try {
            YearMonth.parse(yearMonth)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("yearMonth 형식이 올바르지 않습니다 (yyyy-MM): $yearMonth")
        }
}
