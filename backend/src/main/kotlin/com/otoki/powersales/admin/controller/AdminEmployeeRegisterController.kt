package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.AdminEmployeeRegisterRequest
import com.otoki.powersales.admin.dto.AdminEmployeeRegisterResponse
import com.otoki.powersales.admin.service.AdminEmployeeRegisterService
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 시스템 관리자 수동 등록 컨트롤러 (Spec #579).
 *
 * `POST /api/v1/admin/employees` 단일 엔드포인트만 제공한다.
 * 호출자 권한 (UserRole.MANAGE_PERMISSIONS) 검증은 서비스 계층에서 수행된다.
 */
@RestController
@RequestMapping("/api/v1/admin/employees")
class AdminEmployeeRegisterController(
    private val adminEmployeeRegisterService: AdminEmployeeRegisterService
) {

    @PostMapping
    fun register(
        @Valid @RequestBody request: AdminEmployeeRegisterRequest
    ): ResponseEntity<ApiResponse<AdminEmployeeRegisterResponse>> {
        val response = adminEmployeeRegisterService.register(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "관리자 계정이 등록되었습니다"))
    }
}
