package com.otoki.powersales.domain.activity.claim.controller

import com.otoki.powersales.domain.activity.claim.dto.response.ClaimDetailResponse
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimFormResponse
import com.otoki.powersales.domain.activity.claim.dto.response.ClaimListItemResponse
import com.otoki.powersales.domain.activity.claim.service.ClaimDraftService
import com.otoki.powersales.domain.activity.claim.service.ClaimQueryService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
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
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) accountId: Long?
    ): ResponseEntity<ApiResponse<List<ClaimListItemResponse>>> {
        val result = claimQueryService.getClaims(principal.userId, startDate, endDate, accountId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 클레임 등록 화면 진입 폼 조회. 폼 메타데이터(종류1/2·구매방법·요청사항)와
     * 이어쓰기용 임시저장(없으면 null)을 한 번에 내려준다.
     * GET /api/v1/mobile/claims/form — literal path 가 `/{claimId}` 패턴보다 우선 매칭된다.
     *
     * 일매출 마감 폼([com.otoki.powersales.promotion.controller.MobileDailySalesController.getForm])
     * 의 "진입 1콜 + draft 동봉" 컨벤션과 정합.
     */
    @GetMapping("/form")
    fun getClaimForm(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<ClaimFormResponse>> {
        val response = ClaimFormResponse(
            metadata = claimQueryService.getClaimFormData(),
            draft = claimDraftService.getDraft(principal.userId)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
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
