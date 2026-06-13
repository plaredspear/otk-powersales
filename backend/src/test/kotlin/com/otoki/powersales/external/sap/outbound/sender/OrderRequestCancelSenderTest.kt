package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender
import com.otoki.powersales.order.exception.OrderCancelSapFailedException
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
        "RequestNumber" to "ORD-20260504-000001",
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
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderChange"))
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
    @DisplayName("실패 — resultCode='E' 시 ORD_CANCEL_SAP_FAILED")
    fun resultCodeE() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderChange"))
            .andRespond(
                withSuccess(
                    """{"resultCode":"E","resutlMsg":"이미 취소된 주문"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThatThrownBy { sender.send(payload) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
            .hasMessageContaining("이미 취소된 주문")
    }

    @Test
    @DisplayName("실패 — HTML 응답 → ORD_CANCEL_SAP_FAILED")
    fun htmlResponse() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderChange"))
            .andRespond(
                withSuccess(
                    "<html><body>503 Service Unavailable</body></html>",
                    MediaType.TEXT_HTML,
                ),
            )

        assertThatThrownBy { sender.send(payload) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
    }

    @Test
    @DisplayName("실패 — SAP 5xx → ORD_CANCEL_SAP_FAILED")
    fun serverError() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderChange"))
            .andRespond(withServerError())

        assertThatThrownBy { sender.send(payload) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
    }
}
