package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.OrderDraftRequest
import com.otoki.internal.dto.response.DraftSavedResponse
import com.otoki.internal.dto.response.OrderDraftResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderDraftService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 임시저장 주문서 API Controller
 */
@RestController
@RequestMapping("/api/v1/me/orders/draft")
class OrderDraftController(
    private val orderDraftService: OrderDraftService
) {

    /**
     * 임시저장 주문서 조회
     * GET /api/v1/me/orders/draft
     */
    @GetMapping
    fun getMyDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<OrderDraftResponse?>> {
        val result = orderDraftService.getMyDraft(userId = principal.userId)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 주문서 임시저장
     * POST /api/v1/me/orders/draft
     */
    @PostMapping
    fun saveDraft(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: OrderDraftRequest
    ): ResponseEntity<ApiResponse<DraftSavedResponse>> {
        val result = orderDraftService.saveDraft(userId = principal.userId, request = request)
        return ResponseEntity.ok(ApiResponse.success(result, "임시 저장되었습니다"))
    }

    /**
     * 임시저장 주문서 삭제
     * DELETE /api/v1/me/orders/draft
     */
    @DeleteMapping
    fun deleteDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Any?>> {
        orderDraftService.deleteDraft(userId = principal.userId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "임시 저장 주문서가 삭제되었습니다"))
    }
}
