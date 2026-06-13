package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.domain.foundation.product.dto.request.InventorySearchRequest
import com.otoki.powersales.domain.foundation.product.dto.request.ProductExportRequest
import com.otoki.powersales.domain.foundation.product.dto.response.CategoryTree
import com.otoki.powersales.domain.foundation.product.dto.response.InventorySearchResponse
import com.otoki.powersales.domain.foundation.product.dto.response.ProductDetail
import com.otoki.powersales.domain.foundation.product.dto.response.ProductListResponse
import com.otoki.powersales.domain.foundation.product.service.AdminProductExportService
import com.otoki.powersales.domain.foundation.product.service.AdminProductInventoryService
import com.otoki.powersales.domain.foundation.product.service.AdminProductService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/v1/admin/products")
@Validated
class AdminProductController(
    private val adminProductService: AdminProductService,
    private val adminProductInventoryService: AdminProductInventoryService,
    private val adminProductExportService: AdminProductExportService
) {

    @GetMapping
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
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

    /**
     * 행사마스터 등록/수정 화면의 제품 lookup search — SF Promotion__c.PrimaryProductId__c lookup 정합.
     *
     * SF 의 lookup search 는 Product FLS/object access 와 무관하게 화면 권한 (Promotion CRUD) 으로
     * 작동 — 본 endpoint 는 SF 메커니즘 정합. 결과는 동일 [ProductListResponse] 재사용
     * (검색/필터 평가는 `adminProductService.getProducts` 가 그대로 적용).
     */
    @GetMapping("/lookup")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun lookupProducts(
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<ProductListResponse>> {
        val response = adminProductService.getProducts(
            keyword = keyword,
            category1 = null,
            category2 = null,
            category3 = null,
            productStatus = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 물류 클레임 등록/수정 화면의 제품 lookup search — SF Claim__c.ProductId__c Lookup 정합.
     *
     * SF 의 lookup search 는 Product FLS/object access 와 무관하게 화면 권한 (Suggestion/Claim CRUD)
     * 으로 작동 — 본 endpoint 는 SF 메커니즘 정합.
     */
    @GetMapping("/lookup-for-claim")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.READ)
    fun lookupProductsForClaim(
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<ProductListResponse>> {
        val response = adminProductService.getProducts(
            keyword = keyword,
            category1 = null,
            category2 = null,
            category3 = null,
            productStatus = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/categories")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun getCategories(): ResponseEntity<ApiResponse<List<CategoryTree>>> {
        val response = adminProductService.getCategories()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{productCode}")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun getProductDetail(
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<ProductDetail>> {
        val response = adminProductService.getProductDetail(productCode)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/inventory-search")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun searchInventory(
        @Valid @RequestBody request: InventorySearchRequest
    ): ResponseEntity<ApiResponse<InventorySearchResponse>> {
        val response = adminProductInventoryService.searchInventory(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/export-excel")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun exportSelectedProductsExcel(
        @Valid @RequestBody request: ProductExportRequest
    ): ResponseEntity<ByteArrayResource> {
        val bytes = adminProductExportService.exportSelectedProducts(request.productCodes!!)
        val fileName = URLEncoder.encode("선택제품.xlsx", StandardCharsets.UTF_8)
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$fileName\"; filename*=UTF-8''$fileName"
            )
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(ByteArrayResource(bytes))
    }
}
