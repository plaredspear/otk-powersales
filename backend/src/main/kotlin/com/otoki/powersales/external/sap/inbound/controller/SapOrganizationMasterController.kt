package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.external.sap.inbound.dto.organization.OrganizationMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.organization.OrganizationMasterRequest
import com.otoki.powersales.external.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.external.sap.inbound.service.SapOrganizationMasterService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 조직 마스터 인바운드 컨트롤러. (Spec #556)
 *
 * 응답 포맷은 다른 SAP 인바운드 인터페이스(employee/account/...) 와 동일하게 [SapResultWrapper] 를
 * 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회). SAP 경로 예외 응답은
 * [SapInboundExceptionHandler] 가 동일 포맷으로 변환한다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapOrganizationMasterController(
    private val sapOrganizationMasterService: SapOrganizationMasterService
) {

    @Operation(
        summary = "조직 마스터 적재 (DELETE→INSERT)",
        description = """
            SAP 조직 마스터 데이터를 전체 교체합니다 (파괴적 인터페이스).

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/OrganizeMasterReceive`
            - 레거시 Apex 클래스: `IF_REST_SAP_OrganizeMasterReceive`

            **동작 차이**
            - 신규는 직전 적재 건수 대비 ±20% 초과 변동 시 `422 SANITY_CHECK_FAILED` 로 거부 (레거시는 가드 없이 무조건 실행)
        """
    )
    @PostMapping("/organization")
    @PreAuthorize("hasAuthority('SCOPE_sap.org.write')")
    fun replaceOrganizations(
        @Valid @RequestBody request: OrganizationMasterRequest
    ): ResponseEntity<SapResultWrapper<OrganizationMasterDetail>> {
        val items = request.reqItemList
            ?: throw SapInvalidPayloadException("req_item_list 는 필수입니다")
        val detail = sapOrganizationMasterService.replaceAll(items)
        return ResponseEntity.ok(SapResultWrapper.Companion.ok(detail))
    }
}
