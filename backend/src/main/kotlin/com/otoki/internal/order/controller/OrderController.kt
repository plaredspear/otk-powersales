/* Order 모듈 전체 비활성화 — DB 테이블 미존재
package com.otoki.internal.order.controller

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.order.dto.response.OrderSummaryResponse
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.order.service.OrderService
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

    // TODO: Spec #XXX에서 활성화 — GET /api/v1/me/orders/{orderId} (주문 상세)
    // TODO: Spec #XXX에서 활성화 — POST /api/v1/me/orders/{orderId}/resend (재전송)
    // TODO: Spec #XXX에서 활성화 — POST /api/v1/me/orders/{orderId}/cancel (취소)
    // TODO: Spec #XXX에서 활성화 — POST /api/v1/me/orders/validate (유효성 체크)
    // TODO: Spec #XXX에서 활성화 — POST /api/v1/me/orders (주문서 제출)
}
*/