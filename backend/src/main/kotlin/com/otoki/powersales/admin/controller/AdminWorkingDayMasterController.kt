package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.domain.sales.dto.response.WorkingDayMasterListResponse
import com.otoki.powersales.domain.sales.service.AdminWorkingDayMasterService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 웹 영업일관리마스터(SF `WorkingDayMaster__c`) 조회 컨트롤러.
 *
 * 운영이 관리하는 영업일 달력을 연-월 단위로 조회한다(조회 전용). 권한 가드:
 * `working_day_master` READ — SF `WorkingDayMaster__c` objectPermissions.allowRead 비트 매칭.
 */
@RestController
@RequestMapping("/api/v1/admin/working-day-masters")
class AdminWorkingDayMasterController(
    private val adminWorkingDayMasterService: AdminWorkingDayMasterService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "working_day_master", operation = SfPermissionOperation.READ)
    fun getWorkingDayMasters(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<WorkingDayMasterListResponse>> {
        val response = adminWorkingDayMasterService.getWorkingDayMasters(year, month)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
