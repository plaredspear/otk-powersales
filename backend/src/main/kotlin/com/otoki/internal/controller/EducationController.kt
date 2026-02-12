package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.EducationPostDetailResponse
import com.otoki.internal.dto.response.EducationPostListResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.EducationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 교육 API Controller
 */
@RestController
@RequestMapping("/api/v1/education")
class EducationController(
    private val educationService: EducationService
) {

    /**
     * 교육 게시물 목록 조회
     * GET /api/v1/education/posts?category=TASTING_MANUAL&search=시식&page=1&size=10
     *
     * @param category 카테고리 (필수)
     * @param search 검색 키워드 (선택)
     * @param page 페이지 번호 (선택, 기본 1)
     * @param size 페이지 크기 (선택, 기본 10)
     */
    @GetMapping("/posts")
    fun getPosts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = true) category: String,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<EducationPostListResponse>> {
        val response = educationService.getPosts(category, search, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 교육 게시물 상세 조회
     * GET /api/v1/education/posts/{postId}
     *
     * @param postId 게시물 ID
     */
    @GetMapping("/posts/{postId}")
    fun getPostDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable postId: Long
    ): ResponseEntity<ApiResponse<EducationPostDetailResponse>> {
        val response = educationService.getPostDetail(postId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
