package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.common.dto.ApiResponse
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

@RestController
@RequestMapping("/api/v1/sap")
class SapOrganizeMasterController(
    private val sapOrganizeMasterService: SapOrganizeMasterService
) {

    @PostMapping("/organization")
    @PreAuthorize("hasAuthority('SCOPE_sap.org.write')")
    fun replaceOrganizations(
        @Valid @RequestBody request: OrganizeMasterRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        val items = request.reqItemList
            ?: throw SapInvalidPayloadException("req_item_list 가 누락되었습니다")
        sapOrganizeMasterService.replaceAll(items)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "SUCCESS"))
    }
}
