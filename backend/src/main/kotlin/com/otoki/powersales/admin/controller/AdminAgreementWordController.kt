package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.support.agreement.dto.request.AdminAgreementWordCreateRequest
import com.otoki.powersales.domain.support.agreement.dto.response.AdminAgreementWordActiveResponse
import com.otoki.powersales.domain.support.agreement.dto.response.AdminAgreementWordCreateResponse
import com.otoki.powersales.domain.support.agreement.service.AdminAgreementWordService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/agreement-words")
@Validated
class AdminAgreementWordController(
    private val adminAgreementWordService: AdminAgreementWordService
) {

    @PostMapping
    @RequiresSfPermission(entity = "agreement_word", operation = SfPermissionOperation.EDIT)
    fun createAgreementWord(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminAgreementWordCreateRequest
    ): ResponseEntity<ApiResponse<AdminAgreementWordCreateResponse>> {
        val response = adminAgreementWordService.createAgreementWord(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "약관 등록 성공"))
    }

    @GetMapping("/active")
    @RequiresSfPermission(entity = "agreement_word", operation = SfPermissionOperation.READ)
    fun getActiveAgreementWord(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<AdminAgreementWordActiveResponse?>> {
        val response = adminAgreementWordService.getActiveAgreementWord()
        val message = if (response == null) "활성 약관 없음" else null
        return ResponseEntity.ok(ApiResponse.success(response, message))
    }
}
