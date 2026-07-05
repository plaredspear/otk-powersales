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
import com.otoki.powersales.domain.org.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeService
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.service.EmployeeWorkHistoryService
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * 여사원 현황 페이지 전용 — role 은 [AppAuthority.WOMAN] ("여사원") + [AppAuthority.LEADER] ("조장") 로 고정.
 * 조장은 여사원 조직을 관리하는 직책이라 여사원 현황에 함께 노출한다.
 *
 * 권한 관리 (`/settings/admin-accounts`) 등 전체 role 을 보여야 하는 화면은
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
    private val adminEmployeeCredentialService: AdminEmployeeCredentialService,
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
) {

    companion object {
        /** 여사원 현황에 노출할 직책 — 여사원 + 조장(여사원 조직 관리자). */
        private val FEMALE_EMPLOYEE_ROLES = listOf(AppAuthority.WOMAN, AppAuthority.LEADER)
    }

    /**
     * 여사원 현황 화면 지점 셀렉터 옵션 — `female_employee` 권한으로 가드.
     *
     * 여사원 일정/대시보드/전문행사조와 동일하게 [WomenScheduleBranchResolver] 로 권한별 지점
     * 화이트리스트를 산출한다 (단일 출처). 여사원 현황 화면이 필요로 하는 lookup 이므로 화면의
     * 게이팅 권한(`female_employee`)과 동일하게 가드한다 — 다른 도메인(전문행사조) endpoint 를
     * 빌려쓰지 않아 조장 등 여사원 권한만 가진 직책도 접근 가능하게 한다.
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = womenScheduleBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        // 근무형태1(진열/행사) / 근무형태3(고정/격고/순회) — 최근 출근등록 1건 기준 필터.
        @RequestParam(required = false) workType1: String?,
        @RequestParam(required = false) workType3: String?,
        // 전문행사조 — 조명(라면세일조 등) 또는 '일반'(미배정). blank 면 전체.
        @RequestParam(required = false) professionalPromotionTeam: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            roles = FEMALE_EMPLOYEE_ROLES,
            page = page,
            size = size,
            // SF `SalesMemberListController` / `TeamMemberListController` 의 CostCenterCode 본인 지점 스코프 정합
            applyBranchScope = true,
            workType1 = workType1,
            workType3 = workType3,
            professionalPromotionTeam = professionalPromotionTeam,
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
        @RequestParam(required = false) workType1: String?,
        @RequestParam(required = false) workType3: String?,
        @RequestParam(required = false) professionalPromotionTeam: String?,
    ): ResponseEntity<ByteArray> {
        val result = adminEmployeeService.exportEmployees(
            scope = scope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            roles = FEMALE_EMPLOYEE_ROLES,
            applyBranchScope = true,
            workType1 = workType1,
            workType3 = workType3,
            professionalPromotionTeam = professionalPromotionTeam,
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

    /**
     * 여사원 단말 초기화 — [AdminEmployeeController.resetDevice] 와 동일 동작이나
     * `female_employee:EDIT` 권한으로 가드. 여사원 현황 화면에서 조장 등 여사원 권한만 가진
     * 직책이 호출할 수 있도록 분리. 전체 사원 관리(`MANAGE_USERS`)와 별개 권한.
     */
    @PostMapping("/{employeeId}/reset-device")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.EDIT)
    fun resetFemaleEmployeeDevice(
        @PathVariable employeeId: Long,
    ): ResponseEntity<ApiResponse<ResetDeviceResponse>> {
        val response = adminEmployeeCredentialService.resetDevice(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "단말이 초기화되었습니다"))
    }

    /**
     * 여사원 비밀번호 초기화 — [AdminEmployeeController.resetPassword] 와 동일 동작이나
     * `female_employee:EDIT` 권한으로 가드.
     */
    @PostMapping("/{employeeId}/reset-password")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.EDIT)
    fun resetFemaleEmployeePassword(
        @PathVariable employeeId: Long,
    ): ResponseEntity<ApiResponse<ResetPasswordResponse>> {
        val response = adminEmployeeCredentialService.resetPassword(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 초기화되었습니다"))
    }
}
