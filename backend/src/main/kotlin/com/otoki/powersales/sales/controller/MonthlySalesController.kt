package com.otoki.powersales.sales.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sales.dto.request.MonthlySalesRequest
import com.otoki.powersales.sales.dto.response.MonthlySalesResponse
import com.otoki.powersales.sales.service.MonthlySalesService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 월매출 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/mobile/sales")
class MonthlySalesController(
    private val monthlySalesService: MonthlySalesService
) {

    /**
     * 월매출 조회
     * GET /api/v1/mobile/sales/monthly
     */
    @GetMapping("/monthly")
    fun getMonthlySales(
        @Valid request: MonthlySalesRequest
    ): ResponseEntity<ApiResponse<MonthlySalesResponse>> {
        val response = monthlySalesService.getMonthlySales(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
