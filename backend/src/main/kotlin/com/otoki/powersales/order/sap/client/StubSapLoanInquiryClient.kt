package com.otoki.powersales.order.sap.client

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Spec #592 의 임시 stub. **#594 후속 스펙이 실제 SAP 호출 impl 을 추가하면 본 클래스 제거 예정**.
 *
 * Stub 동작: 모든 거래처에 대해 `Long.MAX_VALUE` 여신 잔액 반환 — 검증을 모두 통과시킴.
 * prod profile 에선 미등록 → 기동 시 missing bean 에러 (운영 위험 차단).
 */
@Component
@Profile("!prod")
class StubSapLoanInquiryClient : SapLoanInquiryClient {

    private val log = LoggerFactory.getLogger(StubSapLoanInquiryClient::class.java)

    override fun inquireCreditBalance(accountId: Long): BigDecimal {
        log.debug("Stub LoanInquiry — accountId={}", accountId)
        return BigDecimal.valueOf(Long.MAX_VALUE)
    }
}
