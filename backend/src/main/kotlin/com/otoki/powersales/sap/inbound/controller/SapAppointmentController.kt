package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.appointment.AppointmentDetail
import com.otoki.powersales.sap.inbound.dto.appointment.AppointmentRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapAppointmentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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

    @PostMapping("/appointment")
    @PreAuthorize("hasAuthority('SCOPE_sap.attendance.write')")
    fun insertAppointment(
        @Valid @RequestBody request: AppointmentRequest
    ): ResponseEntity<SapResultWrapper<AppointmentDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 빈 리스트")
        val detail = sapAppointmentService.insert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
