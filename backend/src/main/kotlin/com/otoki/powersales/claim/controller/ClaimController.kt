package com.otoki.powersales.claim.controller

import com.otoki.powersales.claim.dto.request.ClaimCreateRequest
import com.otoki.powersales.claim.dto.request.ClaimDraftRequest
import com.otoki.powersales.claim.dto.response.ClaimCreateResponse
import com.otoki.powersales.claim.dto.response.ClaimDraftResponse
import com.otoki.powersales.claim.service.ClaimDraftService
import com.otoki.powersales.claim.service.ClaimService
import com.otoki.powersales.claim.service.ClaimUpdateRequest
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 클레임 등록/수정/삭제 API (모바일).
 * 조회는 [ClaimQueryController] 에 분리.
 */
@RestController
@RequestMapping("/api/v1/mobile/claims")
class ClaimController(
    private val claimService: ClaimService,
    private val claimDraftService: ClaimDraftService
) {

    /**
     * UC-02 / UC-10: 클레임 신규 등록.
     */
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

    /**
     * 클레임 임시저장 (upsert). 검증 없이 사원 1건의 draft 를 갱신한다.
     * 레거시 FieldTalkController.tempClaimProc 대응.
     */
    @PostMapping("/draft", consumes = ["multipart/form-data"])
    fun saveDraft(
        @AuthenticationPrincipal principal: UserPrincipal,
        @ModelAttribute request: ClaimDraftRequest,
        @RequestParam(required = false) defectPhoto: MultipartFile?,
        @RequestParam(required = false) labelPhoto: MultipartFile?,
        @RequestParam(required = false) receiptPhoto: MultipartFile?
    ): ResponseEntity<ApiResponse<ClaimDraftResponse>> {
        val result = claimDraftService.saveDraft(
            userId = principal.userId,
            request = request,
            defectPhoto = defectPhoto,
            labelPhoto = labelPhoto,
            receiptPhoto = receiptPhoto
        )
        return ResponseEntity.ok(ApiResponse.success(result, "임시저장되었습니다"))
    }

    /** 클레임 임시저장 폐기. */
    @DeleteMapping("/draft")
    fun deleteDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Unit>> {
        claimDraftService.deleteDraft(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "임시저장이 삭제되었습니다"))
    }

    /**
     * UC-03: 클레임 수정. 상태가 DRAFT 일 때만 허용.
     */
    @PutMapping("/{claimId}")
    fun updateClaim(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable claimId: Long,
        @RequestBody request: ClaimUpdateRequest
    ): ResponseEntity<ApiResponse<ClaimCreateResponse>> {
        val result = claimService.updateClaim(principal.userId, claimId, request)
        return ResponseEntity.ok(ApiResponse.success(result, "클레임이 수정되었습니다"))
    }

    /**
     * UC-11: 클레임 삭제. 상태가 DRAFT 일 때만 허용. 첨부 사진 일괄 삭제.
     */
    @DeleteMapping("/{claimId}")
    fun deleteClaim(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable claimId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        claimService.deleteClaim(principal.userId, claimId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "클레임이 삭제되었습니다"))
    }

    /**
     * UC-06: 클레임 사진 삭제. 상태가 DRAFT 일 때만 허용.
     */
    @DeleteMapping("/{claimId}/photos/{photoId}")
    fun deleteClaimPhoto(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable claimId: Long,
        @PathVariable photoId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        claimService.deletePhoto(principal.userId, claimId, photoId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "사진이 삭제되었습니다"))
    }
}
