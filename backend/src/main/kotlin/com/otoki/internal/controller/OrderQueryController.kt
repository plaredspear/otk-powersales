/*
package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.CreditBalanceResponse
import com.otoki.internal.dto.response.OrderHistoryProductResponse
import com.otoki.internal.dto.response.ProductOrderInfoResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderQueryService
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/ **
 * 주문 조회 관련 API Controller
 * - 주문이력 제품 조회
 * - 거래처 여신잔액 조회
 * - 제품 주문정보 조회
 * /
@RestController
class OrderQueryController(
    private val orderQueryService: OrderQueryService
) {

    / **
     * 주문이력 제품 조회
     * GET /api/v1/me/order-history/products
     * /
    @GetMapping("/api/v1/me/order-history/products")
    fun getOrderHistoryProducts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) orderDateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) orderDateTo: LocalDate?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<Page<OrderHistoryProductResponse>>> {
        val result = orderQueryService.getOrderHistoryProducts(
            userId = principal.userId,
            orderDateFrom = orderDateFrom,
            orderDateTo = orderDateTo,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    / **
     * 거래처 여신잔액 조회
     * GET /api/v1/clients/{clientId}/credit-balance
     * /
    @GetMapping("/api/v1/clients/{clientId}/credit-balance")
    fun getClientCreditBalance(
        @PathVariable clientId: Long
    ): ResponseEntity<ApiResponse<CreditBalanceResponse>> {
        val result = orderQueryService.getClientCreditBalance(clientId)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    / **
     * 제품 주문정보 조회
     * GET /api/v1/products/{productCode}/order-info
     * /
    @GetMapping("/api/v1/products/{productCode}/order-info")
    fun getProductOrderInfo(
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<ProductOrderInfoResponse>> {
        val result = orderQueryService.getProductOrderInfo(productCode)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }
}
*/
