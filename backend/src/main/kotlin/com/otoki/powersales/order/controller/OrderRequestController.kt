package com.otoki.powersales.order.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.order.dto.response.OrderRequestCreateResponse
import com.otoki.powersales.order.dto.response.OrderRequestDetailResponse
import com.otoki.powersales.order.dto.response.OrderRequestListResponse
import com.otoki.powersales.order.service.OrderRequestCreateService
import com.otoki.powersales.order.service.OrderRequestService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/mobile")
class OrderRequestController(
    private val orderRequestService: OrderRequestService,
    private val orderRequestCreateService: OrderRequestCreateService,
) {

    /**
     * GET /api/v1/mobile/me/order-requests
     */
    @GetMapping("/me/order-requests")
    fun getMyOrderRequests(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) clientId: Long?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        deliveryDateFrom: LocalDate?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        deliveryDateTo: LocalDate?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?,
    ): ResponseEntity<ApiResponse<OrderRequestListResponse>> {
        val response = orderRequestService.getMyOrderRequests(
            userId = principal.userId,
            accountId = clientId,
            status = status,
            deliveryDateFrom = deliveryDateFrom,
            deliveryDateTo = deliveryDateTo,
            sortBy = sortBy,
            sortDir = sortDir,
        )
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * GET /api/v1/mobile/me/order-requests/{orderRequestId} — 본인 주문요청 상세 조회 (Spec #595).
     */
    @GetMapping("/me/order-requests/{orderRequestId}")
    fun getOrderRequestDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable orderRequestId: Long,
    ): ResponseEntity<ApiResponse<OrderRequestDetailResponse>> {
        val response = orderRequestService.getOrderRequestDetail(orderRequestId, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * POST /api/v1/mobile/order-requests — 주문 등록 (Spec #592).
     *
     * 멱등키는 본문 `clientRequestId` 또는 헤더 `Idempotency-Key` 로 전달 가능. 헤더 우선.
     */
    @PostMapping("/order-requests")
    fun createOrderRequest(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
        @Valid @RequestBody request: OrderRequestCreateRequest,
    ): ResponseEntity<ApiResponse<OrderRequestCreateResponse>> {
        val resolved = if (!idempotencyKey.isNullOrBlank()) {
            request.copy(clientRequestId = idempotencyKey)
        } else request
        val response = orderRequestCreateService.create(principal.userId, resolved)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "주문 요청이 접수되었습니다"))
    }
}
