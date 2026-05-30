package com.otoki.powersales.inspection.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.inspection.dto.request.InspectionRegisterRequest
import com.otoki.powersales.inspection.dto.response.InspectionDetailResponse
import com.otoki.powersales.inspection.dto.response.InspectionFieldTypeResponse
import com.otoki.powersales.inspection.dto.response.InspectionListItem
import com.otoki.powersales.inspection.dto.response.ThemeResponse
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.service.SiteActivityService
import jakarta.validation.Valid
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 현장점검 API Controller (mobile).
 *
 * 레거시 SF `IF_REST_MOBILE_SiteActivitySearch` / `IF_REST_MOBILE_SiteActivityRegist` 등가.
 */
@RestController
@RequestMapping("/api/v1/mobile/inspections")
class InspectionController(
    private val siteActivityService: SiteActivityService
) {

    @GetMapping
    fun getList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) accountId: Int?,
        @RequestParam(required = false) category: InspectionCategory?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): ResponseEntity<ApiResponse<List<InspectionListItem>>> {
        val result = siteActivityService.getList(principal.userId, accountId, category, fromDate, toDate)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/themes")
    fun getThemes(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<ThemeResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(siteActivityService.getThemes()))
    }

    @GetMapping("/field-types")
    fun getFieldTypes(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<InspectionFieldTypeResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(siteActivityService.getFieldTypes()))
    }

    @GetMapping("/{inspectionId}")
    fun getDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable inspectionId: Long
    ): ResponseEntity<ApiResponse<InspectionDetailResponse>> {
        val result = siteActivityService.getDetail(inspectionId, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @PostMapping(consumes = ["multipart/form-data"])
    fun register(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestPart("request") request: InspectionRegisterRequest,
        @RequestPart(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<InspectionListItem>> {
        val result = siteActivityService.register(principal.userId, request, photos)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "현장점검이 등록되었습니다"))
    }
}
