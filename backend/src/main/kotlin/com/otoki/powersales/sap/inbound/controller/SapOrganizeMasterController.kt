package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterDetail
import com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapOrganizeMasterService
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
class SapOrganizeMasterController(
    private val sapOrganizeMasterService: SapOrganizeMasterService
) {

    @PostMapping("/organization")
    @PreAuthorize("hasAuthority('SCOPE_sap.org.write')")
    fun replaceOrganizations(
        @Valid @RequestBody request: OrganizeMasterRequest
    ): ResponseEntity<SapResultWrapper<OrganizeMasterDetail>> {
        val items = request.reqItemList
            ?: throw SapInvalidPayloadException("req_item_list 는 필수입니다")
        val detail = sapOrganizeMasterService.replaceAll(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
