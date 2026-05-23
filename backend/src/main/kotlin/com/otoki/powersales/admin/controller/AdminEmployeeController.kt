package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionInspectionService
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.entity.UserRoleEnum
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
) {

    @GetMapping
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.READ)
    fun getEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) role: UserRoleEnum?,
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
