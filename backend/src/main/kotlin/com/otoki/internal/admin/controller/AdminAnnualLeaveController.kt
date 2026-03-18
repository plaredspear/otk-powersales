package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.EmployeeAnnualLeaveDto
import com.otoki.internal.admin.service.AdminAnnualLeaveService
import com.otoki.internal.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/annual-leave")
class AdminAnnualLeaveController(
    private val adminAnnualLeaveService: AdminAnnualLeaveService
) {

    @GetMapping("/summary")
    fun getSummary(
        @RequestParam yearMonth: String,
        @RequestParam(required = false) orgCode: String?
    ): ResponseEntity<ApiResponse<List<EmployeeAnnualLeaveDto>>> {
        if (!yearMonth.matches(YEAR_MONTH_PATTERN)) {
            throw InvalidYearMonthException()
        }
        val result = adminAnnualLeaveService.getSummary(yearMonth, orgCode)
        return ResponseEntity.ok(ApiResponse.success(result, "연차 현황 조회 성공"))
    }

    companion object {
        private val YEAR_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    }
}
