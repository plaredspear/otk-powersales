package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.external.sap.inbound.dto.account.AccountCategoryRequest
import com.otoki.powersales.external.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.account.ClientMasterRequest
import com.otoki.powersales.external.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.external.sap.inbound.service.SapAccountCategoryService
import com.otoki.powersales.external.sap.inbound.service.SapClientMasterService
import io.swagger.v3.oas.annotations.Operation
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

    @Operation(
        summary = "거래처 마스터 적재 (UPSERT)",
        description = """
            SAP 거래처 마스터(매장) 데이터를 SAPAccountCode 기준으로 UPSERT 합니다.
            EmployeeCode 가 직원 마스터에 존재하지 않으면 행 단위 부분 실패로 보고됩니다.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/ClientMasterReceive`
            - 레거시 Apex 클래스: `IF_REST_SAP_ClientMasterReceive`

            **레거시 정정**: `BusinessLiicenseNumber` (오타) → `BusinessLicenseNumber`
        """
    )
    @PostMapping("/account")
    @PreAuthorize("hasAuthority('SCOPE_sap.account.write')")
    fun upsertAccount(
        @Valid @RequestBody request: ClientMasterRequest
    ): ResponseEntity<SapResultWrapper<AccountMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("req_item_list 필수")
        val detail = sapClientMasterService.upsert(items)
        return ResponseEntity.ok(SapResultWrapper.Companion.ok(detail))
    }

    @Operation(
        summary = "거래처 카테고리 마스터 적재 (UPSERT)",
        description = """
            SAP 거래처 카테고리(등급) 마스터를 AccountCode 기준으로 UPSERT 합니다.

            **레거시 호환**
            - 레거시 엔드포인트: `POST /services/apexrest/sap/AccountMaster`
            - 레거시 Apex 클래스: `IF_REST_SAP_AccountMaster` (Salesforce `AccountCategoryMaster__c` SObject 매핑)

            **명명 주의**: 레거시명 `AccountMaster` 는 Salesforce 표준 `Account` (거래처 본체) 와 다른 카테고리 마스터입니다. 거래처 본체는 `/account` (`ClientMasterReceive`).
        """
    )
    @PostMapping("/account-category")
    @PreAuthorize("hasAuthority('SCOPE_sap.account.write')")
    fun upsertAccountCategory(
        @Valid @RequestBody request: AccountCategoryRequest
    ): ResponseEntity<SapResultWrapper<AccountMasterDetail>> {
        val items = request.reqItemList?.takeIf { it.isNotEmpty() }
            ?: throw SapInvalidPayloadException("req_item_list 필수")
        // 레거시 IF_REST_SAP_AccountMaster §4 정합 — 이 인터페이스만 예외 시 RESULT_MSG='Failed'
        // (공통 핸들러의 '내부 오류' 가 아니라 레거시 고유 메시지). 페이로드 검증 예외는 공통 경로 유지.
        return try {
            val detail = sapAccountCategoryService.upsert(items)
            ResponseEntity.ok(SapResultWrapper.Companion.ok(detail))
        } catch (ex: Exception) {
            ResponseEntity.ok(SapResultWrapper(SapResultWrapper.Companion.CODE_LEGACY_ERROR, RESULT_MSG_FAILED))
        }
    }

    companion object {
        // 레거시 IF_REST_SAP_AccountMaster 예외 응답 메시지 (§4 — 타 클래스의 'ERROR' 와 다름).
        private const val RESULT_MSG_FAILED: String = "Failed"
    }
}
