package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.InspectionCreateRequest
import com.otoki.internal.dto.response.*
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.InspectionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 현장 점검 API Controller
 */
@RestController
@RequestMapping("/api/v1/inspections")
class InspectionController(
    private val inspectionService: InspectionService
) {

    /**
     * 현장 점검 목록 조회
     * GET /api/v1/inspections
     *
     * @param principal 인증된 사용자
     * @param storeId 거래처 ID (선택)
     * @param category 분류 (선택: OWN, COMPETITOR)
     * @param fromDate 점검일 시작 (YYYY-MM-DD)
     * @param toDate 점검일 종료 (YYYY-MM-DD)
     */
    @GetMapping
    fun getInspectionList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) storeId: Long?,
        @RequestParam(required = false) category: String?,
        @RequestParam fromDate: String,
        @RequestParam toDate: String
    ): ResponseEntity<ApiResponse<InspectionListResponse>> {
        val result = inspectionService.getInspectionList(
            userId = principal.userId,
            fromDate = fromDate,
            toDate = toDate,
            storeId = storeId,
            category = category
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 현장 점검 상세 조회
     * GET /api/v1/inspections/{inspectionId}
     *
     * @param principal 인증된 사용자
     * @param inspectionId 점검 ID
     */
    @GetMapping("/{inspectionId}")
    fun getInspectionDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable inspectionId: Long
    ): ResponseEntity<ApiResponse<InspectionDetailResponse>> {
        val result = inspectionService.getInspectionDetail(
            inspectionId = inspectionId,
            userId = principal.userId
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 현장 점검 등록
     * POST /api/v1/inspections
     *
     * @param principal 인증된 사용자
     * @param request 등록 요청 (multipart form fields)
     * @param photos 사진 파일 (1~2장)
     */
    @PostMapping(consumes = ["multipart/form-data"])
    fun createInspection(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @ModelAttribute request: InspectionCreateRequest,
        @RequestParam photos: Array<MultipartFile>
    ): ResponseEntity<ApiResponse<InspectionListItemResponse>> {
        val result = inspectionService.createInspection(
            request = request,
            photos = photos,
            userId = principal.userId
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "현장 점검이 등록되었습니다"))
    }

    /**
     * 테마 목록 조회
     * GET /api/v1/inspections/themes
     */
    @GetMapping("/themes")
    fun getThemes(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<ThemeResponse>>> {
        val result = inspectionService.getThemes()
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 현장 유형 목록 조회
     * GET /api/v1/inspections/field-types
     */
    @GetMapping("/field-types")
    fun getFieldTypes(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<FieldTypeResponse>>> {
        val result = inspectionService.getFieldTypes()
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }
}
