/*
package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.EventListRequest
import com.otoki.internal.dto.response.DailySalesListResponse
import com.otoki.internal.dto.response.EventDetailResponse
import com.otoki.internal.dto.response.EventListResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.EventService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/ **
 * 행사 관련 API Controller
 * /
@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val eventService: EventService
) {

    / **
     * 행사 목록 조회
     * GET /api/v1/events
     * /
    @GetMapping
    fun getEvents(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid request: EventListRequest
    ): ResponseEntity<ApiResponse<EventListResponse>> {
        val response = eventService.getEvents(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    / **
     * 행사 상세 조회
     * GET /api/v1/events/{eventId}
     * /
    @GetMapping("/{eventId}")
    fun getEventDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable eventId: String
    ): ResponseEntity<ApiResponse<EventDetailResponse>> {
        val response = eventService.getEventDetail(principal.userId, eventId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    / **
     * 일별 매출 목록 조회
     * GET /api/v1/events/{eventId}/daily-sales
     * /
    @GetMapping("/{eventId}/daily-sales")
    fun getDailySales(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable eventId: String
    ): ResponseEntity<ApiResponse<DailySalesListResponse>> {
        val response = eventService.getDailySales(principal.userId, eventId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
*/
