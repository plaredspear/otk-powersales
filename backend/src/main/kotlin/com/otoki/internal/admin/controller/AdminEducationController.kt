package com.otoki.internal.admin.controller

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.education.dto.response.AdminEducationListResponse
import com.otoki.internal.education.dto.response.EducationCategoryResponse
import com.otoki.internal.education.dto.response.EducationMutationResponse
import com.otoki.internal.education.dto.response.EducationPostDetailResponse
import com.otoki.internal.education.service.EducationService
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
    fun getPostDetail(
        @PathVariable postId: String
    ): ResponseEntity<ApiResponse<EducationPostDetailResponse>> {
        val response = educationService.getPostDetail(postId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/posts")
    fun createPost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam title: String,
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam(required = false) files: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<EducationMutationResponse>> {
        val response = educationService.createPost(principal.userId, title, content, category, files)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/posts/{postId}")
    fun updatePost(
        @PathVariable postId: String,
        @RequestParam title: String,
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam(required = false) files: List<MultipartFile>?,
        @RequestParam(required = false) keepFileKeys: List<String>?
    ): ResponseEntity<ApiResponse<EducationMutationResponse>> {
        val response = educationService.updatePost(postId, title, content, category, files, keepFileKeys)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/posts/{postId}")
    fun deletePost(
        @PathVariable postId: String
    ): ResponseEntity<ApiResponse<Any?>> {
        educationService.deletePost(postId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "교육 자료가 삭제되었습니다"))
    }

    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<ApiResponse<List<EducationCategoryResponse>>> {
        val response = educationService.getCategories()
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
