package com.otoki.powersales.order.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.dto.response.OrderRequestListResponse
import com.otoki.powersales.order.service.OrderRequestService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/mobile/me")
class OrderRequestController(
    private val orderRequestService: OrderRequestService,
) {

    /**
     * GET /api/v1/mobile/me/order-requests
     */
    @GetMapping("/order-requests")
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
}
