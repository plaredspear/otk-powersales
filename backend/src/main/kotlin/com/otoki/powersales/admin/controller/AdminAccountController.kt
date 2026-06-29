package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.domain.foundation.account.dto.response.AccountDetailResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountCreateResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.domain.foundation.account.service.AccountCreateService
import com.otoki.powersales.domain.foundation.account.service.AccountDeleteService
import com.otoki.powersales.domain.foundation.account.service.AccountUpdateService
import com.otoki.powersales.domain.foundation.account.service.AdminAccountService
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
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
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
) {

    /**
     * 거래처 화면 지점 셀렉터 옵션 — 여사원 일정/전문행사조와 동일하게
     * [WomenScheduleBranchResolver] 로 권한별 지점 화이트리스트를 산출한다 (단일 출처).
     *
     * 목록은 곧 해당 사용자가 조회 허용된 지점이며, [getAccounts] 의 branchCode 필터는
     * DataScope(sharing policy) 와 AND 합성되어 권한 외 지점 요청 시 자연히 0건 반환된다(IDOR 자연 차단).
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = womenScheduleBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

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
     *
     * 단, 폐업 거래처는 `excludeClosedAccount=true` 로 distribution 면제 없이 완전 제외한다 —
     * 폐업 거래처는 행사 등록 대상이 아니므로 조회 후보에서도 일관되게 배제 (운영 정책).
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
            size = size,
            excludeClosedAccount = true,
            // SF "내 지점 거래처"(myBranchAccount listView) 동등 — owner 가 아닌 조직코드 매칭으로 가시성
            // 평가. sharing policy(owner.user_role_id 계층) 로 평가하면 본인 지점 거래처가 owner 불일치로
            // 전부 누락된다(조장이 본인 지점 이마트 등 검색 시 0건). 행사마스터 등록은 본인 지점 거래처를
            // owner 무관하게 선택할 수 있어야 한다.
            applyMyBranchScope = true
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 진열사원스케줄 마스터 등록/수정 화면의 거래처 lookup search.
     *
     * 행사마스터 lookup(`/lookup`)과 동일한 accountGroup ∈ {1000,1010} 필터를 적용하되, 폐업 거래처는
     * `excludeClosedAccount=true` 로 distribution 면제 없이 완전 제외한다 — 폐업 거래처는 진열사원스케줄
     * 등록 검증(`ScheduleUploadValidator`)에서 차단되므로 조회 후보에서도 일관되게 제외하기 위함이다.
     * display_work_schedule.READ 권한으로 가드 (Account READ 권한 불요 — SF lookup search 메커니즘 정합).
     */
    @GetMapping("/lookup-for-display-schedule")
    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    fun lookupAccountsForDisplaySchedule(
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
            size = size,
            excludeClosedAccount = true
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

    /**
     * POS매출 조회 화면의 거래처 lookup search — monthly_sales_history 권한 보유자 호출용.
     *
     * POS매출 화면은 monthly_sales_history.READ 로 진입하므로, account.READ / product.READ 권한
     * 없이도 거래처를 검색할 수 있도록 monthly_sales_history.READ 가드로 분리한다.
     * ([lookupAccountsForProduct] 를 빌려 쓰면 product.READ 미보유자에게 403 이 발생하던 사례 해소.)
     */
    @GetMapping("/lookup-for-pos-sales")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun lookupAccountsForPosSales(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = adminAccountService.getAccounts(
            scope = scope,
            keyword = keyword,
            abcType = null,
            branchCode = branchCode,
            accountStatusName = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 거래처 상세 조회 — 거래처 상세 페이지(`/account/:id`) 의 "기본 정보" 영역.
     *
     * 목록(`getAccounts`)과 동일한 `account.READ` + SF Sharing Rule 정책 적용. 가시 범위 밖 거래처는
     * 404 (SF sharing rule 동등). lookup 경로(`/lookup*`)와 path 충돌 없음 (`{id}` 는 Int).
     */
    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.READ)
    fun getAccountDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<AccountDetailResponse>> {
        val response = adminAccountService.getAccountDetail(scope, id)
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
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminAccountUpdateRequest
    ): ResponseEntity<ApiResponse<AdminAccountUpdateResponse>> {
        val response = accountUpdateService.update(id, principal, request)
        return ResponseEntity.ok(ApiResponse.success(response, "거래처 수정 성공"))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.DELETE)
    fun deleteAccount(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        accountDeleteService.delete(id, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "거래처 삭제 성공"))
    }
}
