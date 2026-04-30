package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.account.dto.response.AccountListResponse
import com.otoki.powersales.account.service.AdminAccountService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/accounts")
@Validated
class AdminAccountController(
    private val adminAccountService: AdminAccountService
) {

    @GetMapping
    @RequiresPermission(AdminPermission.ACCOUNT_READ)
    fun getAccounts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false) abcType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) accountStatusName: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = adminAccountService.getAccounts(
            keyword = keyword,
            abcType = abcType,
            branchCode = branchCode,
            accountStatusName = accountStatusName,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
