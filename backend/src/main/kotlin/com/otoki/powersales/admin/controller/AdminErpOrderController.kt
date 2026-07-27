package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.activity.order.dto.response.AdminErpOrderDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.AdminErpOrderListResponse
import com.otoki.powersales.domain.activity.order.service.AdminErpOrderService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 관리자 웹 ERP주문 조회 API Controller (기준정보 > ERP주문 조회 화면).
 *
 * SAP 인바운드(#561)가 적재한 `erp_order` / `erp_order_product` 를 web admin 에 조회 노출하는
 * read-only endpoint. `erp_order` (`ERP_Order__c`) READ 권한으로 가드 — 목록/상세 대칭.
 */
@RestController
@RequestMapping("/api/v1/admin/erp-orders")
@Validated
class AdminErpOrderController(
    private val adminErpOrderService: AdminErpOrderService
) {

    @GetMapping
    @RequiresSfPermission(entity = "erp_order", operation = SfPermissionOperation.READ)
    fun getErpOrders(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) @Size(min = 1, max = 80) sapOrderNumber: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) deliveryDateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) deliveryDateTo: LocalDate?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AdminErpOrderListResponse>> {
        val response = adminErpOrderService.getErpOrders(
            sapOrderNumber = sapOrderNumber,
            deliveryDateFrom = deliveryDateFrom,
            deliveryDateTo = deliveryDateTo,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "erp_order", operation = SfPermissionOperation.READ)
    fun getErpOrderDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<AdminErpOrderDetailResponse>> {
        val response = adminErpOrderService.getErpOrderDetail(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
