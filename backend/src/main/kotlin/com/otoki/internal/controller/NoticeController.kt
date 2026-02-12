package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.NoticePostDetailResponse
import com.otoki.internal.dto.response.NoticePostListResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.NoticeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 공지사항 API Controller
 */
@RestController
@RequestMapping("/api/v1/notices")
class NoticeController(
    private val noticeService: NoticeService
) {

    /**
     * 공지사항 게시물 목록 조회
     * GET /api/v1/notices?category=COMPANY&search=포장지&page=1&size=10
     *
     * @param category 카테고리 (선택, null이면 전체)
     * @param search 검색 키워드 (선택)
     * @param page 페이지 번호 (선택, 기본 1)
     * @param size 페이지 크기 (선택, 기본 10)
     */
    @GetMapping
    fun getPosts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<NoticePostListResponse>> {
        val response = noticeService.getPosts(category, search, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 공지사항 게시물 상세 조회
     * GET /api/v1/notices/{noticeId}
     *
     * @param noticeId 게시물 ID
     */
    @GetMapping("/{noticeId}")
    fun getPostDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<NoticePostDetailResponse>> {
        val response = noticeService.getPostDetail(noticeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
