package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SalesSupportTeam2Policy
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.domain.foundation.account.dto.response.AccountDetailResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AccountLookupFilterOptions
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountCreateResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.domain.foundation.account.service.AccountCreateService
import com.otoki.powersales.domain.foundation.account.service.AccountDeleteService
import com.otoki.powersales.domain.foundation.account.service.AccountUpdateService
import com.otoki.powersales.domain.foundation.account.service.AdminAccountService
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
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
    private val branchScopeGateway: BranchScopeGateway,
) {

    /**
     * 영업지원2팀 거래처 조회 예외 — 지점 축만 전사로 바꾼 principal 사본
     * ([SalesSupportTeam2Policy.isAllBranchAccountLookup], 2026-08-03 요구).
     *
     * 영업지원2팀은 전 지점 거래처를 다루지만 `isSalesSupport = false`(2026-08-02) 라 지점 리졸버가
     * 본인 조직(4889) 1건만 산출한다. 거래처 화면 **진입점에서만** 전사 플래그를 세운 사본을 만들어
     * 기존 전사 경로(셀렉터 = 조직 전건, 판정 = 전건)를 그대로 재사용한다. 사본은 이 요청 처리 안에서만
     * 쓰이고 저장/전파되지 않으므로 다른 화면(대시보드·여사원 현황·행사사원 후보 등) 의 스코프는 그대로다.
     */
    private fun accountScopePrincipal(principal: WebUserPrincipal): WebUserPrincipal =
        if (SalesSupportTeam2Policy.isAllBranchAccountLookup(principal.costCenterCode)) {
            principal.copy(isSalesSupport = true)
        } else {
            principal
        }

    /**
     * 영업지원2팀 거래처 조회 예외의 가시성 축 — sharing policy 가 보는 [DataScope] 를 전 지점으로 교체.
     *
     * 지점 셀렉터/필터([accountScopePrincipal])와 별개 축이다. SF Sharing Rule evaluator 는
     * `isAllBranches = true` 를 "지점 제한 없음" 으로 해석해 우선순위 5(legacy branchCodes) 를 통과시킨다
     * — Account 는 OWD Private 이라 이 축을 열지 않으면 owner 불일치로 전부 누락된다.
     * RecordType 가시성(우선순위 7) 은 그대로 AND 로 남는다.
     */
    private fun accountDataScope(principal: WebUserPrincipal, scope: DataScope): DataScope =
        if (SalesSupportTeam2Policy.isAllBranchAccountLookup(principal.costCenterCode)) {
            scope.copy(branchCodes = emptyList(), isAllBranches = true)
        } else {
            scope
        }

    /**
     * 거래처 화면 지점 셀렉터 옵션 — [BranchScopeGateway] + [BranchScopeProfile.ORG_WIDE]
     * (전사 권한자는 조직 전건, 그 외는 본인 costCenterCode 의 조직 트리).
     *
     * 이 목록이 곧 [getAccounts] 의 지점 판정 화이트리스트다 — 셀렉터에 보이는 지점은 그대로 조회되고,
     * 밖의 지점을 요청하면 0건이다(IDOR 차단). 영업지원2팀은 [accountScopePrincipal] 로 전사 목록을 본다.
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = branchScopeGateway.resolveBranches(accountScopePrincipal(principal), BranchScopeProfile.ORG_WIDE)
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
        // "좌표 미수신" 필터 — Naver Geocode batch(#637) 진입 후보와 동일 조건으로 좁혀 조회한다.
        // 스케줄 잡 "거래처 좌표변환" 패널의 링크가 `?coordinatesMissing=true` 로 진입시킨다.
        @RequestParam(required = false, defaultValue = "false") coordinatesMissing: Boolean,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val scopePrincipal = accountScopePrincipal(principal)
        val response = adminAccountService.getAccounts(
            // 지점 축은 셀렉터([getBranches]) 와 같은 출처로 넓힌다 — 비전사 사용자의 DataScope 지점 축은
            // 본인 코드 1건이라, 셀렉터의 하위 지점을 골라도 sharing policy 에서 탈락해 0건이 됐다.
            // 영업지원2팀은 [accountDataScope] 가 이미 전 지점으로 바꿔 두므로 widen 은 no-op 이다.
            scope = branchScopeGateway.applyDataScope(scopePrincipal, accountDataScope(principal, scope)),
            keyword = keyword,
            abcType = abcType,
            branchCodes = branchScopeGateway
                .resolveScope(scopePrincipal, branchCode, BranchScopeProfile.ORG_WIDE)
                .queryCodesOrNull(),
            accountStatusName = accountStatusName,
            page = page,
            size = size,
            // SF 메인 거래처 탭 listView(AllAccounts)=Everything — lookupFilter 미적용 (lookup 진입점에만 적용).
            applyPromotionFilter = false,
            coordinatesMissing = coordinatesMissing
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
        // 고급 검색 부가 필터 — 거래처유형(accountType) / 거래상태(accountStatusName) 정확 일치.
        // 폐업은 excludeClosedAccount 로 원천 배제되므로 accountStatusName=폐업 은 0건.
        @RequestParam(required = false) accountType: String?,
        @RequestParam(required = false) accountStatusName: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = adminAccountService.getAccounts(
            scope = scope,
            keyword = keyword,
            abcType = null,
            branchCodes = null,
            accountStatusName = accountStatusName,
            page = page,
            size = size,
            excludeClosedAccount = true,
            // SF 행사마스터(PPTMaster) 거래처 lookup 정합 — sharing policy(owner.user_role_id 계층) 대신
            // CurrentUserBranchNameList 동등 지점 화이트리스트 → branch_code IN 매칭으로 평가. owner 기준이면
            // 본인 지점 거래처가 owner 불일치로 전부 누락된다(조장이 본인 지점 거래처 검색 시 0건).
            myBranchScopePrincipal = principal,
            accountType = accountType
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 행사마스터 거래처 고급 검색 필터 드롭다운 옵션 — 거래처유형/거래상태 distinct 값.
     *
     * 실제 검색 대상(지점 스코프 + promotionLookupFilter + 폐업 제외) 집합의 값만 반환해
     * 선택지에 노출 불가 값(폐업 등)이 뜨지 않게 한다. lookup 과 동일하게 promotion.READ 로 가드.
     */
    @GetMapping("/lookup-filter-options")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun lookupFilterOptions(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<AccountLookupFilterOptions>> {
        val response = adminAccountService.getPromotionLookupFilterOptions(principal)
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
            branchCodes = null,
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
            branchCodes = null,
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
            branchCodes = null,
            accountStatusName = null,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * ORORA 일매출 화면의 거래처 lookup search — 「기준정보 > ORORA 일매출」 조회 조건 전용.
     *
     * daily_sales_history.READ 권한 보유자가 조회 대상 거래처를 고급 검색으로 고를 때 사용한다.
     * account.READ 권한 없이도 호출 가능 (SF lookup search 메커니즘 정합).
     */
    @GetMapping("/lookup-for-daily-sales")
    @RequiresSfPermission(entity = "daily_sales_history", operation = SfPermissionOperation.READ)
    fun lookupAccountsForDailySales(
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
            branchCodes = null,
            accountStatusName = null,
            page = page,
            size = size,
            // 매출 적재 확인 화면이라 행사 lookupFilter (계정그룹/폐업 제외) 를 적용하지 않는다 —
            // 폐업 거래처의 과거 매출도 조회 대상이다.
            applyPromotionFilter = false
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * ORORA 월매출 화면의 거래처 lookup search — 「기준정보 > ORORA 월매출」 조회 조건 전용.
     *
     * monthly_sales_history.READ 권한 보유자가 조회 대상 거래처를 고급 검색으로 고를 때 사용한다.
     * account.READ 권한 없이도 호출 가능 (SF lookup search 메커니즘 정합).
     *
     * 일매출용(`/lookup-for-daily-sales`) 과 동작은 같지만 가드 entity 가 달라 별도 endpoint 로 둔다 —
     * 화면 게이팅 entity 와 API 가드 entity 가 어긋나면 메뉴는 보이는데 검색만 403 이 된다.
     */
    @GetMapping("/lookup-for-monthly-sales")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun lookupAccountsForMonthlySales(
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
            branchCodes = null,
            accountStatusName = null,
            page = page,
            size = size,
            // 매출 적재 확인 화면이라 행사 lookupFilter (계정그룹/폐업 제외) 를 적용하지 않는다 —
            // 폐업 거래처의 과거 매출도 조회 대상이다 (일매출 lookup 과 동일).
            applyPromotionFilter = false
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 거래처 상세 조회 — 거래처 상세 페이지(`/account/:id`) 의 "기본 정보" 영역.
     *
     * 목록(`getAccounts`)과 동일한 `account.READ` + SF Sharing Rule 정책 적용. 가시 범위 밖 거래처는
     * 404 (SF sharing rule 동등). lookup 경로(`/lookup*`)와 path 충돌 없음 (`{id}` 는 Int).
     *
     * 가시 범위는 목록과 같은 축([accountDataScope])을 쓴다 — 목록에 보이는 행을 클릭했는데 상세가
     * 404 가 되는 불일치를 막는다 (영업지원2팀 전 지점 예외 포함).
     */
    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "account", operation = SfPermissionOperation.READ)
    fun getAccountDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<AccountDetailResponse>> {
        val response = adminAccountService.getAccountDetail(accountDataScope(principal, scope), id)
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
