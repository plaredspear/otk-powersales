package com.otoki.powersales.domain.activity.suggestion.controller

import com.otoki.powersales.domain.activity.suggestion.dto.request.AdminLogisticsClaimRegistTestRequest
import com.otoki.powersales.domain.activity.suggestion.dto.response.AdminLogisticsClaimRegistTestResponse
import com.otoki.powersales.domain.activity.suggestion.service.AdminLogisticsClaimRegistTestService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * SF 물류 클레임 등록(ProposalRegist) 전송 테스트 API (개발자 도구 — 외부 API 테스트).
 *
 * 모바일 물류 클레임 등록 정보로 SF Apex REST `IF_REST_MOBILE_ProposalRegist` 전송 payload(apiMap)
 * 미리보기를 구성한다. SF 전송 API 정보 미확보 단계라 실제 SF POST 는 수행하지 않는다.
 * claim-regist 테스트와 동일하게 SYSTEM(`MODIFY_ALL_DATA`) 권한 필요.
 */
@RestController
class AdminLogisticsClaimRegistTestController(
    private val service: AdminLogisticsClaimRegistTestService,
) {

    @PostMapping("/api/v1/admin/logistics-claim-regist/test", consumes = ["multipart/form-data"])
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun test(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @ModelAttribute request: AdminLogisticsClaimRegistTestRequest,
        @RequestParam(required = false) photo1: MultipartFile?,
        @RequestParam(required = false) photo2: MultipartFile?,
    ): ResponseEntity<ApiResponse<AdminLogisticsClaimRegistTestResponse>> {
        val response = service.test(
            userId = principal.requireEmployeeId(),
            request = request,
            photo1 = photo1,
            photo2 = photo2,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
