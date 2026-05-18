package com.otoki.powersales.sf.inbound.controller

import com.otoki.powersales.sf.inbound.dto.SfResultWrapper
import com.otoki.powersales.sf.inbound.dto.sales.SfMonthlySalesHistoryRequest
import com.otoki.powersales.sf.inbound.dto.sales.SfSalesHistoryDetail
import com.otoki.powersales.sf.inbound.exception.SfInvalidPayloadException
import com.otoki.powersales.sf.inbound.service.SfMonthlySalesHistoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SF 월 매출 이력 인바운드 컨트롤러 (Spec #775).
 *
 * SF Apex 가 client_credentials 으로 발급받은 token + `sf.write` scope 으로 호출.
 *
 * 응답 포맷은 [SfResultWrapper] 사용 (SAP 와 동일 형태로 호환성 유지).
 * 한 요청 최대 행 수 초과는 [com.otoki.powersales.sf.inbound.exception.SfPayloadTooLargeException]
 * 을 통해 413 PAYLOAD_TOO_LARGE 로 응답된다.
 */
@RestController
@RequestMapping("/api/v1/sf/inbound")
class SfMonthlySalesHistoryController(
    private val sfMonthlySalesHistoryService: SfMonthlySalesHistoryService
) {

    @Operation(
        summary = "[SF inbound] 월 매출 이력 적재 (청크 UPSERT)",
        description = """
            SF Apex 측에서 신규 backend 로 월 매출 이력을 (SAPAccountCode + SalesYearMonth) 기준으로 UPSERT.
            한 호출 최대 50,000행, 1,000행 청크 처리. 초과 시 `413 PAYLOAD_TOO_LARGE`.

            **인증**: client_credentials grant 으로 발급받은 token + `scope=sf.write`.
            **도메인 service**: SAP / SF 공유 (`MonthlySalesHistoryUpsertService`).
        """,
        security = [SecurityRequirement(name = "Bearer")]
    )
    @PostMapping("/monthly-sales-history")
    @PreAuthorize("hasAuthority('SCOPE_sf.write')")
    fun upsertMonthly(
        @Valid @RequestBody request: SfMonthlySalesHistoryRequest
    ): ResponseEntity<SfResultWrapper<SfSalesHistoryDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SfInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sfMonthlySalesHistoryService.upsert(items)
        return ResponseEntity.ok(SfResultWrapper.ok(detail))
    }
}
