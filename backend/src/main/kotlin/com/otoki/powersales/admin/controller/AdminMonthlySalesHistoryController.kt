package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesHistoryListResponse
import com.otoki.powersales.domain.sales.service.AdminMonthlySalesHistoryService
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
 * 관리자 웹 「기준정보 > ORORA 월매출」 조회 컨트롤러.
 *
 * ORORA 월별 마감 적재 배치(`OroraMonthlySalesMaterializeBatch`)가 메인 RDS `monthly_sales_history` 에
 * 적재한 결과를 거래처 + 매출년월 단위로 조회한다(조회 전용). 권한 가드: `monthly_sales_history` READ —
 * SF `MonthlySalesHistory__c` objectPermissions.allowRead 비트 매칭.
 *
 * 일매출 화면이 `daily_sales_history` 가드인 것과 달리 여기는 `monthly_sales_history` 다 — 대상 테이블이
 * 다르고, 이 entity 는 월 매출(물류배부/전산실적)/POS매출 등 기존 화면과 권한을 공유한다.
 */
@RestController
@RequestMapping("/api/v1/admin/monthly-sales-histories")
@Validated
class AdminMonthlySalesHistoryController(
    private val adminMonthlySalesHistoryService: AdminMonthlySalesHistoryService,
) {

    /**
     * 거래처(필수) + 매출년월(필수) 의 월매출 적재 행 조회.
     *
     * 거래처를 필수로 둬 월 단위 전 거래처 스캔을 차단한다 (적재 upsert 키상 정상 데이터는 1행).
     */
    @GetMapping
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getMonthlySalesHistories(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam @Size(min = 1, max = 100) accountCode: String,
        @RequestParam @Size(min = 6, max = 7) salesMonth: String,
    ): ResponseEntity<ApiResponse<MonthlySalesHistoryListResponse>> {
        val response = adminMonthlySalesHistoryService.getMonthlySalesHistories(scope, accountCode, salesMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
