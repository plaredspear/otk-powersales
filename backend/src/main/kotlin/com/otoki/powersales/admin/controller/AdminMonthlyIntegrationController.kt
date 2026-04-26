package com.otoki.powersales.admin.controller

import com.otoki.powersales.schedule.dto.response.CategoryScheduleResponse
import com.otoki.powersales.schedule.dto.response.MonthlyIntegrationScheduleResponse
import com.otoki.powersales.schedule.service.AdminMonthlyIntegrationService
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/schedules/monthly-integration")
class AdminMonthlyIntegrationController(
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService
) {

    @GetMapping
    fun getMonthlyIntegration(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>
    ): ResponseEntity<ApiResponse<MonthlyIntegrationScheduleResponse>> {
        val response = adminMonthlyIntegrationService.getMonthlyIntegration(year, month, costCenterCodes)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/export")
    fun exportMonthlyIntegration(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>
    ): ResponseEntity<ByteArray> {
        val result = adminMonthlyIntegrationService.exportMonthlyIntegration(year, month, costCenterCodes)

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.filename}\"")

        return ResponseEntity.ok().headers(headers).body(result.bytes)
    }

    @GetMapping("/category")
    fun getCategorySchedule(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>
    ): ResponseEntity<ApiResponse<CategoryScheduleResponse>> {
        val response = adminMonthlyIntegrationService.getCategorySchedule(year, month, costCenterCodes)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/category/export")
    fun exportCategorySchedule(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>
    ): ResponseEntity<ByteArray> {
        val result = adminMonthlyIntegrationService.exportCategorySchedule(year, month, costCenterCodes)

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.filename}\"")

        return ResponseEntity.ok().headers(headers).body(result.bytes)
    }
}
