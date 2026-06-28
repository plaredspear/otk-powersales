package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.sales.dto.request.PosSalesRangeRequest
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.service.PosSalesService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
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
) {

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
            request.customerId, request.startDate, request.endDate, emptyList(),
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * POS매출 제품별 명세 엑셀 다운로드 (거래처 1곳 + 기간(시작/종료일)) — 조회와 동일 데이터.
     * GET /api/v1/admin/sales/pos/export
     */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/export")
    fun exportPosSales(
        @Valid request: PosSalesRangeRequest,
    ): ResponseEntity<ByteArray> {
        val result = posSalesService.exportPosSalesByRange(
            request.customerId, request.startDate, request.endDate,
        )
        return ExcelResponseUtils.build(result)
    }
}
