package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.CategoryTree
import com.otoki.internal.admin.dto.response.ProductListResponse
import com.otoki.internal.admin.service.AdminProductService
import com.otoki.internal.common.dto.ApiResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/products")
@Validated
class AdminProductController(
    private val adminProductService: AdminProductService
) {

    @GetMapping
    fun getProducts(
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false) category1: String?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false) productStatus: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<ProductListResponse>> {
        val response = adminProductService.getProducts(
            keyword = keyword,
            category1 = category1,
            category2 = category2,
            category3 = category3,
            productStatus = productStatus,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<ApiResponse<List<CategoryTree>>> {
        val response = adminProductService.getCategories()
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
