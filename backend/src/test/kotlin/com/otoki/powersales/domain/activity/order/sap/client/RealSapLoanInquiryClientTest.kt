package com.otoki.powersales.domain.activity.order.sap.client

import com.otoki.powersales.domain.activity.order.exception.OrderInvalidRequestException
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.external.sap.outbound.sender.LoanInquirySender
import com.otoki.powersales.external.sap.outbound.sender.LoanInquirySapResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

@DisplayName("RealSapLoanInquiryClient 테스트")
class RealSapLoanInquiryClientTest {

    private val loanInquirySender: LoanInquirySender = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val client = RealSapLoanInquiryClient(loanInquirySender, accountRepository)

    private val accountId = 103L

    private fun account(externalKey: String?) = Account(
        id = accountId,
        name = "Test 거래처",
        externalKey = externalKey,
    )

    @Test
    @DisplayName("SAP 에는 accountId(PK)가 아니라 account.external_key 를 SAPAccountCode 로 전송한다")
    fun sendsExternalKeyNotAccountId() {
        every { accountRepository.findById(accountId) } returns Optional.of(account("0000012345"))
        every { loanInquirySender.inquire("0000012345") } returns
            LoanInquirySapResult(totalCredit = BigDecimal("1000"), creditBalance = BigDecimal("700"), currency = "KRW")

        val balance = client.inquireCreditBalance(accountId)

        assertThat(balance).isEqualByComparingTo(BigDecimal("700"))
        verify(exactly = 1) { loanInquirySender.inquire("0000012345") }
        verify(exactly = 0) { loanInquirySender.inquire(accountId.toString()) }
    }

    @Test
    @DisplayName("creditBalance 누락(null) 시 0 반환 — 상위 여신 비교에서 안전 차단")
    fun nullCreditBalanceFallsBackToZero() {
        every { accountRepository.findById(accountId) } returns Optional.of(account("0000012345"))
        every { loanInquirySender.inquire("0000012345") } returns
            LoanInquirySapResult(totalCredit = null, creditBalance = null, currency = null)

        assertThat(client.inquireCreditBalance(accountId)).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    @DisplayName("거래처 미존재 시 OrderInvalidRequestException")
    fun missingAccountThrows() {
        every { accountRepository.findById(accountId) } returns Optional.empty()

        assertThatThrownBy { client.inquireCreditBalance(accountId) }
            .isInstanceOf(OrderInvalidRequestException::class.java)
    }

    @Test
    @DisplayName("external_key 공란/null 이면 SAP 호출 없이 OrderInvalidRequestException")
    fun blankExternalKeyThrowsWithoutSapCall() {
        every { accountRepository.findById(accountId) } returns Optional.of(account(" "))

        assertThatThrownBy { client.inquireCreditBalance(accountId) }
            .isInstanceOf(OrderInvalidRequestException::class.java)
        verify(exactly = 0) { loanInquirySender.inquire(any()) }
    }
}
