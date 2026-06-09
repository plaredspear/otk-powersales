package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.PermissionResource
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sales.dto.request.OroraSalesMaterializeRequest
import com.otoki.powersales.sales.materialize.OroraDailyMaterializeResult
import com.otoki.powersales.sales.materialize.OroraMonthlyMaterializeResult
import com.otoki.powersales.sales.materialize.OroraSalesMaterializeFacade
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ORORA 매출이력 수동 적재 트리거 admin API (Spec #855).
 *
 * 평상시 적재는 스케줄 배치([com.otoki.powersales.batch.OroraDailySalesMaterializeBatch] /
 * [com.otoki.powersales.batch.OroraMonthlySalesMaterializeBatch]) 가 수행하며, 본 API 는
 * 특정 월 소급 재적재 등 운영자 수동 트리거용 (레거시의 mdt 수동 지정 유연성 대체, Q2).
 *
 * 권한: 매출/실적 도메인(`monthly_sales_history`) 의 EDIT (upsert = 생성+갱신).
 */
@RestController
@RequestMapping("/api/v1/admin/sales/materialize")
@PermissionResource("monthly_sales_history")
class AdminOroraSalesMaterializeController(
    private val facade: OroraSalesMaterializeFacade,
) {

    @Operation(summary = "ORORA 월별 마감 적재 (수동 트리거)")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.EDIT)
    @PostMapping("/monthly")
    fun materializeMonthly(
        @Valid @RequestBody(required = false) request: OroraSalesMaterializeRequest?,
    ): ResponseEntity<ApiResponse<OroraMonthlyMaterializeResult>> {
        val result = facade.materializeMonthly(request?.salesMonth)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @Operation(summary = "ORORA 일별 매출 적재 + 월별 합계 갱신 (수동 트리거)")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.EDIT)
    @PostMapping("/daily")
    fun materializeDaily(
        @Valid @RequestBody(required = false) request: OroraSalesMaterializeRequest?,
    ): ResponseEntity<ApiResponse<OroraDailyMaterializeResult>> {
        val result = facade.materializeDaily(request?.salesMonth)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
