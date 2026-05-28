package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionInspectionService
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.employee.dto.request.AdminEmployeeManualRegisterRequest
import com.otoki.powersales.employee.dto.request.AdminEmployeeUpdateRequest
import com.otoki.powersales.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.employee.service.AdminEmployeeManualRegisterService
import com.otoki.powersales.employee.service.AdminEmployeeService
import com.otoki.powersales.employee.service.AdminEmployeeUpdateService
import com.otoki.powersales.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.schedule.service.EmployeeWorkHistoryService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/employees")
class AdminEmployeeController(
    private val adminEmployeeService: AdminEmployeeService,
    private val adminEmployeeCredentialService: AdminEmployeeCredentialService,
    private val adminEmployeeUpdateService: AdminEmployeeUpdateService,
    private val adminEmployeeManualRegisterService: AdminEmployeeManualRegisterService,
    private val sfPermissionInspectionService: SfPermissionInspectionService,
    private val employeeWorkHistoryService: EmployeeWorkHistoryService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.READ)
    fun getEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            role = role,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 행사상세/전문행사조 화면의 사원 lookup search — SF PromotionEmployee__c.EmployeeId__c Lookup 정합.
     *
     * SF 의 lookup search 는 Employee FLS/object access 와 무관하게 화면 권한 (Promotion CRUD) 으로
     * 작동 — 본 endpoint 는 SF 메커니즘 정합. 결과는 동일 [EmployeeListResponse] 재사용
     * (sharing rule 평가는 `adminEmployeeService.getEmployees` 가 그대로 적용).
     */
    @GetMapping("/lookup")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun lookupEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = null,
            keyword = keyword,
            role = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 거래처 등록/수정 화면의 영업담당자 lookup search — Spec #640 신규 기능.
     *
     * SF 직접 매핑 없음 (Account.OwnerId 는 User reference, 신규 시스템에서 영업담당자를 Employee
     * 로 매핑). account.EDIT 권한 보유자가 거래처 등록/수정 시 호출.
     */
    @GetMapping("/lookup-for-account")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.EDIT)
    fun lookupEmployeesForAccount(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = null,
            keyword = keyword,
            role = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 유통기한 관리 화면의 사원 lookup search — Heroku 단독 기능 (SF 매핑 없음).
     *
     * product.READ 권한 보유자가 유통기한 등록 시 사원 검색. employee.READ 권한 없이 호출 가능.
     */
    @GetMapping("/lookup-for-product")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun lookupEmployeesForProduct(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = null,
            keyword = keyword,
            role = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 진열사원 스케줄 마스터 화면의 사원 lookup search — SF TeamMemberSchedule__c.EmployeeId__c Lookup 정합.
     *
     * SF 의 lookup search 는 Employee FLS/object access 와 무관하게 화면 권한 (Schedule CRUD) 으로
     * 작동 — 본 endpoint 는 SF 메커니즘 정합. 결과는 동일 [EmployeeListResponse] 재사용
     * (sharing rule 평가는 `adminEmployeeService.getEmployees` 가 그대로 적용).
     */
    @GetMapping("/lookup-for-schedule")
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun lookupEmployeesForSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = null,
            keyword = keyword,
            role = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 사원 단건 상세 조회 — 6개 그룹 (인사·조직·직무·연락처·앱 설정·근무) 의 모든 필드.
     *
     * 레거시 SF 표준 레코드 상세 페이지 동등.
     */
    @GetMapping("/{employeeId}")
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.READ)
    fun getEmployee(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<EmployeeDetailResponse>> {
        val response = adminEmployeeService.getEmployee(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 일반 사원 수동 등록 (UC-06). origin=MANUAL 로 고정 저장.
     *
     * ADMIN-prefix SYSTEM_ADMIN 등록은 `AdminEmployeeRegisterController` 의 `POST /` 사용.
     */
    @PostMapping("/manual")
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.EDIT)
    fun manualRegister(
        @Valid @RequestBody request: AdminEmployeeManualRegisterRequest
    ): ResponseEntity<ApiResponse<EmployeeDetailResponse>> {
        val response = adminEmployeeManualRegisterService.register(request)
        return ResponseEntity
            .status(org.springframework.http.HttpStatus.CREATED)
            .body(ApiResponse.success(response, "사원이 등록되었습니다"))
    }

    /**
     * 사원 정보 수정 (UC-07). origin=SAP 사원은 차단.
     */
    @PatchMapping("/{employeeId}")
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.EDIT)
    fun updateEmployee(
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: AdminEmployeeUpdateRequest
    ): ResponseEntity<ApiResponse<EmployeeDetailResponse>> {
        val response = adminEmployeeUpdateService.update(employeeId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "사원 정보가 수정되었습니다"))
    }

    @PostMapping("/{employeeId}/reset-device")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun resetDevice(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<ResetDeviceResponse>> {
        val response = adminEmployeeCredentialService.resetDevice(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "단말이 초기화되었습니다"))
    }

    @PostMapping("/{employeeId}/reset-password")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun resetPassword(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<ResetPasswordResponse>> {
        val response = adminEmployeeCredentialService.resetPassword(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 초기화되었습니다"))
    }

    /**
     * Spec #802 — 직원의 SF 권한 read-only 조회.
     *
     * SF org 가 부여 SoT 이므로 edit endpoint 없음. 운영 진단 용도.
     * 조회 권한: employee READ.
     */
    @GetMapping("/{employeeId}/permissions")
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.READ)
    fun getEmployeePermissions(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<SfPermissionInspectionService.EmployeePermissionInspection>> {
        val inspection = sfPermissionInspectionService.inspectByEmployeeId(employeeId)
            ?: return ResponseEntity.ok(ApiResponse.success(emptyInspection(employeeId)))
        return ResponseEntity.ok(ApiResponse.success(inspection))
    }

    /**
     * 여사원 상세 — 시간순서별 근무이력(TeamMemberSchedule) 조회.
     *
     * 기본 limit 10. working_date desc + created_at desc 정렬.
     */
    @GetMapping("/{employeeId}/work-history")
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.READ)
    fun getWorkHistory(
        @PathVariable employeeId: Long,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<ApiResponse<EmployeeWorkHistoryResponse>> {
        val response = employeeWorkHistoryService.getRecentHistory(employeeId, limit)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    private fun emptyInspection(employeeId: Long): SfPermissionInspectionService.EmployeePermissionInspection =
        SfPermissionInspectionService.EmployeePermissionInspection(
            employeeCode = "",
            userId = employeeId,
            username = "",
            profile = null,
            permissionSets = emptyList(),
            entityMatrix = emptyList(),
            systemPermissions = emptyList(),
        )
}
