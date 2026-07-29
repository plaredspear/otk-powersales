package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.sales.dto.response.DailySalesHistoryListResponse
import com.otoki.powersales.domain.sales.service.AdminDailySalesHistoryService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 웹 「기준정보 > ORORA 일매출」 조회 컨트롤러.
 *
 * ORORA 일별 매출 적재 배치(`OroraDailySalesMaterializeBatch`)가 메인 RDS `daily_sales_history` 에
 * 적재한 결과를 거래처 + 매출월 단위로 조회한다(조회 전용). 권한 가드: `daily_sales_history` READ —
 * SF `DailySalesHistory__c` objectPermissions.allowRead 비트 매칭.
 */
@RestController
@RequestMapping("/api/v1/admin/daily-sales-histories")
@Validated
class AdminDailySalesHistoryController(
    private val adminDailySalesHistoryService: AdminDailySalesHistoryService,
) {

    /**
     * 거래처(필수) + 매출월(필수) 의 일별 매출 일람 조회.
     *
     * 거래처를 필수로 둬 월 단위 전 거래처 스캔을 차단한다 (한 거래처의 한 달치는 최대 31행).
     */
    @GetMapping
    @RequiresSfPermission(entity = "daily_sales_history", operation = SfPermissionOperation.READ)
    fun getDailySalesHistories(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam @Size(min = 1, max = 100) accountCode: String,
        @RequestParam @Size(min = 6, max = 7) salesMonth: String,
    ): ResponseEntity<ApiResponse<DailySalesHistoryListResponse>> {
        val response = adminDailySalesHistoryService.getDailySalesHistories(scope, accountCode, salesMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
