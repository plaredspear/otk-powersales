package com.otoki.internal.notice.controller

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.notice.dto.response.NoticePostDetailResponse
import com.otoki.internal.notice.dto.response.NoticePostListResponse
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.notice.exception.InvalidNoticeIdException
import com.otoki.internal.notice.service.NoticeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/notices")
class NoticeController(
    private val noticeService: NoticeService
) {

    @GetMapping
    fun getPosts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<NoticePostListResponse>> {
        val response = noticeService.getPosts(principal.userId, category, search, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{noticeId}")
    fun getNoticeDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<NoticePostDetailResponse>> {
        if (noticeId <= 0) {
            throw InvalidNoticeIdException()
        }
        val response = noticeService.getNoticeDetail(noticeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
