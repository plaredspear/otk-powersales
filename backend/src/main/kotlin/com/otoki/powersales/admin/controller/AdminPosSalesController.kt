package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.domain.sales.dto.request.PosSalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.PosSalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.service.PosSalesAdminQueryService
import com.otoki.powersales.domain.sales.service.PosSalesDashboardExcelExporter
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
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
 * ([AdminElectronicSalesDashboardController]) 과 동일한 권한 entity(`monthly_sales_history`)/READ +
 * endpoint 구성. 유통형태/거래처유형/중·소분류 옵션과 제품 검색은 전산실적의
 * `/filter-options` / `/product-lookup` 을 재사용한다 (동일 권한 가드, 메인 DB 메타).
 */
@RestController
@RequestMapping("/api/v1/admin/sales/pos")
@PermissionResource("monthly_sales_history")
class AdminPosSalesController(
    private val queryService: PosSalesAdminQueryService,
    private val excelExporter: PosSalesDashboardExcelExporter,
) {

    /** 거래처별 POS매출 명세 — 기간(일 단위, 최대 31일) + 유통형태/거래처유형/제품/분류 필터 + 페이징 + 정렬. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun getList(
        @CurrentDataScope scope: DataScope,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ApiResponse<PosSalesDashboardListResponse>> {
        val request = PosSalesDashboardListRequest(
            startDate = startDate, endDate = endDate, costCenterCodes = costCenterCodes,
            customerKeyword = customerKeyword,
            distributionChannels = distributionChannels ?: emptyList(),
            accountTypes = accountTypes ?: emptyList(),
            productIds = productIds ?: emptyList(),
            category2 = category2, category3 = category3,
            page = page, size = size, sort = sort,
        )
        val response = queryService.getList(scope, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 거래처별 POS매출 명세 엑셀 다운로드. 페이징 미적용 (권한 범위 전체 export). */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/list/export")
    fun exportList(
        @CurrentDataScope scope: DataScope,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ByteArray> {
        val request = PosSalesDashboardListRequest(
            startDate = startDate, endDate = endDate, costCenterCodes = costCenterCodes,
            customerKeyword = customerKeyword,
            distributionChannels = distributionChannels ?: emptyList(),
            accountTypes = accountTypes ?: emptyList(),
            productIds = productIds ?: emptyList(),
            category2 = category2, category3 = category3,
            page = 0, size = Int.MAX_VALUE, sort = sort,
        )
        val items = queryService.getListForExport(scope, request)
        val excel = excelExporter.export(startDate, endDate, items)
        return ExcelResponseUtils.build(excel)
    }

    /** 단건 거래처 상세 — 제품별 POS매출 명세 (목록과 동일한 기간/제품/분류 필터 반영). */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/detail/{customerId}")
    fun getDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable customerId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam(required = false) productIds: List<Long>?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
    ): ResponseEntity<ApiResponse<PosSalesRangeResponse>> {
        val response = queryService.getDetail(
            scope = scope,
            customerId = customerId,
            startDate = startDate,
            endDate = endDate,
            productIds = productIds ?: emptyList(),
            category2 = category2,
            category3 = category3,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
