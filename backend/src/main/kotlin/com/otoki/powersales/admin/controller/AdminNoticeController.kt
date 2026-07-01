package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.support.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.domain.support.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.domain.support.notice.dto.response.NoticeFormMetaResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticeInlineImageResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticeMutationResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.support.notice.service.NoticeService
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
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.READ)
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
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.READ)
    fun getNoticeFormMeta(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<NoticeFormMetaResponse>> {
        val response = noticeService.getNoticeFormMeta(principal.role)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{noticeId}")
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.READ)
    fun getNoticeDetail(
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<NoticePostDetailResponse>> {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        // admin 상세는 임시저장(DRAFT)도 조회 가능 (발행 전 미리보기/편집).
        val response = noticeService.getNoticeDetail(noticeId, publishedOnly = false)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.CREATE)
    fun createNotice(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: NoticeCreateRequest
    ): ResponseEntity<ApiResponse<NoticeMutationResponse>> {
        val response = noticeService.createNotice(request, principal.requireEmployeeId(), principal.role)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{noticeId}")
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.EDIT)
    fun updateNotice(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable noticeId: Long,
        @Valid @RequestBody request: NoticeUpdateRequest
    ): ResponseEntity<ApiResponse<NoticeMutationResponse>> {
        val response = noticeService.updateNotice(noticeId, request, principal.role)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{noticeId}")
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.DELETE)
    fun deleteNotice(
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        noticeService.deleteNotice(noticeId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "공지사항이 삭제되었습니다"))
    }

    @PatchMapping("/{noticeId}/publish")
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.EDIT)
    fun publishNotice(
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<NoticeMutationResponse>> {
        val response = noticeService.publishNotice(noticeId)
        return ResponseEntity.ok(ApiResponse.success(response, "공지사항이 발행되었습니다"))
    }

    @PatchMapping("/{noticeId}/unpublish")
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.EDIT)
    fun unpublishNotice(
        @PathVariable noticeId: Long
    ): ResponseEntity<ApiResponse<NoticeMutationResponse>> {
        val response = noticeService.unpublishNotice(noticeId)
        return ResponseEntity.ok(ApiResponse.success(response, "공지사항 발행이 취소되었습니다"))
    }

    @PostMapping("/images/inline", consumes = ["multipart/form-data"])
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.CREATE)
    fun uploadNoticeInlineImage(
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<ApiResponse<NoticeInlineImageResponse>> {
        val response = noticeService.uploadNoticeInlineImage(image)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PostMapping("/{noticeId}/images", consumes = ["multipart/form-data"])
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.EDIT)
    fun uploadNoticeImage(
        @PathVariable noticeId: Long,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<ApiResponse<NoticeImageResponse>> {
        val response = noticeService.uploadNoticeImage(noticeId, image)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @DeleteMapping("/{noticeId}/images/{imageId}")
    @RequiresSfPermission(entity = "notice", operation = SfPermissionOperation.EDIT)
    fun deleteNoticeImage(
        @PathVariable noticeId: Long,
        @PathVariable imageId: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        noticeService.deleteNoticeImage(noticeId, imageId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "첨부 이미지가 삭제되었습니다"))
    }
}
