package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.SelectorBranchResult
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.domain.sales.dto.request.PosSalesAccountListRequest
import com.otoki.powersales.domain.sales.dto.request.PosSalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.PosSalesAccountListResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.service.PosSalesAdminQueryService
import com.otoki.powersales.domain.sales.service.PosSalesDashboardExcelExporter
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 「POS매출」 web admin 대시보드 — POS `live_pos_sales_dh` 거래처/제품별 POS매출.
 *
 * 레거시 「POS매출 조회」 (`/sales/posMain` → `posmain.jsp`) 의 거래처별 확장. 전산실적
 * ([AdminElectronicSalesDashboardController]) 과 동일한 권한 자원 [SALES_DASHBOARD_RESOURCE]/READ +
 * endpoint 구성. 유통형태/거래처유형/중·소분류 옵션과 제품 검색은 전산실적의
 * `/filter-options` / `/product-lookup` 을 재사용한다 (동일 권한 가드, 메인 DB 메타).
 *
 * ## 2단 조회 (외부 POS DB 부하 축소)
 * - `/accounts` (1단): 지점/거래처명/유통형태/거래처유형으로 메인 DB 거래처 목록만 조회 (POS 미접촉).
 * - `/list`, `/list/export` (2단): 1단에서 선택한 거래처(accountIds, 최대 20)만 외부 POS DB 집계.
 */
@RestController
@RequestMapping("/api/v1/admin/sales/pos")
@PermissionResource(SALES_DASHBOARD_RESOURCE)
class AdminPosSalesController(
    private val queryService: PosSalesAdminQueryService,
    private val excelExporter: PosSalesDashboardExcelExporter,
    private val branchScopeGateway: BranchScopeGateway,
) {

    /**
     * 1단 — 조건에 맞는 거래처 목록 조회 (외부 POS DB 미접촉). 지점/거래처명/유통형태/거래처유형 필터.
     *
     * `costCenterCodes` 는 **선택**이다 — 거래처명만으로도 검색할 수 있게 해 "지점을 먼저 골라야
     * 거래처를 고를 수 있다" 는 선행 강제를 없앤다. 대신 응답 각 행에 지점 셀렉터 역산 결과를 실어,
     * 화면이 고른 거래처들의 지점을 체크박스에 자동 반영한다.
     */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/accounts")
    fun getAccounts(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) costCenterCodes: List<String>?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
    ): ResponseEntity<ApiResponse<PosSalesAccountListResponse>> {
        val selectedCodes = costCenterCodes?.filter { it.isNotBlank() } ?: emptyList()
        val request = PosSalesAccountListRequest(
            // 미선택은 빈 목록 그대로 전달 — 서비스가 "지점 필터 없음" 으로 해석한다 (권한은 DataScope 가 가드).
            costCenterCodes = if (selectedCodes.isEmpty()) emptyList() else codesOf(principal, selectedCodes),
            customerKeyword = customerKeyword,
            distributionChannels = distributionChannels ?: emptyList(),
            accountTypes = accountTypes ?: emptyList(),
        )
        val response = queryService.getAccounts(scopeOf(principal, scope), request)
        return ResponseEntity.ok(ApiResponse.success(withSelectorBranch(principal, response)))
    }

    /** 2단 — 선택 거래처별 POS매출 명세. 기간(일 단위, 최대 31일) + 제품/분류 필터 + 페이징 + 정렬. */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun getList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam accountIds: List<Long>,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ApiResponse<PosSalesDashboardListResponse>> {
        val request = PosSalesDashboardListRequest(
            startDate = startDate, endDate = endDate, accountIds = accountIds,
            productIds = productIds ?: emptyList(),
            category2 = category2, category3 = category3,
            page = page, size = size, sort = sort,
        )
        val response = queryService.getList(scopeOf(principal, scope), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 2단 — 선택 거래처별 POS매출 명세 엑셀 다운로드. 페이징 미적용 (선택 거래처 전체 export). */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/list/export")
    fun exportList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam accountIds: List<Long>,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ByteArray> {
        val request = PosSalesDashboardListRequest(
            startDate = startDate, endDate = endDate, accountIds = accountIds,
            productIds = productIds ?: emptyList(),
            category2 = category2, category3 = category3,
            page = 0, size = Int.MAX_VALUE, sort = sort,
        )
        val items = queryService.getListForExport(scopeOf(principal, scope), request)
        val excel = excelExporter.export(startDate, endDate, items)
        return ExcelResponseUtils.build(excel)
    }

    /** 단건 거래처 상세 — 제품별 POS매출 명세 (목록과 동일한 기간/제품/분류 필터 반영). */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/detail/{customerId}")
    fun getDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable customerId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
    ): ResponseEntity<ApiResponse<PosSalesRangeResponse>> {
        val response = queryService.getDetail(
            scope = scopeOf(principal, scope),
            customerId = customerId,
            startDate = startDate,
            endDate = endDate,
            productIds = productIds ?: emptyList(),
            category2 = category2,
            category3 = category3,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
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

    /**
     * 거래처 → 지점 셀렉터 역산 결과를 1단 응답에 부착 (목록 1회당 gateway 1회 호출).
     *
     * 거래처의 `branchCode` 는 상위 조직/별칭 코드로 적재돼 있을 수 있어 셀렉터 옵션값과 그대로
     * 일치하지 않는다. 매핑은 `BranchMapping` 캐시를 가진 서버만 풀 수 있으므로 여기서 해소해
     * 화면에는 바로 쓸 수 있는 코드로 내려준다. 후보가 둘 이상이거나(롤업) 권한 밖이면 코드 없이
     * 상태만 내려 화면이 자동 선택 대신 안내하도록 한다.
     */
    private fun withSelectorBranch(
        principal: WebUserPrincipal,
        response: PosSalesAccountListResponse,
    ): PosSalesAccountListResponse {
        val resolved = branchScopeGateway.resolveSelectorBranches(
            principal,
            BranchScopeProfile.SALES,
            response.items.map { it.branchCode },
        )
        return response.copy(
            items = response.items.map { item ->
                val result = resolved[item.branchCode] ?: SelectorBranchResult.OutOfScope
                item.copy(
                    selectorBranchCode = result.branchCode,
                    selectorBranchName = result.branchName,
                    selectorBranchStatus = result.status,
                )
            },
        )
    }
}
