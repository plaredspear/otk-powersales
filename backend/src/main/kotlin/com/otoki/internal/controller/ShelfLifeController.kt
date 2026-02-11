package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.dto.response.ShelfLifeBatchDeleteResponse
import com.otoki.internal.dto.response.ShelfLifeItemResponse
import com.otoki.internal.dto.response.ShelfLifeListResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ShelfLifeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 유통기한 관리 API Controller
 */
@RestController
@RequestMapping("/api/v1/shelf-life")
class ShelfLifeController(
    private val shelfLifeService: ShelfLifeService
) {

    /**
     * 유통기한 목록 조회
     * GET /api/v1/shelf-life?storeId=&fromDate=&toDate=
     */
    @GetMapping
    fun getShelfLifeList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) storeId: Long?,
        @RequestParam fromDate: String,
        @RequestParam toDate: String
    ): ResponseEntity<ApiResponse<ShelfLifeListResponse>> {
        val response = shelfLifeService.getShelfLifeList(principal.userId, storeId, fromDate, toDate)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * 유통기한 단건 조회
     * GET /api/v1/shelf-life/{shelfLifeId}
     */
    @GetMapping("/{shelfLifeId}")
    fun getShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable shelfLifeId: Long
    ): ResponseEntity<ApiResponse<ShelfLifeItemResponse>> {
        val response = shelfLifeService.getShelfLife(principal.userId, shelfLifeId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * 유통기한 등록
     * POST /api/v1/shelf-life
     */
    @PostMapping
    fun createShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ShelfLifeCreateRequest
    ): ResponseEntity<ApiResponse<ShelfLifeItemResponse>> {
        val response = shelfLifeService.createShelfLife(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "유통기한이 등록되었습니다"))
    }

    /**
     * 유통기한 수정
     * PUT /api/v1/shelf-life/{shelfLifeId}
     */
    @PutMapping("/{shelfLifeId}")
    fun updateShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable shelfLifeId: Long,
        @Valid @RequestBody request: ShelfLifeUpdateRequest
    ): ResponseEntity<ApiResponse<ShelfLifeItemResponse>> {
        val response = shelfLifeService.updateShelfLife(principal.userId, shelfLifeId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "유통기한이 수정되었습니다"))
    }

    /**
     * 유통기한 단건 삭제
     * DELETE /api/v1/shelf-life/{shelfLifeId}
     */
    @DeleteMapping("/{shelfLifeId}")
    fun deleteShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable shelfLifeId: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        shelfLifeService.deleteShelfLife(principal.userId, shelfLifeId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "유통기한이 삭제되었습니다"))
    }

    /**
     * 유통기한 일괄 삭제
     * POST /api/v1/shelf-life/batch-delete
     */
    @PostMapping("/batch-delete")
    fun deleteShelfLifeBatch(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ShelfLifeBatchDeleteRequest
    ): ResponseEntity<ApiResponse<ShelfLifeBatchDeleteResponse>> {
        val response = shelfLifeService.deleteShelfLifeBatch(principal.userId, request)
        return ResponseEntity.ok(
            ApiResponse.success(response, "${response.deletedCount}건의 유통기한이 삭제되었습니다")
        )
    }
}
