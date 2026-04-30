package com.otoki.powersales.claim.controller

import com.otoki.powersales.claim.dto.sap.ClaimStatusDetail
import com.otoki.powersales.claim.dto.sap.ClaimStatusRequest
import com.otoki.powersales.claim.service.SapClaimStatusService
import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 클레임 상태 인바운드 컨트롤러. (Spec #561)
 *
 * 응답 포맷은 [SapResultWrapper] 를 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 * SAP 인바운드 예외 핸들러 ([com.otoki.powersales.sap.inbound.controller.SapInboundExceptionHandler])
 * 의 `assignableTypes` 에 등록되어 있어 동일 응답 포맷을 공유한다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapClaimStatusController(
    private val sapClaimStatusService: SapClaimStatusService
) {

    @PostMapping("/claim-status")
    @PreAuthorize("hasAuthority('SCOPE_sap.claim.write')")
    fun updateClaimStatus(
        @Valid @RequestBody request: ClaimStatusRequest
    ): ResponseEntity<SapResultWrapper<ClaimStatusDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sapClaimStatusService.update(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
