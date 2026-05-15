package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.claim.ClaimStatusDetail
import com.otoki.powersales.sap.inbound.dto.claim.ClaimStatusRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapClaimStatusService
import io.swagger.v3.oas.annotations.Operation
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
 * SAP 인바운드 예외 핸들러 ([SapInboundExceptionHandler])
 * 의 `assignableTypes` 에 등록되어 있어 동일 응답 포맷을 공유한다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapClaimStatusController(
    private val sapClaimStatusService: SapClaimStatusService
) {

    @Operation(
        summary = "클레임 상태 갱신 (UPDATE only)",
        description = """
            SAP 클레임 상태를 갱신합니다 (존재하는 클레임만, 없으면 행 단위 부분 실패).

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/ClaimReceive`
            - 레거시 Apex 클래스: `IF_REST_SAP_ClaimReceive`

            **동작 범위 차이**: 레거시는 INSERT + UPDATE 둘 다 처리했으나, 신규는 **상태 UPDATE-only** 입니다. 신규 클레임 INSERT 시도는 `claim not found` 부분 실패로 응답됩니다.

            **응답 코드 변경**: 레거시는 `RESULT_CODE` 로 `"S"` (성공) / `"E"` (실패) 변종을 사용했으나, 신규는 다른 인터페이스와 동일하게 `"200"` + `RESULT_DETAIL.failures[]` 행 단위 보고로 통일됩니다.
        """
    )
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
