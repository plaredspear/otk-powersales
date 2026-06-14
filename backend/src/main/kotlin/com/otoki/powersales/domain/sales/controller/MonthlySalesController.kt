package com.otoki.powersales.domain.sales.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.sales.dto.request.ElectronicSalesRequest
import com.otoki.powersales.domain.sales.dto.request.LogisticsSalesRequest
import com.otoki.powersales.domain.sales.dto.request.MonthlySalesRequest
import com.otoki.powersales.domain.sales.dto.request.PosSalesRangeRequest
import com.otoki.powersales.domain.sales.dto.request.PosSalesRequest
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesResponse
import com.otoki.powersales.domain.sales.dto.response.LogisticsSalesResponse
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import com.otoki.powersales.domain.sales.service.ElectronicSalesService
import com.otoki.powersales.domain.sales.service.LogisticsSalesService
import com.otoki.powersales.domain.sales.service.MonthlySalesService
import com.otoki.powersales.domain.sales.service.PosSalesService
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
     * 전산매출(ABC) 조회 (거래처 1곳 + 기간 + 매출 조회 제품 바코드, 제품별 실적)
     * GET /api/v1/mobile/sales/electronic
     */
    @GetMapping("/electronic")
    fun getElectronicSales(
        @Valid request: ElectronicSalesRequest
    ): ResponseEntity<ApiResponse<ElectronicSalesResponse>> {
        val response = electronicSalesService.getElectronicSales(
            customerId = request.customerId,
            startDate = request.startDate,
            endDate = request.endDate,
            barcodes = request.barcodes,
        )
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

    /**
     * POS매출 조회 (거래처 1곳 + 기간(시작/종료일) + 선택 바코드 목록)
     * GET /api/v1/mobile/sales/pos/by-range
     *
     * 레거시 `promotion/month/posmain.jsp` 의 daterangepicker + 매출 조회 제품 정합.
     * `barcodes` 미지정 시 거래처 전체 제품 집계, 1건 이상 시 해당 바코드 제품만 집계.
     */
    @GetMapping("/pos/by-range")
    fun getPosSalesByRange(
        @Valid request: PosSalesRangeRequest
    ): ResponseEntity<ApiResponse<PosSalesRangeResponse>> {
        val response = posSalesService.getPosSalesByRange(
            request.customerId,
            request.startDate,
            request.endDate,
            request.barcodeList(),
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
