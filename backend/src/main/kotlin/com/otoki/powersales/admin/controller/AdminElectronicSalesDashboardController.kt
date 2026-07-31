package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.sales.dto.request.ElectronicSalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardDetailResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardFilterOptionsResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesProductLookupItem
import com.otoki.powersales.domain.sales.service.ElectronicSalesAdminQueryService
import com.otoki.powersales.domain.sales.service.ElectronicSalesDashboardExcelExporter
import com.otoki.powersales.domain.foundation.product.dto.response.ProductLookupFilterOptions
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesProductAdvancedResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 「월 매출(전산실적)」 web admin 대시보드 — POS `live_tot_sales_dh` 거래처/제품별 전산매출.
 *
 * 레거시 「월 매출 조회(전산)」 (`/sales/abcMain` → `abcmain.jsp`) 동등. 물류배부
 * ([AdminMonthlySalesDashboardController]) 와 동일한 권한 자원 [SALES_DASHBOARD_RESOURCE]/READ 사용.
 * filter-options / product-lookup 도 본 화면 도메인 권한으로 가드 (lookup 권한 정책 정합) — 물류배부·POS
 * 화면이 이 두 endpoint 를 재사용하는데 3화면이 같은 자원이라 교차 403 이 나지 않는다.
 */
@RestController
@RequestMapping("/api/v1/admin/sales/electronic")
@PermissionResource(SALES_DASHBOARD_RESOURCE)
@Validated
class AdminElectronicSalesDashboardController(
    private val queryService: ElectronicSalesAdminQueryService,
    private val excelExporter: ElectronicSalesDashboardExcelExporter,
    private val branchScopeGateway: BranchScopeGateway,
) {

    /** 거래처별 전산매출 명세 — 기간(일 단위) + 유통형태/거래처유형/제품/분류 필터 + 페이징 + 정렬. */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun getList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ApiResponse<ElectronicSalesDashboardListResponse>> {
        val request = ElectronicSalesDashboardListRequest(
            startDate = startDate, endDate = endDate, costCenterCodes = codesOf(principal, costCenterCodes),
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            distributionChannels = distributionChannels ?: emptyList(),
            accountTypes = accountTypes ?: emptyList(),
            productIds = productIds ?: emptyList(),
            category2 = category2, category3 = category3,
            page = page, size = size, sort = sort,
        )
        val response = queryService.getList(scopeOf(principal, scope), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 거래처별 전산매출 명세 엑셀 다운로드. 페이징 미적용 (권한 범위 전체 export). */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/list/export")
    fun exportList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ByteArray> {
        val request = ElectronicSalesDashboardListRequest(
            startDate = startDate, endDate = endDate, costCenterCodes = codesOf(principal, costCenterCodes),
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            distributionChannels = distributionChannels ?: emptyList(),
            accountTypes = accountTypes ?: emptyList(),
            productIds = productIds ?: emptyList(),
            category2 = category2, category3 = category3,
            page = 0, size = Int.MAX_VALUE, sort = sort,
        )
        val items = queryService.getListForExport(scopeOf(principal, scope), request)
        val excel = excelExporter.export(startDate, endDate, items)
        return ExcelResponseUtils.build(excel)
    }

    /** 단건 거래처 상세 — 제품별 전산매출 명세 (목록과 동일한 기간/제품/분류 필터 반영). */
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
    ): ResponseEntity<ApiResponse<ElectronicSalesDashboardDetailResponse>> {
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

    /** 조회 조건 옵션 — 유통형태 / 거래처유형 / 제품 중·소분류 (메인 DB distinct). */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/filter-options")
    fun getFilterOptions(): ResponseEntity<ApiResponse<ElectronicSalesDashboardFilterOptionsResponse>> {
        return ResponseEntity.ok(ApiResponse.success(queryService.getFilterOptions()))
    }

    /** 조회 조건 제품 검색 — 제품명/제품코드/바코드 부분일치 (바코드 보유 제품 한정, 최대 50건). */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/product-lookup")
    fun lookupProducts(
        @RequestParam keyword: String,
    ): ResponseEntity<ApiResponse<List<ElectronicSalesProductLookupItem>>> {
        return ResponseEntity.ok(ApiResponse.success(queryService.searchProducts(keyword)))
    }

    /**
     * 조회 조건 제품 고급 검색 — 키워드 + 대/중/소분류 + 제품상태 조합 (페이징).
     *
     * 드롭다운 빠른 검색([lookupProducts])은 상위 50건만 반환해 결과가 많은 키워드에서 뒤쪽 제품에
     * 도달할 수 없다. 본 endpoint 는 페이지 이동으로 전체 결과를 열람하게 한다 — 행사마스터
     * `/products/lookup` 과 동일 목적이나, 그쪽은 promotion.READ 가드라 재사용 불가하므로
     * 본 화면 도메인 권한(`monthly_sales_history`) 으로 별도 신설한다.
     *
     * 검색 대상은 드롭다운과 동일하게 소비자 바코드 보유 제품 한정 — 두 진입점의 결과 집합이
     * 어긋나지 않게 하고, 매출이 절대 잡히지 않는 제품의 헛선택을 차단한다.
     *
     * POS 매출 조회(`/list`) 화면과 월 매출(전산실적) 화면이 공유한다.
     */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/product-lookup/advanced")
    fun lookupProductsAdvanced(
        @RequestParam(required = false) @Size(min = 1, max = 50) keyword: String?,
        @RequestParam(required = false) category1: String?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false) productStatus: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<ApiResponse<ElectronicSalesProductAdvancedResponse>> {
        val response = queryService.searchProductsAdvanced(
            keyword = keyword,
            category1 = category1,
            category2 = category2,
            category3 = category3,
            productStatus = productStatus,
            page = page,
            size = size,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 제품 고급 검색 모달의 필터 드롭다운 옵션 — 대/중/소분류 트리 + 제품상태.
     *
     * 기존 [getFilterOptions] 의 categories 는 중/소분류 2단이라 대분류 필터를 채울 수 없어 분리한다.
     * `/products/lookup-filter-options` 는 promotion.READ 가드라 재사용 불가.
     * 모달을 여는 시점에만 조회한다.
     */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/product-lookup/filter-options")
    fun productLookupFilterOptions(): ResponseEntity<ApiResponse<ProductLookupFilterOptions>> {
        return ResponseEntity.ok(ApiResponse.success(queryService.getProductLookupFilterOptions()))
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
