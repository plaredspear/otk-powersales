package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.domain.activity.order.exception.LoanSapErrorException
import com.otoki.powersales.domain.activity.order.exception.LoanSapHtmlResponseException
import com.otoki.powersales.domain.activity.order.exception.LoanSapUnavailableException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

@DisplayName("LoanInquirySender 테스트 (#594)")
class LoanInquirySenderTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var sender: LoanInquirySender
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl("http://sap-mock")
        server = MockRestServiceServer.bindTo(builder).build()
        sender = LoanInquirySender(builder.build(), objectMapper)
    }

    @Test
    @DisplayName("성공 — resultCode='S' + result 필드 매핑")
    fun success() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03040"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"result":{"TotalCredit":"10000000","CreditBalance":"2500000","CreditCurrency":"KRW"},"resultCode":"S","resutlMsg":"OK"}""",
                    MediaType.APPLICATION_JSON
                )
            )

        val result = sender.inquire("EK001")

        assertThat(result.totalCredit).isEqualByComparingTo("10000000")
        assertThat(result.creditBalance).isEqualByComparingTo("2500000")
        assertThat(result.currency).isEqualTo("KRW")
        server.verify()
    }

    @Test
    @DisplayName("resultCode != 'S' → LOAN_SAP_ERROR + resutlMsg 패스스루")
    fun sapError() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03040"))
            .andRespond(
                withSuccess(
                    """{"resultCode":"E","resutlMsg":"거래처 미존재"}""",
                    MediaType.APPLICATION_JSON
                )
            )

        assertThatThrownBy { sender.inquire("EK_BAD") }
            .isInstanceOf(LoanSapErrorException::class.java)
            .hasMessageContaining("거래처 미존재")
    }

    @Test
    @DisplayName("HTML 응답 본문 → LOAN_SAP_HTML_RESPONSE")
    fun htmlResponse() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03040"))
            .andRespond(
                withSuccess(
                    "<html><body>503 Service Unavailable</body></html>",
                    MediaType.TEXT_HTML
                )
            )

        assertThatThrownBy { sender.inquire("EK001") }
            .isInstanceOf(LoanSapHtmlResponseException::class.java)
    }

    @Test
    @DisplayName("SAP 5xx → LOAN_SAP_UNAVAILABLE")
    fun serverError() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03040"))
            .andRespond(withServerError())

        assertThatThrownBy { sender.inquire("EK001") }
            .isInstanceOf(LoanSapUnavailableException::class.java)
    }
}
