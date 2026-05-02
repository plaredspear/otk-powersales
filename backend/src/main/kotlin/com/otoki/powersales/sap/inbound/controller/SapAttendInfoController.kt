package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoDetail
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapAttendInfoService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 출근 정보 인바운드 컨트롤러. (Spec #562)
 *
 * 응답 포맷은 [SapResultWrapper] 를 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 * 한 요청 최대 행 수 초과는 [com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException]
 * 을 통해 413 PAYLOAD_TOO_LARGE 로 응답된다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapAttendInfoController(
    private val sapAttendInfoService: SapAttendInfoService
) {

    @Operation(
        summary = "출근 정보 적재 (UPSERT)",
        description = """
            SAP 출근 정보를 적재합니다.
            한 호출 최대 행 수 초과 시 `413 PAYLOAD_TOO_LARGE`.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/AttendInfo`
            - 레거시 Apex 클래스: `IF_REST_SAP_AttendInfo`
        """
    )
    @PostMapping("/attend-info")
    @PreAuthorize("hasAuthority('SCOPE_sap.attendance.write')")
    fun insertAttendInfo(
        @Valid @RequestBody request: AttendInfoRequest
    ): ResponseEntity<SapResultWrapper<AttendInfoDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sapAttendInfoService.insert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
