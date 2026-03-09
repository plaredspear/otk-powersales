package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.request.PromotionTypeRequest
import com.otoki.internal.admin.dto.response.PromotionTypeResponse
import com.otoki.internal.admin.service.AdminPromotionTypeService
import com.otoki.internal.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/promotion-types")
class AdminPromotionTypeController(
    private val adminPromotionTypeService: AdminPromotionTypeService
) {

    @GetMapping
    fun getPromotionTypes(): ResponseEntity<ApiResponse<List<PromotionTypeResponse>>> {
        val response = adminPromotionTypeService.getPromotionTypes()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    fun createPromotionType(
        @Valid @RequestBody request: PromotionTypeRequest
    ): ResponseEntity<ApiResponse<PromotionTypeResponse>> {
        val response = adminPromotionTypeService.createPromotionType(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    fun updatePromotionType(
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionTypeRequest
    ): ResponseEntity<ApiResponse<PromotionTypeResponse>> {
        val response = adminPromotionTypeService.updatePromotionType(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    fun deletePromotionType(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionTypeService.deletePromotionType(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }
}
