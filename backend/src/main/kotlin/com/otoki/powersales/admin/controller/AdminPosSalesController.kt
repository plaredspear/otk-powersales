package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.domain.sales.dto.request.PosSalesRequest
import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import com.otoki.powersales.domain.sales.service.PosSalesService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * POS매출 조회 admin API Controller.
 *
 * 레거시 `promotion/month/posmain.jsp` (POS DB `live_pos_sales_dh`) 의 web admin 이관.
 * 매출/실적 도메인 권한(`monthly_sales_history`) 을 재사용한다.
 */
@RestController
@RequestMapping("/api/v1/admin/sales/pos")
@PermissionResource("monthly_sales_history")
class AdminPosSalesController(
    private val posSalesService: PosSalesService,
) {

    /**
     * POS매출 조회 (거래처 1곳 + 연월, 제품별 실적)
     * GET /api/v1/admin/sales/pos
     */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping
    fun getPosSales(
        @Valid request: PosSalesRequest,
    ): ResponseEntity<ApiResponse<PosSalesResponse>> {
        val response = posSalesService.getPosSales(request.customerId, request.yearMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
