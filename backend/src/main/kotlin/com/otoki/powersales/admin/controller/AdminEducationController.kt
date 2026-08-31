package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.support.education.dto.response.AdminEducationListResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationCategoryResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationMutationResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationPostDetailResponse
import com.otoki.powersales.domain.support.education.service.EducationService
import com.otoki.powersales.platform.common.storage.InlineImageUploadResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/admin/education")
class AdminEducationController(
    private val educationService: EducationService
) {

    @GetMapping("/posts")
    @RequiresSfPermission(entity = "education_post", operation = SfPermissionOperation.READ)
    fun getPosts(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int
    ): ResponseEntity<ApiResponse<AdminEducationListResponse>> {
        val response = educationService.getPostsForAdmin(category, search, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/posts/{postId}")
    @RequiresSfPermission(entity = "education_post", operation = SfPermissionOperation.READ)
    fun getPostDetail(
        @PathVariable postId: String
    ): ResponseEntity<ApiResponse<EducationPostDetailResponse>> {
        val response = educationService.getPostDetail(postId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/posts")
    @RequiresSfPermission(entity = "education_post", operation = SfPermissionOperation.CREATE)
    fun createPost(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam title: String,
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam(required = false) files: List<MultipartFile>?,
        @RequestParam(required = false) sessionUploadedRefids: List<String>?
    ): ResponseEntity<ApiResponse<EducationMutationResponse>> {
        val response = educationService.createPost(
            principal.requireEmployeeId(), title, content, category, files, sessionUploadedRefids,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/posts/{postId}")
    @RequiresSfPermission(entity = "education_post", operation = SfPermissionOperation.EDIT)
    fun updatePost(
        @PathVariable postId: String,
        @RequestParam title: String,
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam(required = false) files: List<MultipartFile>?,
        @RequestParam(required = false) keepFileKeys: List<String>?,
        @RequestParam(required = false) sessionUploadedRefids: List<String>?
    ): ResponseEntity<ApiResponse<EducationMutationResponse>> {
        val response = educationService.updatePost(
            postId, title, content, category, files, keepFileKeys, sessionUploadedRefids,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 교육 본문 인라인 이미지 업로드 (에디터 전용).
     *
     * 응답의 placeholder 는 본문에, previewUrl 은 에디터 미리보기에 쓴다 — presigned 는 만료되므로
     * **본문에 저장하면 안 된다**. 글이 아직 없을 수 있어(신규 작성) 경로에 postId 를 두지 않는다.
     */
    @PostMapping("/images/inline")
    @RequiresSfPermission(entity = "education_post", operation = SfPermissionOperation.CREATE)
    fun uploadInlineImage(
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<ApiResponse<InlineImageUploadResult>> {
        val response = educationService.uploadInlineImage(image)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @DeleteMapping("/posts/{postId}")
    @RequiresSfPermission(entity = "education_post", operation = SfPermissionOperation.DELETE)
    fun deletePost(
        @PathVariable postId: String
    ): ResponseEntity<ApiResponse<Any?>> {
        educationService.deletePost(postId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "교육 자료가 삭제되었습니다"))
    }

    @GetMapping("/categories")
    @RequiresSfPermission(entity = "education_post", operation = SfPermissionOperation.READ)
    fun getCategories(): ResponseEntity<ApiResponse<List<EducationCategoryResponse>>> {
        val response = educationService.getCategories()
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
