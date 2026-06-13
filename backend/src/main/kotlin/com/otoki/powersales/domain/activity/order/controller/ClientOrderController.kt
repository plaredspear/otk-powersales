package com.otoki.powersales.domain.activity.order.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.domain.activity.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.ClientOrderSummaryResponse
import com.otoki.powersales.domain.activity.order.service.ClientOrderQueryService
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 거래처별 출하 주문 API Controller (Spec #593).
 *
 * 레거시: Heroku `OrderController#orderDetail(type=client)` → SF `IF_REST_MOBILE_ClientOrderDetail`.
 * 신규: `erp_order` / `erp_order_product` 테이블 직접 조회 (#561 적재 데이터).
 */
@RestController
@RequestMapping("/api/v1/mobile/client-orders")
class ClientOrderController(
    private val clientOrderQueryService: ClientOrderQueryService
) {

    /**
     * 거래처별 주문 목록 조회 (거래처별 주문 탭).
     *
     * 레거시: Heroku `OrderController#clientlistapi` → SF `ClientOrderSearch`.
     */
    @GetMapping
    fun getClientOrders(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam clientId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) deliveryDate: LocalDate?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<Page<ClientOrderSummaryResponse>>> {
        val response = clientOrderQueryService.getClientOrders(clientId, deliveryDate, page, size)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    @GetMapping("/{sapOrderNumber}")
    fun getClientOrderDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable sapOrderNumber: String
    ): ResponseEntity<ApiResponse<ClientOrderDetailResponse>> {
        val response = clientOrderQueryService.getClientOrderDetail(principal.userId, sapOrderNumber)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
