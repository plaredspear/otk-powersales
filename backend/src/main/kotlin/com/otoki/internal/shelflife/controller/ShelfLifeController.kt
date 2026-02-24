package com.otoki.internal.shelflife.controller

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * ShelfLifeService 비활성화에 따라 Controller도 주석 처리.

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.shelflife.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.shelflife.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.shelflife.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.shelflife.dto.response.ShelfLifeBatchDeleteResponse
import com.otoki.internal.shelflife.dto.response.ShelfLifeItemResponse
import com.otoki.internal.shelflife.dto.response.ShelfLifeListResponse
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.shelflife.service.ShelfLifeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/shelf-life")
class ShelfLifeController(
    private val shelfLifeService: ShelfLifeService
) {

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

    @GetMapping("/{shelfLifeId}")
    fun getShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable shelfLifeId: Long
    ): ResponseEntity<ApiResponse<ShelfLifeItemResponse>> {
        val response = shelfLifeService.getShelfLife(principal.userId, shelfLifeId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    @PostMapping
    fun createShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ShelfLifeCreateRequest
    ): ResponseEntity<ApiResponse<ShelfLifeItemResponse>> {
        val response = shelfLifeService.createShelfLife(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "유통기한이 등록되었습니다"))
    }

    @PutMapping("/{shelfLifeId}")
    fun updateShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable shelfLifeId: Long,
        @Valid @RequestBody request: ShelfLifeUpdateRequest
    ): ResponseEntity<ApiResponse<ShelfLifeItemResponse>> {
        val response = shelfLifeService.updateShelfLife(principal.userId, shelfLifeId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "유통기한이 수정되었습니다"))
    }

    @DeleteMapping("/{shelfLifeId}")
    fun deleteShelfLife(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable shelfLifeId: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        shelfLifeService.deleteShelfLife(principal.userId, shelfLifeId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "유통기한이 삭제되었습니다"))
    }

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

--- */
