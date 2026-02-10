package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.OrderSummaryResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderService
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
     *
     * @param principal 인증된 사용자 정보
     * @param clientId 거래처 ID (선택)
     * @param status 승인상태 (선택: APPROVED, PENDING, SEND_FAILED, RESEND)
     * @param deliveryDateFrom 납기일 시작 (선택, YYYY-MM-DD)
     * @param deliveryDateTo 납기일 종료 (선택, YYYY-MM-DD)
     * @param sortBy 정렬 기준 (선택: orderDate, deliveryDate, totalAmount. 기본: orderDate)
     * @param sortDir 정렬 방향 (선택: ASC, DESC. 기본: DESC)
     * @param page 페이지 번호 (선택, 기본: 0)
     * @param size 페이지 크기 (선택, 기본: 20)
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
}
