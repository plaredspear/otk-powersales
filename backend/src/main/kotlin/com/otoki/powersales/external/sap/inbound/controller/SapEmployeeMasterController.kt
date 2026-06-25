package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.external.sap.inbound.dto.employee.EmployeeMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.employee.EmployeeMasterRequest
import com.otoki.powersales.external.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.external.sap.inbound.service.SapEmployeeMasterService
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
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

    @Operation(
        summary = "직원 마스터 적재 (UPSERT)",
        description = """
            SAP 직원 마스터 데이터를 EmployeeCode 기준으로 UPSERT 합니다.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/employeeMaster`
            - 레거시 Apex 클래스: `IF_REST_SAP_EmployeeMaster`
        """
    )
    @PostMapping("/employee")
    fun upsertEmployee(
        @Valid @RequestBody request: EmployeeMasterRequest
    ): ResponseEntity<SapResultWrapper<EmployeeMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("reqItemList 필수")
        val detail = sapEmployeeMasterService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.Companion.ok(detail))
    }
}
