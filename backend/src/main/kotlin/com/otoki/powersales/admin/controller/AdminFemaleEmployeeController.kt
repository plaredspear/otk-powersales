package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.service.AdminEmployeeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 여사원 현황 페이지 전용 — role 은 항상 [AppAuthority.WOMAN] ("여사원") 으로 고정.
 *
 * 권한 관리 (`/settings/admin-accounts`) 등 다른 role 도 보여야 하는 화면은
 * [AdminEmployeeController.getEmployees] 를 그대로 사용하고, 본 endpoint 는
 * 여사원 현황 화면에서만 호출한다.
 */
@RestController
@RequestMapping("/api/v1/admin/female-employees")
class AdminFemaleEmployeeController(
    private val adminEmployeeService: AdminEmployeeService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "employee", operation = SfPermissionOperation.READ)
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
}
