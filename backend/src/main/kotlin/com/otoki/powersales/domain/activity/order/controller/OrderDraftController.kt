package com.otoki.powersales.domain.activity.order.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import com.otoki.powersales.domain.activity.order.dto.request.OrderDraftRequest
import com.otoki.powersales.domain.activity.order.dto.response.OrderDraftDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderDraftSaveResponse
import com.otoki.powersales.domain.activity.order.service.OrderDraftService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 주문 임시저장 (Draft) API — Spec #596.
 *
 * - `POST /api/v1/mobile/orders/draft` — 등록 (UPSERT, 사번당 1건).
 * - `GET /api/v1/mobile/orders/draft` — 본인 임시저장 단건 조회 (없으면 `data: null`).
 * - `DELETE /api/v1/mobile/orders/draft` — 본인 임시저장 삭제 (없어도 204 멱등).
 */
@RestController
@RequestMapping("/api/v1/mobile/orders/draft")
class OrderDraftController(
    private val orderDraftService: OrderDraftService,
) {

    @PostMapping
    fun save(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: OrderDraftRequest,
    ): ResponseEntity<ApiResponse<OrderDraftSaveResponse>> {
        val response = orderDraftService.save(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "임시저장이 완료되었습니다"))
    }

    @GetMapping
    fun get(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<ApiResponse<OrderDraftDetailResponse?>> {
        val response = orderDraftService.findByUserId(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    @DeleteMapping
    fun delete(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<Void> {
        orderDraftService.deleteByEmployeeId(principal.userId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
