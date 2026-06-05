package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.permission.PermissionResource
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sales.dto.request.ElectronicSalesDashboardListRequest
import com.otoki.powersales.sales.dto.response.ElectronicSalesDashboardDetailResponse
import com.otoki.powersales.sales.dto.response.ElectronicSalesDashboardListResponse
import com.otoki.powersales.sales.service.ElectronicSalesAdminQueryService
import com.otoki.powersales.sales.service.ElectronicSalesDashboardExcelExporter
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 「월 매출(전산실적)」 web admin 대시보드 — POS `live_tot_sales_dh` 거래처/제품별 전산매출.
 *
 * 레거시 「월 매출 조회(전산)」 (`/sales/abcMain` → `abcmain.jsp`) 동등. 물류배부
 * ([AdminMonthlySalesDashboardController]) 와 동일한 권한 entity(`monthly_sales_history`)/READ 사용.
 */
@RestController
@RequestMapping("/api/v1/admin/sales/electronic")
@PermissionResource("monthly_sales_history")
class AdminElectronicSalesDashboardController(
    private val queryService: ElectronicSalesAdminQueryService,
    private val excelExporter: ElectronicSalesDashboardExcelExporter,
) {

    /** 거래처별 전산매출 명세 — 페이징 + 정렬 + 필터. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun getList(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ApiResponse<ElectronicSalesDashboardListResponse>> {
        val request = ElectronicSalesDashboardListRequest(
            year = year, month = month, costCenterCodes = costCenterCodes,
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            page = page, size = size, sort = sort,
        )
        val response = queryService.getList(scope, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 거래처별 전산매출 명세 엑셀 다운로드. 페이징 미적용 (권한 범위 전체 export). */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/list/export")
    fun exportList(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ByteArray> {
        val request = ElectronicSalesDashboardListRequest(
            year = year, month = month, costCenterCodes = costCenterCodes,
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            page = 0, size = Int.MAX_VALUE, sort = sort,
        )
        val items = queryService.getListForExport(scope, request)
        val excel = excelExporter.export(year, month, items)
        return buildExcelResponse(excel)
    }

    /** 단건 거래처 상세 — 제품별 전산매출 명세. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/detail/{customerId}")
    fun getDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable customerId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<ElectronicSalesDashboardDetailResponse>> {
        val response = queryService.getDetail(scope, customerId, year, month)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    private fun buildExcelResponse(result: ElectronicSalesDashboardExcelExporter.ExcelResult): ResponseEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val encodedFilename = URLEncoder.encode(result.filename, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedFilename")
        return ResponseEntity.ok().headers(headers).body(result.bytes)
    }
}
