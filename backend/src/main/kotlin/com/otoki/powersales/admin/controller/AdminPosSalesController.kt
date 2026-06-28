package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.foundation.product.dto.response.ProductDto
import com.otoki.powersales.domain.foundation.product.service.ProductService
import com.otoki.powersales.domain.sales.dto.request.PosSalesRangeRequest
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.service.PosSalesService
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * POS매출 조회 admin API Controller.
 *
 * 레거시 `promotion/month/posmain.jsp` (POS DB `live_pos_sales_dh`) 의 web admin 이관.
 * 매출/실적 도메인 권한(`monthly_sales_history`) 을 재사용한다.
 */
@RestController
@RequestMapping("/api/v1/admin/sales/pos")
@PermissionResource("monthly_sales_history")
class AdminPosSalesController(
    private val posSalesService: PosSalesService,
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val productService: ProductService,
) {

    /**
     * POS매출 조회 화면의 매출 조회 제품 검색 — 제품명/제품코드/바코드 통합 검색.
     * GET /api/v1/admin/sales/pos/products
     *
     * 모바일 POS매출(`/mobile/products/search`)과 동일하게 [ProductService.searchProducts] 를 재사용한다
     * (바코드 포함 [ProductDto] 반환). 제품 도메인 권한 없이 매출/실적 권한(`monthly_sales_history`)으로
     * 호출 가능 — POS매출 화면 진입(monthly_sales_history:R)만으로 매출 조회 제품을 검색할 수 있도록 가드.
     *
     * @param query 검색어 (제품명/제품코드/바코드). 숫자면 제품코드+바코드 포함 매칭.
     * @param type 검색 유형 (text 기본 / barcode — 바코드 정확 조회).
     */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/products")
    fun searchProducts(
        @RequestParam @Size(min = 1, max = 50) query: String,
        @RequestParam(required = false, defaultValue = "text") type: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "30") size: Int,
    ): ResponseEntity<ApiResponse<Page<ProductDto>>> {
        val result = productService.searchProducts(query, type, page, size)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * POS매출 조회 화면 지점 셀렉터 옵션 — 권한별 지점 화이트리스트.
     * GET /api/v1/admin/sales/pos/branches
     *
     * 전문행사조/여사원 일정/대시보드와 동일하게 [WomenScheduleBranchResolver] 로 산출(단일 출처).
     * 목록은 곧 해당 사용자가 조회 허용된 지점이며, 거래처 lookup(`/lookup-for-pos-sales`)은
     * DataScope 로 동일 화이트리스트를 재적용해 임의 branchCode 조회(IDOR)를 차단한다.
     */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = womenScheduleBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * POS매출 조회 (거래처 1곳 + 기간(시작/종료일), 제품별 실적)
     * GET /api/v1/admin/sales/pos
     *
     * 레거시 `posmain.jsp` 의 daterangepicker 정합 — 기간(YYYY-MM-DD ~ YYYY-MM-DD) 단위 조회.
     * 합계금액/수량은 [PosSalesRangeResponse] 가 서버 산출분으로 제공.
     */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping
    fun getPosSales(
        @Valid request: PosSalesRangeRequest,
    ): ResponseEntity<ApiResponse<PosSalesRangeResponse>> {
        val response = posSalesService.getPosSalesByRange(
            request.customerId, request.startDate, request.endDate, request.barcodeList(),
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * POS매출 제품별 명세 엑셀 다운로드 (거래처 1곳 + 기간(시작/종료일) + 선택 바코드) — 조회와 동일 데이터.
     * GET /api/v1/admin/sales/pos/export
     */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/export")
    fun exportPosSales(
        @Valid request: PosSalesRangeRequest,
    ): ResponseEntity<ByteArray> {
        val result = posSalesService.exportPosSalesByRange(
            request.customerId, request.startDate, request.endDate, request.barcodeList(),
        )
        return ExcelResponseUtils.build(result)
    }
}
