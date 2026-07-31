package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyInputAdequacyResponse
import com.otoki.powersales.domain.activity.schedule.service.AdminMonthlyInputAdequacyService
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/schedules/monthly-input-adequacy")
class AdminMonthlyInputAdequacyController(
    private val adminMonthlyInputAdequacyService: AdminMonthlyInputAdequacyService,
    private val branchScopeGateway: BranchScopeGateway,
) {

    /** 1~12월 적합성 매트릭스 조회. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping
    fun getMatrix(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) workingCategory3: String?
    ): ResponseEntity<ApiResponse<MonthlyInputAdequacyResponse>> {
        val response = adminMonthlyInputAdequacyService.getMatrix(scopeOf(principal, scope), year, codesOf(principal, costCenterCodes), workingCategory3)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 매트릭스 엑셀 다운로드. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/export")
    fun exportMatrix(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) workingCategory3: String?
    ): ResponseEntity<ByteArray> {
        val result = adminMonthlyInputAdequacyService.exportMatrix(scopeOf(principal, scope), year, codesOf(principal, costCenterCodes), workingCategory3)
        return ExcelResponseUtils.build(result)
    }

    /**
     * 지점 축 보정 — 셀렉터([AdminSalesBranchController]) 와 같은 출처로 조회 범위를 맞춘다.
     *
     * `DataScope` 의 지점 축은 비전사 사용자에게 본인 costCenterCode 1건이라, 셀렉터가 보여준 조직 트리의
     * 하위 지점을 골라도 교집합에서 탈락해 0건이 됐다. [BranchScopeGateway.applyDataScope] 가 그 축을
     * 셀렉터와 같은 조직 트리로 넓힌다(넓히기만 하므로 보이던 행이 사라지지 않는다).
     */
    private fun scopeOf(principal: WebUserPrincipal, scope: DataScope): DataScope =
        branchScopeGateway.applyDataScope(principal, scope)

    /**
     * 화면에서 고른 지점 코드 → 조회 코드 — 셀렉터 화이트리스트로 판정한 뒤 `BranchMapping` 으로 확장한다.
     * 권한 밖 지점을 요청하면 매칭 0건. 신/구 방식은 개발자 도구 토글로 전환한다(비교 검증용 한시 조치).
     */
    private fun codesOf(principal: WebUserPrincipal, costCenterCodes: List<String>): List<String> =
        branchScopeGateway.resolveQueryCodes(principal, costCenterCodes, BranchScopeProfile.SALES)

}
