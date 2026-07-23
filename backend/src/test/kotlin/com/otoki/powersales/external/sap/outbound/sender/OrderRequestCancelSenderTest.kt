package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.domain.activity.order.exception.OrderCancelSapFailedException
import org.assertj.core.api.Assertions.assertThatCode
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

@DisplayName("OrderRequestCancelSender 테스트 (#597)")
class OrderRequestCancelSenderTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var sender: OrderRequestCancelSender
    private val objectMapper = ObjectMapper()

    private val payload = mapOf(
        "RequestNumber" to "OR00000001",
        "reqItemList" to listOf(
            mapOf("LineNumber" to "10", "ProductCode" to "P001", "LineChangeType" to "X"),
        ),
    )

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl("http://sap-mock")
        server = MockRestServiceServer.bindTo(builder).build()
        sender = OrderRequestCancelSender(builder.build(), objectMapper)
    }

    @Test
    @DisplayName("성공 — resultCode='S' 시 정상 반환")
    fun success() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03051"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"resultCode":"S","resutlMsg":"OK"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThatCode { sender.send(payload) }.doesNotThrowAnyException()
        server.verify()
    }

    @Test
    @DisplayName("실패 — resultCode='E'(HTTP 200 명시적 거부) → ORD_CANCEL_SAP_FAILED, rejected=true")
    fun resultCodeE() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03051"))
            .andRespond(
                withSuccess(
                    """{"resultCode":"E","resutlMsg":"이미 취소된 주문"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThatThrownBy { sender.send(payload) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
            .hasMessageContaining("이미 취소된 주문")
            // HTTP 200 + resultCode != 'S' = SAP 확정 거부 → 흔적 롤백 대상.
            .extracting { (it as OrderCancelSapFailedException).rejected }
            .isEqualTo(true)
    }

    @Test
    @DisplayName("실패 — HTML 응답(불확실) → ORD_CANCEL_SAP_FAILED, rejected=false")
    fun htmlResponse() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03051"))
            .andRespond(
                withSuccess(
                    "<html><body>503 Service Unavailable</body></html>",
                    MediaType.TEXT_HTML,
                ),
            )

        assertThatThrownBy { sender.send(payload) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
            // 형식 오류는 결과 불확실 → 흔적 유지.
            .extracting { (it as OrderCancelSapFailedException).rejected }
            .isEqualTo(false)
    }

    @Test
    @DisplayName("실패 — SAP 5xx(불확실) → ORD_CANCEL_SAP_FAILED, rejected=false")
    fun serverError() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03051"))
            .andRespond(withServerError())

        assertThatThrownBy { sender.send(payload) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
            // HTTP 5xx 는 결과 불확실 → 흔적 유지.
            .extracting { (it as OrderCancelSapFailedException).rejected }
            .isEqualTo(false)
    }
}
