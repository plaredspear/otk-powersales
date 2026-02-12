package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.SuggestionCreateRequest
import com.otoki.internal.dto.response.SuggestionCreateResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.SuggestionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 제안하기 API Controller
 */
@RestController
@RequestMapping("/api/v1/suggestions")
class SuggestionController(
    private val suggestionService: SuggestionService
) {

    /**
     * 제안하기 등록
     * POST /api/v1/suggestions
     *
     * @param principal 인증된 사용자
     * @param request 제안 등록 요청 (multipart form fields)
     * @param photos 사진 목록 (선택, 최대 2장)
     * @return 제안 등록 결과
     */
    @PostMapping(consumes = ["multipart/form-data"])
    fun createSuggestion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @ModelAttribute request: SuggestionCreateRequest,
        @RequestParam(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<SuggestionCreateResponse>> {
        val result = suggestionService.createSuggestion(
            userId = principal.userId,
            request = request,
            photos = photos
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "제안이 등록되었습니다"))
    }
}
