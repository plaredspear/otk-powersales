package com.otoki.powersales.order.service

import com.otoki.powersales.order.dto.response.LoanInquiryResponse
import com.otoki.powersales.sap.outbound.sender.LoanInquirySender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 거래처 여신 한도 조회 서비스 (Spec #594).
 *
 * SAP `LoanInquiry` 직접 호출 (캐시 없음, 레거시 동등 — `OrderController.java:380, 797`).
 *
 * **서버 재검증** (#592 주문 등록 시점) 도 본 서비스를 재사용한다 — 동일 sender / 동일 페이로드 (스펙 §7.3).
 */
@Service
@Transactional(readOnly = true)
class LoanInquiryService(
    private val loanInquirySender: LoanInquirySender,
) {

    fun inquireByExternalKey(externalKey: String): LoanInquiryResponse {
        val sap = loanInquirySender.inquire(externalKey)
        return LoanInquiryResponse.from(externalKey, sap)
    }
}
