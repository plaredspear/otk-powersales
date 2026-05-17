package com.otoki.powersales.admin.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.schedule.dto.response.SalesComparisonDetailResponse
import com.otoki.powersales.schedule.dto.response.SalesComparisonMiddleResponse
import com.otoki.powersales.schedule.dto.response.SalesComparisonSummaryResponse
import com.otoki.powersales.schedule.dto.response.SearchAccountCategoryItem
import com.otoki.powersales.schedule.service.AdminSalesComparisonService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/v1/admin/schedules/sales-comparison")
class AdminSalesComparisonController(
    private val adminSalesComparisonService: AdminSalesComparisonService
) {

    /** 거래처유형 picklist — `AccountCategoryMaster.useSearch=true` 항목 목록. */
    @GetMapping("/categories")
    fun getSearchCategories(): ResponseEntity<ApiResponse<List<SearchAccountCategoryItem>>> {
        val response = adminSalesComparisonService.getSearchCategories()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 집계 모드 — 배치적합성 × 거래처카테고리 거래처 수 집계표. */
    @GetMapping("/summary")
    fun getSummary(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>
    ): ResponseEntity<ApiResponse<SalesComparisonSummaryResponse>> {
        val response = adminSalesComparisonService.getSummary(year, month, costCenterCodes)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 중간집계 모드 — 거래처별 행 + 적합성별 소계 + 총계. */
    @GetMapping("/middle")
    fun getMiddle(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Int>?
    ): ResponseEntity<ApiResponse<SalesComparisonMiddleResponse>> {
        val response = adminSalesComparisonService.getMiddle(year, month, costCenterCodes, accountIds ?: emptyList())
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 상세 모드 — 사원별 행 + 총계. */
    @GetMapping("/detail")
    fun getDetail(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Int>?,
        @RequestParam(required = false) workingCategory1: String?,
        @RequestParam(required = false) workingCategory5: String?
    ): ResponseEntity<ApiResponse<SalesComparisonDetailResponse>> {
        val response = adminSalesComparisonService.getDetail(
            year, month, costCenterCodes,
            accountIds ?: emptyList(),
            workingCategory1,
            workingCategory5
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 집계표 엑셀 다운로드. */
    @GetMapping("/summary/export")
    fun exportSummary(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>
    ): ResponseEntity<ByteArray> = buildExcelResponse(
        adminSalesComparisonService.exportSummary(year, month, costCenterCodes)
    )

    /** 중간집계 엑셀 다운로드. */
    @GetMapping("/middle/export")
    fun exportMiddle(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Int>?
    ): ResponseEntity<ByteArray> = buildExcelResponse(
        adminSalesComparisonService.exportMiddle(year, month, costCenterCodes, accountIds ?: emptyList())
    )

    /** 상세 엑셀 다운로드. */
    @GetMapping("/detail/export")
    fun exportDetail(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Int>?,
        @RequestParam(required = false) workingCategory1: String?,
        @RequestParam(required = false) workingCategory5: String?
    ): ResponseEntity<ByteArray> = buildExcelResponse(
        adminSalesComparisonService.exportDetail(
            year, month, costCenterCodes,
            accountIds ?: emptyList(),
            workingCategory1,
            workingCategory5
        )
    )

    private fun buildExcelResponse(result: AdminSalesComparisonService.ExcelResult): ResponseEntity<ByteArray> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val encodedFilename = URLEncoder.encode(result.filename, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedFilename")
        return ResponseEntity.ok().headers(headers).body(result.bytes)
    }
}
