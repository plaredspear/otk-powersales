package com.otoki.powersales.domain.activity.order.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.order.dto.response.LoanInquiryResponse
import com.otoki.powersales.domain.activity.order.service.LoanInquiryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 거래처 여신 한도 조회 API Controller (Spec #594).
 *
 * 모바일 주문 작성 사전 단계에서 거래처 여신 잔액을 조회한다.
 * 캐시 없이 매 호출 SAP 직호출 (레거시 정합 — `OrderController.java:380, 797`).
 */
@RestController
@RequestMapping("/api/v1/mobile/clients")
class LoanInquiryController(
    private val loanInquiryService: LoanInquiryService,
) {

    @GetMapping("/{externalKey}/loan-inquiry")
    fun getLoanInquiry(
        @PathVariable externalKey: String,
    ): ResponseEntity<ApiResponse<LoanInquiryResponse>> {
        val response = loanInquiryService.inquireByExternalKey(externalKey)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
