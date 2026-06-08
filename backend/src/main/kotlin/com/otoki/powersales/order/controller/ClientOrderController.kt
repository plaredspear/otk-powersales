package com.otoki.powersales.order.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.order.dto.response.OrderHistoryGroupResponse
import com.otoki.powersales.order.service.ClientOrderQueryService
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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

    @GetMapping("/{sapOrderNumber}")
    fun getClientOrderDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable sapOrderNumber: String
    ): ResponseEntity<ApiResponse<ClientOrderDetailResponse>> {
        val response = clientOrderQueryService.getClientOrderDetail(principal.userId, sapOrderNumber)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * 거래처 주문이력(제품 선택용) 조회.
     * GET /api/v1/mobile/client-orders/product-history
     *
     * 레거시: Heroku `OrderController#searchOrderHistory` → SF `OrderHistory`.
     * 제품추가 팝업 "주문 이력" 탭에서 거래처+기간으로 주문한 제품을 주문일별로 조회한다.
     *
     * @param accountCode 거래처 SAP 코드
     * @param startDate 주문일 시작 (YYYY-MM-DD)
     * @param endDate 주문일 종료 (YYYY-MM-DD)
     */
    @GetMapping("/product-history")
    fun getAccountOrderHistory(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam accountCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<ApiResponse<List<OrderHistoryGroupResponse>>> {
        val response = clientOrderQueryService.getAccountOrderHistory(
            principal.userId, accountCode, startDate, endDate
        )
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
