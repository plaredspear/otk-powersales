package com.otoki.powersales.domain.activity.order.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.domain.activity.order.dto.request.OrderCancelRequest
import com.otoki.powersales.domain.activity.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.domain.activity.order.dto.response.OrderCancelResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderHistoryGroupResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestCreateResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestListResponse
import com.otoki.powersales.domain.activity.order.service.OrderCancelService
import com.otoki.powersales.domain.activity.order.service.OrderRequestCreateService
import com.otoki.powersales.domain.activity.order.service.OrderRequestService
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
    private val orderCancelService: OrderCancelService,
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
     * GET /api/v1/mobile/me/order-requests/product-history — 거래처 주문이력(제품 선택용).
     *
     * 레거시: SF `IF_REST_MOBILE_OrderHistory`. 유통기한 등록 "제품 추가" 팝업의 주문 이력 탭에서
     * 거래처+기간으로 본인이 주문한 제품을 주문일별로 조회한다.
     *
     * @param accountCode 거래처 SAP 코드 (Account.externalKey)
     * @param startDate 주문일 시작 (YYYY-MM-DD)
     * @param endDate 주문일 종료 (YYYY-MM-DD)
     */
    @GetMapping("/me/order-requests/product-history")
    fun getAccountOrderHistory(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam accountCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<ApiResponse<List<OrderHistoryGroupResponse>>> {
        val response = orderRequestService.getAccountOrderHistory(
            principal.userId, accountCode, startDate, endDate,
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
     * POST /api/v1/mobile/me/order-requests/{orderRequestId}/cancel — 본인 주문 취소 (Spec #597).
     *
     * `orderProductIds` 빈 배열이면 전체 라인 취소, 그 외는 부분 취소 (라인 PK 배열).
     */
    @PostMapping("/me/order-requests/{orderRequestId}/cancel")
    fun cancelOrderRequest(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable orderRequestId: Long,
        @RequestBody(required = false) request: OrderCancelRequest?,
    ): ResponseEntity<ApiResponse<OrderCancelResponse>> {
        val orderProductIds = request?.orderProductIds.orEmpty()
        val response = orderCancelService.cancel(orderRequestId, principal.userId, orderProductIds)
        return ResponseEntity.ok(ApiResponse.success(response, "주문이 취소되었습니다"))
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
