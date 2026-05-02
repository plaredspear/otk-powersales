package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.sales.DailySalesHistoryRequest
import com.otoki.powersales.sap.inbound.dto.sales.MonthlySalesHistoryRequest
import com.otoki.powersales.sap.inbound.dto.sales.SalesHistoryDetail
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapDailySalesHistoryService
import com.otoki.powersales.sap.inbound.service.SapMonthlySalesHistoryService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 일/월 매출 이력 인바운드 컨트롤러. (Spec #560)
 *
 * 응답 포맷은 [SapResultWrapper] 를 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 * 한 요청 최대 행 수 초과는 [com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException]
 * 을 통해 413 PAYLOAD_TOO_LARGE 로 응답된다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapSalesHistoryController(
    private val sapDailySalesHistoryService: SapDailySalesHistoryService,
    private val sapMonthlySalesHistoryService: SapMonthlySalesHistoryService
) {

    @Operation(
        summary = "일 매출 이력 적재 (청크 UPSERT)",
        description = """
            SAP 일 매출 이력을 (SAPAccountCode + SalesDate) 기준으로 UPSERT 합니다.
            한 호출 최대 50,000행, 1,000행 청크 처리. 초과 시 `413 PAYLOAD_TOO_LARGE`.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/DailyErpSalesInfoReceive`
            - 레거시 Apex 클래스: `IF_REST_SAP_DailyErpSalesInfoReceive`
        """
    )
    @PostMapping("/daily-sales-history")
    @PreAuthorize("hasAuthority('SCOPE_sap.sales.write')")
    fun upsertDaily(
        @Valid @RequestBody request: DailySalesHistoryRequest
    ): ResponseEntity<SapResultWrapper<SalesHistoryDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sapDailySalesHistoryService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }

    @Operation(
        summary = "월 매출 이력 적재 (청크 UPSERT)",
        description = """
            SAP 월 매출 이력을 (SAPAccountCode + SalesYearMonth) 기준으로 UPSERT 합니다.
            한 호출 최대 50,000행, 1,000행 청크 처리. 초과 시 `413 PAYLOAD_TOO_LARGE`.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/MonthlySalesHistory`
            - 레거시 Apex 클래스: `IF_REST_SAP_MonthlySalesHistory`
        """
    )
    @PostMapping("/monthly-sales-history")
    @PreAuthorize("hasAuthority('SCOPE_sap.sales.write')")
    fun upsertMonthly(
        @Valid @RequestBody request: MonthlySalesHistoryRequest
    ): ResponseEntity<SapResultWrapper<SalesHistoryDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sapMonthlySalesHistoryService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
