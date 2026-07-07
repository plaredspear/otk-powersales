package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderRequest
import com.otoki.powersales.external.sap.inbound.service.SapErpOrderService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
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
    fun upsertErpOrder(
        @Valid @RequestBody request: ErpOrderRequest
    ): ResponseEntity<SapResultWrapper<ErpOrderDetail>> {
        // reqItemList 가 null/빈 리스트여도 400/422 로 거절하지 않고 적재 0건으로 200 을 반환한다.
        // (SAP 가 alias 세트에 없는 키 표기로 보내 null 로 바인딩되는 경우 실패를 받지 않게 하기 위한 임시 조치.
        //  근본 해결은 SAP 실제 키 확인 후 ErpOrderRequest @JsonAlias 추가 — ErpOrderRequest 주석 참고.)
        val items = request.reqItemList.orEmpty()
        val detail = sapErpOrderService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.Companion.ok(detail))
    }
}
