package com.otoki.powersales.suggestion.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.suggestion.dto.request.SuggestionCreateRequest
import com.otoki.powersales.suggestion.dto.request.SuggestionUpdateRequest
import com.otoki.powersales.suggestion.dto.response.SuggestionCreateResponse
import com.otoki.powersales.suggestion.dto.response.SuggestionListItem
import com.otoki.powersales.suggestion.dto.response.SuggestionResponse
import com.otoki.powersales.suggestion.service.SuggestionService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 제안하기 API Controller — Spec #664 P2-B.
 *
 * ## 레거시 매핑
 * - SF Apex: `IF_REST_MOBILE_ProposalRegist.cls#doPost`, `IF_REST_MOBILE_LogisticsClaimSearch.cls#doPost` (조회는 #667 별 스펙)
 */
@RestController
@RequestMapping("/api/v1/mobile/suggestions")
class SuggestionController(
    private val suggestionService: SuggestionService
) {

    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestPart("request") request: SuggestionCreateRequest,
        @RequestPart(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<SuggestionCreateResponse>> {
        val result = suggestionService.create(
            employeeId = principal.userId,
            request = request,
            photos = photos
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "제안이 등록되었습니다"))
    }

    @GetMapping("/{suggestionId}")
    fun getDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long
    ): ResponseEntity<ApiResponse<SuggestionResponse>> {
        val response = suggestionService.getDetail(suggestionId, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping
    fun listMine(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<SuggestionListItem>>> {
        val response = suggestionService.listMine(principal.userId, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{suggestionId}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long,
        @Valid @RequestBody request: SuggestionUpdateRequest
    ): ResponseEntity<ApiResponse<SuggestionResponse>> {
        val response = suggestionService.update(suggestionId, principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "제안이 수정되었습니다"))
    }

    @DeleteMapping("/{suggestionId}")
    fun softDelete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        suggestionService.softDelete(suggestionId, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "제안이 삭제되었습니다"))
    }

    /**
     * UC-06: 제안 첨부 사진 단건 삭제 (Spec #828). 본인 row 한정, 상태 무관 허용.
     */
    @DeleteMapping("/{suggestionId}/photos/{photoId}")
    fun deletePhoto(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable suggestionId: Long,
        @PathVariable photoId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        suggestionService.deletePhoto(principal.userId, suggestionId, photoId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "사진이 삭제되었습니다"))
    }
}
