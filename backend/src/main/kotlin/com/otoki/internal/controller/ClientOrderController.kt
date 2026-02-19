/*
package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.ClientOrderDetailResponse
import com.otoki.internal.dto.response.ClientOrderSummaryResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ClientOrderService
import org.springframework.data.domain.Page
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/ **
 * 거래처별 주문 API Controller (F28)
 * /
@RestController
@RequestMapping("/api/v1/client-orders")
class ClientOrderController(
    private val clientOrderService: ClientOrderService
) {

    / **
     * 거래처별 주문 목록 조회
     * GET /api/v1/client-orders?clientId=123&deliveryDate=2026-02-10&page=0&size=20
     *
     * @param principal 인증된 사용자 정보
     * @param clientId 거래처 ID (필수)
     * @param deliveryDate 납기일 (선택, 기본: 오늘)
     * @param page 페이지 번호 (선택, 기본: 0)
     * @param size 페이지 크기 (선택, 기본: 20)
     * /
    @GetMapping
    fun getClientOrders(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam clientId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) deliveryDate: LocalDate?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<Page<ClientOrderSummaryResponse>>> {
        val result = clientOrderService.getClientOrders(
            userId = principal.userId,
            clientId = clientId,
            deliveryDate = deliveryDate,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    / **
     * 거래처별 주문 상세 조회
     * GET /api/v1/client-orders/{sapOrderNumber}
     *
     * @param principal 인증된 사용자 정보
     * @param sapOrderNumber SAP 주문번호
     * /
    @GetMapping("/{sapOrderNumber}")
    fun getClientOrderDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable sapOrderNumber: String
    ): ResponseEntity<ApiResponse<ClientOrderDetailResponse>> {
        val result = clientOrderService.getClientOrderDetail(
            userId = principal.userId,
            sapOrderNumber = sapOrderNumber
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }
}
*/
