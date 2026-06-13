package com.otoki.powersales.domain.activity.claim.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.activity.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.domain.activity.claim.dto.response.AdminClaimRegistTestResponse
import com.otoki.powersales.domain.activity.claim.service.AdminClaimRegistTestService
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * SF ClaimRegist 전송 테스트 API (개발자 도구 — 외부 API 테스트).
 *
 * 운영 클레임 등록과 달리 신규 DB 저장 없이 SF Apex REST `/ClaimRegist` 로만 전송한다.
 * 호출 즉시 실제 SF 로 요청이 나가며 SF 에 클레임 row 가 INSERT 된다 (테스트 잡데이터 주의).
 * naver-geocode 테스트와 동일하게 SYSTEM(`MODIFY_ALL_DATA`) 권한 필요.
 */
@RestController
class AdminClaimRegistTestController(
    private val adminClaimRegistTestService: AdminClaimRegistTestService,
) {

    @PostMapping("/api/v1/admin/claim-regist/test", consumes = ["multipart/form-data"])
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun test(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @ModelAttribute request: AdminClaimCreateRequest,
        @RequestParam(required = false) claimPhoto: MultipartFile?,
        @RequestParam(required = false) partPhoto: MultipartFile?,
        @RequestParam(required = false) receiptPhoto: MultipartFile?,
    ): ResponseEntity<ApiResponse<AdminClaimRegistTestResponse>> {
        val response = adminClaimRegistTestService.test(
            userId = principal.requireEmployeeId(),
            request = request,
            claimPhoto = claimPhoto,
            partPhoto = partPhoto,
            receiptPhoto = receiptPhoto,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
