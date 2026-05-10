package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.agreement.dto.request.AdminAgreementWordCreateRequest
import com.otoki.powersales.agreement.dto.response.AdminAgreementWordActiveResponse
import com.otoki.powersales.agreement.dto.response.AdminAgreementWordCreateResponse
import com.otoki.powersales.agreement.service.AdminAgreementWordService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
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
    @RequiresPermission(AdminPermission.AGREEMENT_WRITE)
    fun createAgreementWord(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminAgreementWordCreateRequest
    ): ResponseEntity<ApiResponse<AdminAgreementWordCreateResponse>> {
        val response = adminAgreementWordService.createAgreementWord(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "약관 등록 성공"))
    }

    @GetMapping("/active")
    @RequiresPermission(AdminPermission.AGREEMENT_READ)
    fun getActiveAgreementWord(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<AdminAgreementWordActiveResponse?>> {
        val response = adminAgreementWordService.getActiveAgreementWord()
        val message = if (response == null) "활성 약관 없음" else null
        return ResponseEntity.ok(ApiResponse.success(response, message))
    }
}
