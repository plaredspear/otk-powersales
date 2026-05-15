package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.NaverGeocodeTestRequest
import com.otoki.powersales.admin.dto.response.NaverGeocodeTestResponse
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.admin.service.AdminNaverGeocodeService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminNaverGeocodeController(
    private val adminNaverGeocodeService: AdminNaverGeocodeService
) {

    @PostMapping("/api/v1/admin/naver-geocode/test")
    @RequiresPermission(AdminPermission.NAVER_GEOCODE_TEST)
    fun test(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: NaverGeocodeTestRequest
    ): ResponseEntity<ApiResponse<NaverGeocodeTestResponse>> {
        val response = adminNaverGeocodeService.test(principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
