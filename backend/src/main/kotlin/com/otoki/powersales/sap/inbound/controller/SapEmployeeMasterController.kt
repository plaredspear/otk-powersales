package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterDetail
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapEmployeeMasterService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 직원 마스터 인바운드 컨트롤러. (Spec #557)
 *
 * 응답 포맷은 [SapResultWrapper] 를 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 * SAP 경로 예외 응답은 [SapInboundExceptionHandler] 가 동일 포맷으로 변환한다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapEmployeeMasterController(
    private val sapEmployeeMasterService: SapEmployeeMasterService
) {

    @PostMapping("/employee")
    @PreAuthorize("hasAuthority('SCOPE_sap.employee.write')")
    fun upsertEmployee(
        @Valid @RequestBody request: EmployeeMasterRequest
    ): ResponseEntity<SapResultWrapper<EmployeeMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 필수")
        val detail = sapEmployeeMasterService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
