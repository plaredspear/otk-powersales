package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeService
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.service.EmployeeWorkHistoryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * 여사원 현황 페이지 전용 — role 은 항상 [AppAuthority.WOMAN] ("여사원") 으로 고정.
 *
 * 권한 관리 (`/settings/admin-accounts`) 등 다른 role 도 보여야 하는 화면은
 * [AdminEmployeeController.getEmployees] 를 그대로 사용하고, 본 endpoint 는
 * 여사원 현황 화면에서만 호출한다.
 *
 * ## 권한 자원 — `female_employee` (가상 자원, [PermissionResource])
 *
 * 여사원 현황/상세는 전체 사원 관리(`employee`)와 권한을 분리한다. 조장 등 "여사원만 보는"
 * 직책에 여사원 현황만 부여하고 전체 사원 관리는 막기 위함. 레거시 SF 는 Employee 객체 권한
 * 하나만 두어 두 영역을 구분하지 않았으므로 본 분리는 신규 deviation 이며, SF Custom Permission
 * (`female_employee`) 으로 부여한다 — JPA entity 가 없는 가상 자원이라 [PermissionResource] 로 등록.
 * 상세/근무이력은 [AdminEmployeeController] 의 공용 endpoint 를 쓰지 않고 본 컨트롤러에서
 * `female_employee` 가드로 제공하여 여사원 권한만으로 현황+상세 완결 접근이 되도록 한다.
 */
@RestController
@RequestMapping("/api/v1/admin/female-employees")
@PermissionResource("female_employee")
class AdminFemaleEmployeeController(
    private val adminEmployeeService: AdminEmployeeService,
    private val employeeWorkHistoryService: EmployeeWorkHistoryService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            role = AppAuthority.WOMAN,
            page = page,
            size = size,
            // SF `SalesMemberListController` / `TeamMemberListController` 의 CostCenterCode 본인 지점 스코프 정합
            applyBranchScope = true,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 여사원 현황 엑셀 다운로드 — 목록과 동일한 지점 스코프/필터로 전량 추출 (최대 건수 제한 적용). */
    @GetMapping("/export")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun exportFemaleEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<ByteArray> {
        val result = adminEmployeeService.exportEmployees(
            scope = scope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            role = AppAuthority.WOMAN,
            applyBranchScope = true,
        )
        return ExcelResponseUtils.build(result)
    }

    /**
     * 여사원 단건 상세 조회 — [AdminEmployeeController.getEmployee] 와 동일 응답이나
     * `female_employee` 권한으로 가드. 여사원 현황에서 행 클릭 시 호출.
     */
    @GetMapping("/{employeeId}")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployee(
        @PathVariable employeeId: Long,
    ): ResponseEntity<ApiResponse<EmployeeDetailResponse>> {
        val response = adminEmployeeService.getEmployee(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 여사원 상세 — 시간순서별 근무이력(TeamMemberSchedule) 조회. 기본 limit 10.
     */
    @GetMapping("/{employeeId}/work-history")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployeeWorkHistory(
        @PathVariable employeeId: Long,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<ApiResponse<EmployeeWorkHistoryResponse>> {
        val response = employeeWorkHistoryService.getRecentHistory(employeeId, limit)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 여사원 근무기간 조회(월별) — 인원 1명 × 지정 월의 근무내역을 일자 오름차순 조회.
     *
     * @param yearMonth `yyyy-MM` (예: 2026-06).
     */
    @GetMapping("/{employeeId}/work-history/monthly")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployeeMonthlyWorkHistory(
        @PathVariable employeeId: Long,
        @RequestParam yearMonth: String,
    ): ResponseEntity<ApiResponse<EmployeeWorkHistoryResponse>> {
        val parsed = try {
            YearMonth.parse(yearMonth)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("yearMonth 형식이 올바르지 않습니다 (yyyy-MM): $yearMonth")
        }
        val response = employeeWorkHistoryService.getMonthlyHistory(employeeId, parsed)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
