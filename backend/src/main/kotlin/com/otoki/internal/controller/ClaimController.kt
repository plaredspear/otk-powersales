/*
package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.ClaimCreateRequest
import com.otoki.internal.dto.response.ClaimCreateResponse
import com.otoki.internal.dto.response.ClaimFormDataResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ClaimService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/ **
 * 클레임 API Controller
 * /
@RestController
@RequestMapping("/api/v1/claims")
class ClaimController(
    private val claimService: ClaimService
) {

    / **
     * 클레임 등록
     * POST /api/v1/claims
     *
     * @param principal 인증된 사용자
     * @param request 클레임 등록 요청 (multipart form fields)
     * @param defectPhoto 불량 사진 (필수)
     * @param labelPhoto 일부인 사진 (필수)
     * @param receiptPhoto 구매 영수증 사진 (선택)
     * @return 클레임 등록 결과
     * /
    @PostMapping(consumes = ["multipart/form-data"])
    fun createClaim(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @ModelAttribute request: ClaimCreateRequest,
        @RequestParam defectPhoto: MultipartFile,
        @RequestParam labelPhoto: MultipartFile,
        @RequestParam(required = false) receiptPhoto: MultipartFile?
    ): ResponseEntity<ApiResponse<ClaimCreateResponse>> {
        val result = claimService.createClaim(
            userId = principal.userId,
            request = request,
            defectPhoto = defectPhoto,
            labelPhoto = labelPhoto,
            receiptPhoto = receiptPhoto
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "클레임이 등록되었습니다"))
    }

    / **
     * 폼 초기화 데이터 조회
     * GET /api/v1/claims/form-data
     *
     * @param principal 인증된 사용자
     * @return 종류1+종류2, 구매방법, 요청사항 통합 데이터
     * /
    @GetMapping("/form-data")
    fun getFormData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ClaimFormDataResponse>> {
        val result = claimService.getFormData()
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }
}
*/
