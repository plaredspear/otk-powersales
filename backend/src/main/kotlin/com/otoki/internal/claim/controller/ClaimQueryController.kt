package com.otoki.internal.claim.controller

import com.otoki.internal.claim.dto.response.ClaimDetailResponse
import com.otoki.internal.claim.dto.response.ClaimListItemResponse
import com.otoki.internal.claim.service.ClaimQueryService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/claims")
class ClaimQueryController(
    private val claimQueryService: ClaimQueryService
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

    @GetMapping("/{claimId}")
    fun getClaimDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable claimId: Long
    ): ResponseEntity<ApiResponse<ClaimDetailResponse>> {
        val result = claimQueryService.getClaimDetail(principal.userId, claimId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
