package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.account.dto.response.AccountListResponse
import com.otoki.powersales.account.dto.response.AdminAccountCreateResponse
import com.otoki.powersales.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.account.service.AccountCreateService
import com.otoki.powersales.account.service.AccountDeleteService
import com.otoki.powersales.account.service.AccountUpdateService
import com.otoki.powersales.account.service.AdminAccountService
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
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
    private val accountDeleteService: AccountDeleteService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.READ)
    fun getAccounts(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false) abcType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) accountStatusName: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = adminAccountService.getAccounts(
            scope = scope,
            keyword = keyword,
            abcType = abcType,
            branchCode = branchCode,
            accountStatusName = accountStatusName,
            page = page,
            size = size,
            // SF 메인 거래처 탭 listView(AllAccounts)=Everything — lookupFilter 미적용 (lookup 진입점에만 적용).
            applyPromotionFilter = false
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 행사마스터 등록/수정 화면의 거래처 lookup search — SF AccId__c lookupFilter + sharing rule 동등.
     *
     * SF 의 lookup search 는 Account FLS/object access 와 무관하게 화면 권한 (Promotion CRUD) 으로
     * 작동 — 본 endpoint 는 SF 메커니즘 정합. 결과는 동일 [AccountListResponse] 재사용
     * (lookupFilter + sharing rule 평가는 `adminAccountService.getAccounts` 가 그대로 적용).
     */
    @GetMapping("/lookup")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun lookupAccounts(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = adminAccountService.getAccounts(
            scope = scope,
            keyword = keyword,
            abcType = null,
            branchCode = null,
            accountStatusName = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 물류 클레임 등록/수정 화면의 거래처 lookup search — SF Claim__c.AccId__c Lookup 정합.
     *
     * SF 의 lookup search 는 Account FLS/object access 와 무관하게 화면 권한 (Suggestion/Claim CRUD)
     * 으로 작동 — 본 endpoint 는 SF 메커니즘 정합. 결과는 동일 [AccountListResponse] 재사용
     * (lookupFilter + sharing rule 평가는 `adminAccountService.getAccounts` 가 그대로 적용).
     */
    @GetMapping("/lookup-for-claim")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.READ)
    fun lookupAccountsForClaim(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = adminAccountService.getAccounts(
            scope = scope,
            keyword = keyword,
            abcType = null,
            branchCode = null,
            accountStatusName = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 유통기한 관리 / 재고조회 화면의 거래처 lookup search — Heroku 단독 / 신규 기능 (SF 매핑 없음).
     *
     * product.READ 권한 보유자가 유통기한 등록 또는 재고조회 시 거래처 검색. account.READ 권한 없이
     * 호출 가능.
     */
    @GetMapping("/lookup-for-product")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun lookupAccountsForProduct(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = adminAccountService.getAccounts(
            scope = scope,
            keyword = keyword,
            abcType = null,
            branchCode = null,
            accountStatusName = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.EDIT)
    fun createAccount(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminAccountCreateRequest
    ): ResponseEntity<ApiResponse<AdminAccountCreateResponse>> {
        val response = accountCreateService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "거래처 등록 성공"))
    }

    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.EDIT)
    fun updateAccount(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Int,
        @Valid @RequestBody request: AdminAccountUpdateRequest
    ): ResponseEntity<ApiResponse<AdminAccountUpdateResponse>> {
        val response = accountUpdateService.update(id, principal, request)
        return ResponseEntity.ok(ApiResponse.success(response, "거래처 수정 성공"))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.DELETE)
    fun deleteAccount(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Int
    ): ResponseEntity<ApiResponse<Any?>> {
        accountDeleteService.delete(id, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "거래처 삭제 성공"))
    }
}
