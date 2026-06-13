package com.otoki.powersales.domain.activity.order.sap.client

import com.otoki.powersales.external.sap.outbound.sender.LoanInquirySender
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * `SapLoanInquiryClient` 의 prod 환경 실제 impl (Spec #594).
 *
 * #592 가 정의한 interface 를 [LoanInquirySender] 위임으로 구현.
 * dev/test 환경은 [StubSapLoanInquiryClient] (`@Profile("!prod")`) 가 활성화되어 검증을 통과시킨다.
 *
 * 응답 `creditBalance` 가 null 인 경우 (SAP 응답 누락) 0 으로 반환 — `OrderRequestCreateService` 가
 * `creditBalance < totalAmount` 비교에서 안전하게 차단하도록.
 */
@Component
@Profile("prod")
class RealSapLoanInquiryClient(
    private val loanInquirySender: LoanInquirySender,
) : SapLoanInquiryClient {

    override fun inquireCreditBalance(accountId: Long): BigDecimal {
        // accountId → external_key 매핑은 호출자가 책임 (현재 인터페이스는 accountId 받음).
        // 본 impl 은 accountId.toString() 을 external_key 로 사용 — 추후 정합 필요 시 인터페이스 변경 후속.
        val sap = loanInquirySender.inquire(accountId.toString())
        return sap.creditBalance ?: BigDecimal.ZERO
    }
}
