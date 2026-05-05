package com.otoki.powersales.order.service

import com.otoki.powersales.order.exception.LoanSapErrorException
import com.otoki.powersales.order.exception.LoanSapHtmlResponseException
import com.otoki.powersales.order.exception.LoanSapUnavailableException
import com.otoki.powersales.sap.outbound.sender.LoanInquirySapResult
import com.otoki.powersales.sap.outbound.sender.LoanInquirySender
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@DisplayName("LoanInquiryService 테스트 (#594)")
class LoanInquiryServiceTest {

    @Mock private lateinit var loanInquirySender: LoanInquirySender
    @InjectMocks private lateinit var service: LoanInquiryService

    private val externalKey = "EK001"

    @Test
    @DisplayName("정상 — SAP 응답을 카멜케이스로 매핑")
    fun success() {
        whenever(loanInquirySender.inquire(eq(externalKey))).thenReturn(
            LoanInquirySapResult(
                totalCredit = BigDecimal.valueOf(10_000_000),
                creditBalance = BigDecimal.valueOf(2_500_000),
                currency = "KRW",
            )
        )

        val response = service.inquireByExternalKey(externalKey)

        assertThat(response.externalKey).isEqualTo(externalKey)
        assertThat(response.totalCredit).isEqualByComparingTo("10000000")
        assertThat(response.creditBalance).isEqualByComparingTo("2500000")
        assertThat(response.currency).isEqualTo("KRW")
        assertThat(response.dataAsOf).isNotNull
    }

    @Test
    @DisplayName("잔여한도 0 — 정상 응답으로 전달 (한도 초과 판정은 호출자 책임)")
    fun zeroBalance() {
        whenever(loanInquirySender.inquire(eq(externalKey))).thenReturn(
            LoanInquirySapResult(
                totalCredit = BigDecimal.valueOf(10_000_000),
                creditBalance = BigDecimal.ZERO,
                currency = "KRW",
            )
        )

        val response = service.inquireByExternalKey(externalKey)

        assertThat(response.creditBalance).isEqualByComparingTo("0")
    }

    @Test
    @DisplayName("Sender 가 SAP 에러 던지면 그대로 전파")
    fun sapErrorPropagates() {
        whenever(loanInquirySender.inquire(eq(externalKey)))
            .thenThrow(LoanSapErrorException("거래처 미존재"))

        assertThatThrownBy { service.inquireByExternalKey(externalKey) }
            .isInstanceOf(LoanSapErrorException::class.java)
    }

    @Test
    @DisplayName("HTML 응답 가드 예외 전파")
    fun htmlResponsePropagates() {
        whenever(loanInquirySender.inquire(eq(externalKey)))
            .thenThrow(LoanSapHtmlResponseException())

        assertThatThrownBy { service.inquireByExternalKey(externalKey) }
            .isInstanceOf(LoanSapHtmlResponseException::class.java)
    }

    @Test
    @DisplayName("SAP 타임아웃/5xx 예외 전파")
    fun unavailablePropagates() {
        whenever(loanInquirySender.inquire(eq(externalKey)))
            .thenThrow(LoanSapUnavailableException("SAP 네트워크 오류"))

        assertThatThrownBy { service.inquireByExternalKey(externalKey) }
            .isInstanceOf(LoanSapUnavailableException::class.java)
    }
}
