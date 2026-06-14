package com.otoki.powersales.platform.common.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val authService: AuthService
) {

    @PostMapping("/users/{employeeCode}/reset-device")
    fun resetDevice(@PathVariable employeeCode: String): ResponseEntity<ApiResponse<Any?>> {
        authService.resetDevice(employeeCode)
        return ResponseEntity.ok(ApiResponse.success(null, "단말기 등록이 초기화되었습니다"))
    }
}
