package com.otoki.powersales.claim.controller

import com.otoki.powersales.claim.dto.response.ClaimDetailResponse
import com.otoki.powersales.claim.dto.response.ClaimDraftResponse
import com.otoki.powersales.claim.dto.response.ClaimFormDataResponse
import com.otoki.powersales.claim.dto.response.ClaimListItemResponse
import com.otoki.powersales.claim.service.ClaimDraftService
import com.otoki.powersales.claim.service.ClaimQueryService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/mobile/claims")
class ClaimQueryController(
    private val claimQueryService: ClaimQueryService,
    private val claimDraftService: ClaimDraftService
) {

    @GetMapping
    fun getClaims(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<ApiResponse<List<ClaimListItemResponse>>> {
        val result = claimQueryService.getClaims(principal.userId, startDate, endDate)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 클레임 등록 폼 초기화 데이터 조회 (클레임 종류1/2, 구매 방법, 요청사항).
     * GET /api/v1/mobile/claims/form-data
     *
     * literal path 가 `/{claimId}` 패턴보다 우선 매칭된다.
     */
    @GetMapping("/form-data")
    fun getClaimFormData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ClaimFormDataResponse>> {
        return ResponseEntity.ok(ApiResponse.success(claimQueryService.getClaimFormData()))
    }

    /**
     * 클레임 임시저장 조회 (등록 폼 prefill 용). 없으면 data=null.
     * GET /api/v1/mobile/claims/draft — literal path 가 `/{claimId}` 패턴보다 우선 매칭된다.
     */
    @GetMapping("/draft")
    fun getDraft(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ClaimDraftResponse?>> {
        return ResponseEntity.ok(ApiResponse.success(claimDraftService.getDraft(principal.userId)))
    }

    @GetMapping("/{claimId}")
    fun getClaimDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable claimId: Long
    ): ResponseEntity<ApiResponse<ClaimDetailResponse>> {
        val result = claimQueryService.getClaimDetail(principal.userId, claimId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
