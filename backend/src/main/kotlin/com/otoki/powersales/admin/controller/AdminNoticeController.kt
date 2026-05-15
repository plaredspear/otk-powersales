package com.otoki.powersales.admin.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.notice.dto.response.NoticeFormMetaResponse
import com.otoki.powersales.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.notice.dto.response.NoticeMutationResponse
import com.otoki.powersales.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.notice.service.NoticeService
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

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

    @GetMapping("/form-meta")
    fun getNoticeFormMeta(): ResponseEntity<ApiResponse<NoticeFormMetaResponse>> {
        val response = noticeService.getNoticeFormMeta()
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
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: NoticeCreateRequest
    ): ResponseEntity<ApiResponse<NoticeMutationResponse>> {
        val response = noticeService.createNotice(request, principal.requireEmployeeId())
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

    @PostMapping("/{noticeId}/images", consumes = ["multipart/form-data"])
    fun uploadNoticeImage(
        @PathVariable noticeId: Long,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<ApiResponse<NoticeImageResponse>> {
        val response = noticeService.uploadNoticeImage(noticeId, image)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @DeleteMapping("/{noticeId}/images/{imageId}")
    fun deleteNoticeImage(
        @PathVariable noticeId: Long,
        @PathVariable imageId: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        noticeService.deleteNoticeImage(noticeId, imageId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "첨부 이미지가 삭제되었습니다"))
    }
}
