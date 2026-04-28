package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.sap.inbound.dto.account.AccountCategoryRequest
import com.otoki.powersales.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.sap.inbound.dto.account.ClientMasterRequest
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapAccountCategoryService
import com.otoki.powersales.sap.inbound.service.SapClientMasterService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SAP 거래처 / 거래처 카테고리 마스터 인바운드 컨트롤러. (Spec #558)
 *
 * 응답 포맷은 [SapResultWrapper] 를 직접 사용한다 (글로벌 ApiResponse 컨벤션 우회).
 * SAP 경로 예외 응답은 [SapInboundExceptionHandler] 가 동일 포맷으로 변환한다.
 */
@RestController
@RequestMapping("/api/v1/sap")
class SapAccountMasterController(
    private val sapClientMasterService: SapClientMasterService,
    private val sapAccountCategoryService: SapAccountCategoryService
) {

    @PostMapping("/account")
    @PreAuthorize("hasAuthority('SCOPE_sap.account.write')")
    fun upsertAccount(
        @Valid @RequestBody request: ClientMasterRequest
    ): ResponseEntity<SapResultWrapper<AccountMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("req_item_list 필수")
        val detail = sapClientMasterService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }

    @PostMapping("/account-category")
    @PreAuthorize("hasAuthority('SCOPE_sap.account.write')")
    fun upsertAccountCategory(
        @Valid @RequestBody request: AccountCategoryRequest
    ): ResponseEntity<SapResultWrapper<AccountMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("req_item_list 필수")
        val detail = sapAccountCategoryService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.ok(detail))
    }
}
