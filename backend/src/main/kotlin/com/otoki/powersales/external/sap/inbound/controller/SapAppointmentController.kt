package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.external.sap.inbound.dto.appointment.AppointmentDetail
import com.otoki.powersales.external.sap.inbound.dto.appointment.AppointmentRequest
import com.otoki.powersales.external.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.external.sap.inbound.service.SapAppointmentService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 인사발령 인바운드 컨트롤러. (Spec #562)
 *
 * 응답 포맷은 [SapResultWrapper] 를 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapAppointmentController(
    private val sapAppointmentService: SapAppointmentService
) {

    @Operation(
        summary = "인사발령 적재 (UPSERT)",
        description = """
            SAP 인사발령 데이터를 적재합니다.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/Appointment`
            - 레거시 Apex 클래스: `IF_REST_SAP_Appointment`
        """
    )
    @PostMapping("/appointment")
    fun insertAppointment(
        @Valid @RequestBody request: AppointmentRequest
    ): ResponseEntity<SapResultWrapper<AppointmentDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sapAppointmentService.insert(items)
        return ResponseEntity.ok(SapResultWrapper.Companion.ok(detail))
    }
}
