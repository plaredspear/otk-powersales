package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.PermissionMatrixResponse
import com.otoki.internal.admin.service.AdminPermissionMatrixService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/permissions")
class AdminPermissionMatrixController(
    private val adminPermissionMatrixService: AdminPermissionMatrixService
) {

    @GetMapping("/matrix")
    fun getMatrix(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<PermissionMatrixResponse>> {
        val response = adminPermissionMatrixService.getMatrix(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
