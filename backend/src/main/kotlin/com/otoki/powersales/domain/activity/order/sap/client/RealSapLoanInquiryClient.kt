package com.otoki.powersales.domain.activity.order.sap.client

import com.otoki.powersales.domain.activity.order.exception.OrderInvalidRequestException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
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
 * 거래처 ID → `account.external_key` (SAP 거래처 코드) 매핑 후 전송 — 레거시
 * `IF_REST_MOBILE_LoanInquiry` 의 `SAPAccountCode` = `Account.ExternalKey__c` 정합
 * ([RealSapInventorySearchClient] 와 동일 패턴).
 *
 * 응답 `creditBalance` 가 null 인 경우 (SAP 응답 누락) 0 으로 반환 — `OrderRequestCreateService` 가
 * `creditBalance < totalAmount` 비교에서 안전하게 차단하도록.
 */
@Component
@Profile("prod")
class RealSapLoanInquiryClient(
    private val loanInquirySender: LoanInquirySender,
    private val accountRepository: AccountRepository,
) : SapLoanInquiryClient {

    override fun inquireCreditBalance(accountId: Long): BigDecimal {
        val account = accountRepository.findById(accountId)
            .orElseThrow { OrderInvalidRequestException("거래처를 찾을 수 없습니다") }
        val externalKey = account.externalKey?.takeIf { it.isNotBlank() }
            ?: throw OrderInvalidRequestException("거래처 SAP 코드(external_key)가 없습니다")

        val sap = loanInquirySender.inquire(externalKey)
        return sap.creditBalance ?: BigDecimal.ZERO
    }
}
