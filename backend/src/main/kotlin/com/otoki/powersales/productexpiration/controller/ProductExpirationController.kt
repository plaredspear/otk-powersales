package com.otoki.powersales.productexpiration.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.productexpiration.dto.request.ProductExpirationBatchDeleteRequest
import com.otoki.powersales.productexpiration.dto.request.ProductExpirationCreateRequest
import com.otoki.powersales.productexpiration.dto.request.ProductExpirationUpdateRequest
import com.otoki.powersales.productexpiration.dto.response.ProductExpirationBatchDeleteResponse
import com.otoki.powersales.productexpiration.dto.response.ProductExpirationItemResponse
import com.otoki.powersales.productexpiration.service.ProductExpirationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/mobile/product-expiration")
class ProductExpirationController(
    private val productExpirationService: ProductExpirationService
) {

    @GetMapping
    fun getProductExpirationList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) accountCode: String?,
        @RequestParam fromDate: String,
        @RequestParam toDate: String
    ): ResponseEntity<ApiResponse<List<ProductExpirationItemResponse>>> {
        val response = productExpirationService.getProductExpirationList(principal.userId, accountCode, fromDate, toDate)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    @PostMapping
    fun createProductExpiration(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ProductExpirationCreateRequest
    ): ResponseEntity<ApiResponse<ProductExpirationItemResponse>> {
        val response = productExpirationService.createProductExpiration(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "유통기한이 등록되었습니다"))
    }

    @PutMapping("/{seq}")
    fun updateProductExpiration(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable seq: Int,
        @Valid @RequestBody request: ProductExpirationUpdateRequest
    ): ResponseEntity<ApiResponse<ProductExpirationItemResponse>> {
        val response = productExpirationService.updateProductExpiration(principal.userId, seq, request)
        return ResponseEntity.ok(ApiResponse.success(response, "유통기한이 수정되었습니다"))
    }

    @DeleteMapping("/{seq}")
    fun deleteProductExpiration(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable seq: Int
    ): ResponseEntity<ApiResponse<Any?>> {
        productExpirationService.deleteProductExpiration(principal.userId, seq)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "유통기한이 삭제되었습니다"))
    }

    @PostMapping("/batch-delete")
    fun deleteProductExpirationBatch(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ProductExpirationBatchDeleteRequest
    ): ResponseEntity<ApiResponse<ProductExpirationBatchDeleteResponse>> {
        val response = productExpirationService.deleteProductExpirationBatch(principal.userId, request)
        return ResponseEntity.ok(
            ApiResponse.success(response, "${response.deletedCount}건의 유통기한이 삭제되었습니다")
        )
    }
}
