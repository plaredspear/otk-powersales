package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
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
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
) {

    /**
     * 거래처목표등록마스터 화면 지점 셀렉터 옵션 — 거래처/여사원 일정과 동일하게
     * [WomenScheduleBranchResolver] 로 권한별 지점 화이트리스트를 산출한다 (단일 출처).
     *
     * 가드는 사용처 도메인 권한(sales_progress_rate_master READ) — account READ 미보유 역할이
     * 거래처 도메인의 `/accounts/branches` 를 빌려쓸 때 발생하는 403 을 회피한다.
     * 목록은 곧 해당 사용자가 조회 허용된 지점이며, [getList] 의 branchCode 필터는
     * 가시 범위 predicate 와 AND 합성되어 권한 외 지점 요청 시 자연히 0건 반환된다 (IDOR 자연 차단).
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "sales_progress_rate_master", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = womenScheduleBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping
    @RequiresSfPermission(entity = "sales_progress_rate_master", operation = SfPermissionOperation.READ)
    fun getList(
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) targetYear: String?,
        @RequestParam(required = false) targetMonth: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<ApiResponse<SalesProgressRateMasterListResponse>> {
        val response = service.getList(scope, keyword, targetYear, targetMonth, branchCode, page, size)
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
