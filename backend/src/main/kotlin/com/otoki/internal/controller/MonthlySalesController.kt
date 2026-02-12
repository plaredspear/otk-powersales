package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.MonthlySalesRequest
import com.otoki.internal.dto.response.MonthlySalesResponse
import com.otoki.internal.service.MonthlySalesService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 월매출 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/sales")
class MonthlySalesController(
    private val monthlySalesService: MonthlySalesService
) {

    /**
     * 월매출 조회
     * GET /api/v1/sales/monthly
     */
    @GetMapping("/monthly")
    fun getMonthlySales(
        @Valid request: MonthlySalesRequest
    ): ResponseEntity<ApiResponse<MonthlySalesResponse>> {
        val response = monthlySalesService.getMonthlySales(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
