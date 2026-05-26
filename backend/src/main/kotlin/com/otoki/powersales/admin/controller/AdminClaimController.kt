package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.claim.dto.response.AdminClaimCreateResponse
import com.otoki.powersales.claim.dto.response.AdminClaimDetailResponse
import com.otoki.powersales.claim.dto.response.AdminClaimListResponse
import com.otoki.powersales.claim.service.AdminClaimCreateService
import com.otoki.powersales.claim.service.AdminClaimResendService
import com.otoki.powersales.claim.service.AdminClaimService
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/claims")
class AdminClaimController(
    private val adminClaimService: AdminClaimService,
    private val adminClaimCreateService: AdminClaimCreateService,
    private val adminClaimResendService: AdminClaimResendService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "claim", operation = SfPermissionOperation.READ)
    fun getClaims(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) storeName: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<AdminClaimListResponse>> {
        val response = adminClaimService.getClaims(startDate, endDate, status, employeeName, storeName, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{claimId}")
    @RequiresSfPermission(entity = "claim", operation = SfPermissionOperation.READ)
    fun getClaimDetail(
        @PathVariable claimId: Long
    ): ResponseEntity<ApiResponse<AdminClaimDetailResponse>> {
        val response = adminClaimService.getClaimDetail(claimId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * Web admin 클레임 등록 — Spec #829.
     *
     * dual-write: backend.claim INSERT 후 SF Apex `IF_REST_MOBILE_ClaimRegist` 호출.
     * SF 실패도 HTTP 200 으로 응답 (`data.status=SEND_FAILED`) — backend.claim 적재 사실 우선.
     */
    @PostMapping(consumes = ["multipart/form-data"])
    @RequiresSfPermission(entity = "claim", operation = SfPermissionOperation.CREATE)
    fun createClaim(
        @Valid @ModelAttribute request: AdminClaimCreateRequest,
        @RequestParam claimPhoto: MultipartFile,
        @RequestParam partPhoto: MultipartFile,
        @RequestParam(required = false) receiptPhoto: MultipartFile?,
    ): ResponseEntity<ApiResponse<AdminClaimCreateResponse>> {
        val response = adminClaimCreateService.createClaim(request, claimPhoto, partPhoto, receiptPhoto)
        val message = when (response.status) {
            "SENT" -> "클레임이 등록되었으며 SF에 전송되었습니다"
            else -> "클레임은 등록되었으나 SF 전송에 실패했습니다. 재전송이 필요합니다"
        }
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response, message))
    }

    /**
     * SF 재전송 — `claim.status == SEND_FAILED` 인 경우만 허용.
     */
    @PostMapping("/{claimId}/sf-resend")
    @RequiresSfPermission(entity = "claim", operation = SfPermissionOperation.CREATE)
    fun resendClaim(
        @PathVariable claimId: Long,
    ): ResponseEntity<ApiResponse<AdminClaimCreateResponse>> {
        val response = adminClaimResendService.resend(claimId)
        val message = when (response.status) {
            "SENT" -> "SF 재전송에 성공했습니다"
            else -> "SF 재전송에 실패했습니다"
        }
        return ResponseEntity.ok(ApiResponse.success(response, message))
    }
}
