package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.product.BarcodeMasterRequest
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterRequest
import com.otoki.powersales.sap.inbound.dto.product.SystemCodeMasterRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapBarcodeMasterService
import com.otoki.powersales.sap.inbound.service.SapProductMasterService
import com.otoki.powersales.sap.inbound.service.SapSystemCodeMasterService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 제품 / 제품 바코드 / 시스템 공통 코드 마스터 인바운드 컨트롤러. (Spec #559)
 *
 * 응답 포맷은 [SapResultWrapper] 를 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 * SAP 경로 예외 응답은 [SapInboundExceptionHandler] 가 동일 포맷으로 변환한다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapProductMasterController(
    private val sapProductMasterService: SapProductMasterService,
    private val sapBarcodeMasterService: SapBarcodeMasterService,
    private val sapSystemCodeMasterService: SapSystemCodeMasterService
) {

    @Operation(
        summary = "제품 마스터 적재 (UPSERT)",
        description = """
            SAP 제품 마스터를 ProductCode 기준으로 UPSERT 합니다.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/ProductMasterSend`
            - 레거시 Apex 클래스: `IF_REST_SAP_ProductMasterSend`

            **명명 주의**: 레거시명 `ProductMasterSend` 의 `Send` 접미사는 SF→SAP 송신을 의미하지 않습니다. 실제는 SAP→backend 인바운드 (UPSERT) 로 동작합니다.
        """
    )
    @PostMapping("/product")
    @PreAuthorize("hasAuthority('SCOPE_sap.product.write')")
    fun upsertProduct(
        @Valid @RequestBody request: ProductMasterRequest
    ): ResponseEntity<SapResultWrapper<ProductMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 필수")
        val detail = sapProductMasterService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }

    @Operation(
        summary = "제품 바코드 마스터 적재 (UPSERT)",
        description = """
            SAP 제품 바코드 마스터를 (ProductCode + ProductUnit + ProductSequence) 복합키 기준으로 UPSERT 합니다.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/BarcodeMaster`
            - 레거시 Apex 클래스: `IF_REST_SAP_BarcodeMaster`
        """
    )
    @PostMapping("/product-barcode")
    @PreAuthorize("hasAuthority('SCOPE_sap.product.write')")
    fun upsertProductBarcode(
        @Valid @RequestBody request: BarcodeMasterRequest
    ): ResponseEntity<SapResultWrapper<ProductMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 필수")
        val detail = sapBarcodeMasterService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }

    @Operation(
        summary = "시스템 공통 코드 마스터 적재 (UPSERT)",
        description = """
            SAP 시스템 공통 코드 마스터를 (CompanyCode + GroupCode + DetailCode) 복합키 기준으로 UPSERT 합니다.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/SystemCodeMaster`
            - 레거시 Apex 클래스: `IF_REST_SAP_SystemCodeMaster`
        """
    )
    @PostMapping("/system-code")
    @PreAuthorize("hasAuthority('SCOPE_sap.product.write')")
    fun upsertSystemCode(
        @Valid @RequestBody request: SystemCodeMasterRequest
    ): ResponseEntity<SapResultWrapper<ProductMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 필수")
        val detail = sapSystemCodeMasterService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
