package com.otoki.powersales.order.dto.response

import com.otoki.powersales.external.sap.outbound.sender.LoanInquirySapResult
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * 거래처 여신 한도 조회 응답 (Spec #594 §5).
 *
 * SAP `LoanInquiry` 응답을 카멜케이스로 변환. SAP 송수신 키는 레거시 그대로
 * `TotalCredit / CreditBalance / CreditCurrency` 사용 (Q3 결정).
 */
data class LoanInquiryResponse(
    val externalKey: String,
    val totalCredit: BigDecimal?,
    val creditBalance: BigDecimal?,
    val currency: String?,
    val dataAsOf: OffsetDateTime,
) {
    companion object {
        fun from(externalKey: String, sap: LoanInquirySapResult, asOf: OffsetDateTime = OffsetDateTime.now()): LoanInquiryResponse =
            LoanInquiryResponse(
                externalKey = externalKey,
                totalCredit = sap.totalCredit,
                creditBalance = sap.creditBalance,
                currency = sap.currency,
                dataAsOf = asOf,
            )
    }
}
