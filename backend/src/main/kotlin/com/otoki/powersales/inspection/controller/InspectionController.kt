package com.otoki.powersales.inspection.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import com.otoki.powersales.inspection.dto.request.InspectionRegisterRequest
import com.otoki.powersales.inspection.dto.request.SiteActivityDraftRequest
import com.otoki.powersales.inspection.dto.response.InspectionDetailResponse
import com.otoki.powersales.inspection.dto.response.InspectionFieldTypeResponse
import com.otoki.powersales.inspection.dto.response.InspectionListItem
import com.otoki.powersales.inspection.dto.response.SiteActivityDraftResponse
import com.otoki.powersales.inspection.dto.response.ThemeResponse
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.service.SiteActivityDraftService
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
    private val siteActivityService: SiteActivityService,
    private val siteActivityDraftService: SiteActivityDraftService
) {

    @GetMapping
    fun getList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) accountId: Long?,
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
        return ResponseEntity.ok(ApiResponse.success(siteActivityService.getThemes(principal.userId)))
    }

    @GetMapping("/field-types")
    fun getFieldTypes(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<InspectionFieldTypeResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(siteActivityService.getFieldTypes()))
    }

    /**
     * 현장점검 임시저장 조회. 없으면 data=null.
     * (literal "/draft" 이 "/{inspectionId}" 보다 우선 매칭된다.)
     */
    @GetMapping("/draft")
    fun getDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<SiteActivityDraftResponse?>> {
        return ResponseEntity.ok(ApiResponse.success(siteActivityDraftService.getDraft(principal.userId)))
    }

    /**
     * 현장점검 임시저장 (upsert). 검증 없이 사원 1건의 draft 를 갱신한다.
     * 레거시 FieldTalkController.tempFieldChkProc 대응.
     */
    @PostMapping("/draft", consumes = ["multipart/form-data"])
    fun saveDraft(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestPart("request") request: SiteActivityDraftRequest,
        @RequestPart(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<SiteActivityDraftResponse>> {
        val result = siteActivityDraftService.saveDraft(principal.userId, request, photos)
        return ResponseEntity.ok(ApiResponse.success(result, "임시저장되었습니다"))
    }

    /** 현장점검 임시저장 폐기. */
    @DeleteMapping("/draft")
    fun deleteDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Unit>> {
        siteActivityDraftService.deleteDraft(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "임시저장이 삭제되었습니다"))
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
