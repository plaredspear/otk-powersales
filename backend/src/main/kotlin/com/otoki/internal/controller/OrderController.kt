/*
package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.OrderCancelRequest
import com.otoki.internal.dto.request.OrderDraftRequest
import com.otoki.internal.dto.response.OrderCancelResponse
import com.otoki.internal.dto.response.OrderDetailResponse
import com.otoki.internal.dto.response.OrderSubmitResponse
import com.otoki.internal.dto.response.OrderSummaryResponse
import com.otoki.internal.dto.response.ValidationResultResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderService
import com.otoki.internal.service.OrderSubmitService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/ **
 * 주문 API Controller
 * /
@RestController
@RequestMapping("/api/v1/me")
class OrderController(
    private val orderService: OrderService,
    private val orderSubmitService: OrderSubmitService
) {

    / **
     * 내 주문 목록 조회
     * GET /api/v1/me/orders
     * /
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

    / **
     * 주문 상세 조회
     * GET /api/v1/me/orders/{orderId}
     *
     * @param principal 인증된 사용자 정보
     * @param orderId 주문 고유 ID
     * /
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

    / **
     * 주문 재전송
     * POST /api/v1/me/orders/{orderId}/resend
     *
     * @param principal 인증된 사용자 정보
     * @param orderId 주문 고유 ID
     * /
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

    / **
     * 주문 취소
     * POST /api/v1/me/orders/{orderId}/cancel
     *
     * @param principal 인증된 사용자 정보
     * @param orderId 주문 고유 ID
     * @param request 취소할 제품코드 목록
     * /
    @PostMapping("/orders/{orderId}/cancel")
    fun cancelOrder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable orderId: Long,
        @Valid @RequestBody request: OrderCancelRequest
    ): ResponseEntity<ApiResponse<OrderCancelResponse>> {
        val result = orderService.cancelOrder(
            userId = principal.userId,
            orderId = orderId,
            productCodes = request.productCodes
        )
        return ResponseEntity.ok(ApiResponse.success(result, "주문이 취소되었습니다"))
    }

    / **
     * 주문서 유효성 체크
     * POST /api/v1/me/orders/validate
     * /
    @PostMapping("/orders/validate")
    fun validateOrder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: OrderDraftRequest
    ): ResponseEntity<ApiResponse<ValidationResultResponse>> {
        val result = orderSubmitService.validateOrder(
            userId = principal.userId,
            request = request
        )
        val message = if (result.isValid) "유효성 검증 통과" else "유효성 검증 실패"
        return ResponseEntity.ok(ApiResponse.success(result, message))
    }

    / **
     * 주문서 승인요청(제출)
     * POST /api/v1/me/orders
     * /
    @PostMapping("/orders")
    fun submitOrder(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: OrderDraftRequest
    ): ResponseEntity<ApiResponse<OrderSubmitResponse>> {
        val result = orderSubmitService.submitOrder(
            userId = principal.userId,
            request = request
        )
        val message = if (result.approvalStatus == "APPROVED") "주문이 승인되었습니다" else "주문 전송에 실패했습니다"
        return ResponseEntity.ok(ApiResponse.success(result, message))
    }
}
*/
