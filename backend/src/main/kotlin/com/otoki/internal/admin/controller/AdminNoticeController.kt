package com.otoki.internal.admin.controller

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.notice.dto.request.NoticeCreateRequest
import com.otoki.internal.notice.dto.request.NoticeUpdateRequest
import com.otoki.internal.notice.dto.response.NoticeMutationResponse
import com.otoki.internal.notice.dto.response.NoticePostDetailResponse
import com.otoki.internal.notice.dto.response.NoticePostListResponse
import com.otoki.internal.notice.exception.InvalidNoticeIdException
import com.otoki.internal.notice.service.NoticeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/notices")
class AdminNoticeController(
    private val noticeService: NoticeService
) {

    @GetMapping
    fun getPosts(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<NoticePostListResponse>> {
        val response = noticeService.getPostsForAdmin(category, search, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{noticeId}")
    fun getNoticeDetail(
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<NoticePostDetailResponse>> {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val response = noticeService.getNoticeDetail(noticeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    fun createNotice(
        @Valid @RequestBody request: NoticeCreateRequest
    ): ResponseEntity<ApiResponse<NoticeMutationResponse>> {
        val response = noticeService.createNotice(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{noticeId}")
    fun updateNotice(
        @PathVariable noticeId: Long,
        @Valid @RequestBody request: NoticeUpdateRequest
    ): ResponseEntity<ApiResponse<NoticeMutationResponse>> {
        val response = noticeService.updateNotice(noticeId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{noticeId}")
    fun deleteNotice(
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        noticeService.deleteNotice(noticeId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "공지사항이 삭제되었습니다"))
    }
}
