package com.otoki.powersales.admin.controller

import com.otoki.powersales.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.account.dto.response.AccountListResponse
import com.otoki.powersales.account.dto.response.AdminAccountCreateResponse
import com.otoki.powersales.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.account.service.AccountCreateService
import com.otoki.powersales.account.service.AccountDeleteService
import com.otoki.powersales.account.service.AccountUpdateService
import com.otoki.powersales.account.service.AdminAccountService
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/accounts")
@Validated
class AdminAccountController(
    private val adminAccountService: AdminAccountService,
    private val accountCreateService: AccountCreateService,
    private val accountUpdateService: AccountUpdateService,
    private val accountDeleteService: AccountDeleteService
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

    @PostMapping
    @RequiresPermission(AdminPermission.ACCOUNT_WRITE)
    fun createAccount(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminAccountCreateRequest
    ): ResponseEntity<ApiResponse<AdminAccountCreateResponse>> {
        val response = accountCreateService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "거래처 등록 성공"))
    }

    @PutMapping("/{id}")
    @RequiresPermission(AdminPermission.ACCOUNT_WRITE)
    fun updateAccount(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Int,
        @Valid @RequestBody request: AdminAccountUpdateRequest
    ): ResponseEntity<ApiResponse<AdminAccountUpdateResponse>> {
        val response = accountUpdateService.update(id, principal, request)
        return ResponseEntity.ok(ApiResponse.success(response, "거래처 수정 성공"))
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(AdminPermission.ACCOUNT_DELETE)
    fun deleteAccount(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Int
    ): ResponseEntity<ApiResponse<Any?>> {
        accountDeleteService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "거래처 삭제 성공"))
    }
}
