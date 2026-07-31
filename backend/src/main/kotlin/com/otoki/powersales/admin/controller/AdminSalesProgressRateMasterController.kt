package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterDetailResponse
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterListResponse
import com.otoki.powersales.domain.sales.service.AdminSalesProgressRateMasterService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 거래처목표등록마스터(SF `SalesProgressRateMaster__c`) admin 조회 API (읽기 전용).
 *
 * SF ListView "모두" 동등 목록 + 행 클릭 상세. 데이터 권위는 SF — 등록/수정/삭제 없음.
 * 권한 자원 = entity table name `sales_progress_rate_master` (EntitySfNameRegistry 자동 등록).
 */
@RestController
@RequestMapping("/api/v1/admin/sales-progress-rate-masters")
@Validated
class AdminSalesProgressRateMasterController(
    private val service: AdminSalesProgressRateMasterService,
    private val branchScopeGateway: BranchScopeGateway,
) {

    /**
     * 거래처목표등록마스터 화면 지점 셀렉터 옵션 — 거래처 조회와 동일하게
     * [BranchScopeGateway] + [BranchScopeProfile.ORG_WIDE] 로 권한별 지점 화이트리스트를 산출한다 (단일 출처).
     *
     * 가드는 사용처 도메인 권한(sales_progress_rate_master READ) — account READ 미보유 역할이
     * 거래처 도메인의 `/accounts/branches` 를 빌려쓸 때 발생하는 403 을 회피한다.
     * 이 목록이 곧 [getList] 의 지점 판정 화이트리스트다 — 셀렉터에 보이는 지점(상위 조직 계정이면
     * 하위 지점들)은 그대로 조회되고, 밖의 지점을 요청하면 0건이다(IDOR 차단).
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "sales_progress_rate_master", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = branchScopeGateway.resolveBranches(principal, BranchScopeProfile.ORG_WIDE)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping
    @RequiresSfPermission(entity = "sales_progress_rate_master", operation = SfPermissionOperation.READ)
    fun getList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) targetYear: String?,
        @RequestParam(required = false) targetMonth: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<ApiResponse<SalesProgressRateMasterListResponse>> {
        // 지점 축은 셀렉터([getBranches]) 와 같은 출처 — 판정 후 `BranchMapping` 확장까지 게이트웨이가 끝낸다.
        val response = service.getList(
            branchScopeGateway.applyDataScope(principal, scope),
            keyword, targetYear, targetMonth,
            branchScopeGateway.resolveScope(principal, branchCode, BranchScopeProfile.ORG_WIDE).queryCodesOrNull(),
            page, size,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "sales_progress_rate_master", operation = SfPermissionOperation.READ)
    fun getDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<SalesProgressRateMasterDetailResponse>> {
        val response = service.getDetail(scope, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
