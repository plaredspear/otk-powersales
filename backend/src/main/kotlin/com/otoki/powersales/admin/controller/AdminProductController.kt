package com.otoki.powersales.admin.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.product.dto.request.InventorySearchRequest
import com.otoki.powersales.product.dto.request.ProductExportRequest
import com.otoki.powersales.product.dto.response.CategoryTree
import com.otoki.powersales.product.dto.response.InventorySearchResponse
import com.otoki.powersales.product.dto.response.ProductDetail
import com.otoki.powersales.product.dto.response.ProductListResponse
import com.otoki.powersales.product.service.AdminProductExportService
import com.otoki.powersales.product.service.AdminProductInventoryService
import com.otoki.powersales.product.service.AdminProductService
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

    @GetMapping("/{productCode}")
    fun getProductDetail(
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<ProductDetail>> {
        val response = adminProductService.getProductDetail(productCode)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/inventory-search")
    fun searchInventory(
        @Valid @RequestBody request: InventorySearchRequest
    ): ResponseEntity<ApiResponse<InventorySearchResponse>> {
        val response = adminProductInventoryService.searchInventory(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/export-excel")
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
