package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapErpOrderService
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
