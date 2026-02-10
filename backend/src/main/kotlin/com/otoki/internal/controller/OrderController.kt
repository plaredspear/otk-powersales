package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.OrderDetailResponse
import com.otoki.internal.dto.response.OrderSummaryResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderService
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 주문 API Controller
 */
@RestController
@RequestMapping("/api/v1/me")
class OrderController(
    private val orderService: OrderService
) {

    /**
     * 내 주문 목록 조회
     * GET /api/v1/me/orders
     */
    @GetMapping("/orders")
    fun getMyOrders(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) clientId: Long?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) deliveryDateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) deliveryDateTo: LocalDate?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> {
        val result = orderService.getMyOrders(
            userId = principal.userId,
            storeId = clientId,
            status = status,
            deliveryDateFrom = deliveryDateFrom,
            deliveryDateTo = deliveryDateTo,
            sortBy = sortBy,
            sortDir = sortDir,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 주문 상세 조회
     * GET /api/v1/me/orders/{orderId}
     *
     * @param principal 인증된 사용자 정보
     * @param orderId 주문 고유 ID
     */
    @GetMapping("/orders/{orderId}")
    fun getOrderDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<OrderDetailResponse>> {
        val result = orderService.getOrderDetail(
            userId = principal.userId,
            orderId = orderId
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 주문 재전송
     * POST /api/v1/me/orders/{orderId}/resend
     *
     * @param principal 인증된 사용자 정보
     * @param orderId 주문 고유 ID
     */
    @PostMapping("/orders/{orderId}/resend")
    fun resendOrder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable orderId: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        orderService.resendOrder(
            userId = principal.userId,
            orderId = orderId
        )
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "주문이 재전송되었습니다"))
    }
}
