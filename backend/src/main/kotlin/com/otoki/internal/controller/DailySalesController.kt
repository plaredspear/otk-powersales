package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.DailySalesCreateRequest
import com.otoki.internal.dto.response.DailySalesCreateResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.DailySalesService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 일매출 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/daily-sales")
class DailySalesController(
    private val dailySalesService: DailySalesService
) {

    /**
     * 일매출 등록
     * POST /api/v1/events/{eventId}/daily-sales
     *
     * - multipart/form-data로 전송
     * - 대표제품 또는 기타제품 중 최소 하나 입력 필수
     * - 사진 파일 필수
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun registerDailySales(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable eventId: String,
        @Valid @ModelAttribute request: DailySalesCreateRequest
    ): ResponseEntity<ApiResponse<DailySalesCreateResponse>> {
        val response = dailySalesService.registerDailySales(
            userId = principal.userId,
            eventId = eventId,
            request = request
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 일매출 임시저장
     * POST /api/v1/events/{eventId}/daily-sales/draft
     *
     * - multipart/form-data로 전송
     * - 모든 필드 선택적 (부분 저장 허용)
     * - 기존 DRAFT가 있으면 업데이트 (upsert)
     */
    @PostMapping("/draft", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun saveDailySalesDraft(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable eventId: String,
        @ModelAttribute request: DailySalesCreateRequest
    ): ResponseEntity<ApiResponse<DailySalesCreateResponse>> {
        val response = dailySalesService.saveDailySalesDraft(
            userId = principal.userId,
            eventId = eventId,
            request = request
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
