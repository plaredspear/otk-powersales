package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapErpOrderService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP ERP 주문 인바운드 컨트롤러. (Spec #561)
 *
 * 응답 포맷은 [SapResultWrapper] 를 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapErpOrderController(
    private val sapErpOrderService: SapErpOrderService
) {

    @Operation(
        summary = "ERP 주문 적재 (헤더+라인 UPSERT)",
        description = """
            SAP ERP 주문 헤더와 라인을 단일 트랜잭션으로 UPSERT 합니다.
            라인 UPSERT 키는 (SAPOrderNumber 선두 0 1자 제거 + LineNumber).

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/ClientOrderSearch`
            - 레거시 Apex 클래스: `IF_REST_SAP_ClientOrderReceive`

            **명명 주의**: 레거시 URL 의 `Search` 는 클래스명(`Receive`) 과 다르며 실제 동작은 적재(POST)입니다.
        """
    )
    @PostMapping("/erp-order")
    @PreAuthorize("hasAuthority('SCOPE_sap.order.write')")
    fun upsertErpOrder(
        @Valid @RequestBody request: ErpOrderRequest
    ): ResponseEntity<SapResultWrapper<ErpOrderDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sapErpOrderService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
