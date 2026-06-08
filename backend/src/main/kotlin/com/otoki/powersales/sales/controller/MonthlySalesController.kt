package com.otoki.powersales.sales.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sales.dto.request.ElectronicSalesRequest
import com.otoki.powersales.sales.dto.request.LogisticsSalesRequest
import com.otoki.powersales.sales.dto.request.MonthlySalesRequest
import com.otoki.powersales.sales.dto.request.PosSalesRequest
import com.otoki.powersales.sales.dto.response.ElectronicSalesResponse
import com.otoki.powersales.sales.dto.response.LogisticsSalesResponse
import com.otoki.powersales.sales.dto.response.MonthlySalesResponse
import com.otoki.powersales.sales.dto.response.PosSalesResponse
import com.otoki.powersales.sales.service.ElectronicSalesService
import com.otoki.powersales.sales.service.LogisticsSalesService
import com.otoki.powersales.sales.service.MonthlySalesService
import com.otoki.powersales.sales.service.PosSalesService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 월매출 / 물류매출 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/mobile/sales")
class MonthlySalesController(
    private val monthlySalesService: MonthlySalesService,
    private val logisticsSalesService: LogisticsSalesService,
    private val electronicSalesService: ElectronicSalesService,
    private val posSalesService: PosSalesService,
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

    /**
     * 물류매출 조회 (거래처 1곳 + 연월, 온도대별 물류마감실적)
     * GET /api/v1/mobile/sales/logistics
     */
    @GetMapping("/logistics")
    fun getLogisticsSales(
        @Valid request: LogisticsSalesRequest
    ): ResponseEntity<ApiResponse<LogisticsSalesResponse>> {
        val response = logisticsSalesService.getLogisticsSales(request.customerId, request.yearMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 전산매출(ABC) 조회 (거래처 1곳 + 연월, 제품별 실적)
     * GET /api/v1/mobile/sales/electronic
     */
    @GetMapping("/electronic")
    fun getElectronicSales(
        @Valid request: ElectronicSalesRequest
    ): ResponseEntity<ApiResponse<ElectronicSalesResponse>> {
        val response = electronicSalesService.getElectronicSales(request.customerId, request.yearMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * POS매출 조회 (거래처 1곳 + 연월, 제품별 POS 스캔 실적)
     * GET /api/v1/mobile/sales/pos
     */
    @GetMapping("/pos")
    fun getPosSales(
        @Valid request: PosSalesRequest
    ): ResponseEntity<ApiResponse<PosSalesResponse>> {
        val response = posSalesService.getPosSales(request.customerId, request.yearMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
