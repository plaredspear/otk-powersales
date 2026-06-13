package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.external.sap.outbound.sender.OrderRequestDetailSapSender
import org.assertj.core.api.Assertions.assertThat
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

@DisplayName("OrderRequestDetailSapSender 테스트 (#595)")
class OrderRequestDetailSapSenderTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var sender: OrderRequestDetailSapSender
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl("http://sap-mock")
        server = MockRestServiceServer.bindTo(builder).build()
        sender = OrderRequestDetailSapSender(builder.build(), objectMapper)
    }

    @Test
    @DisplayName("성공 — resultCode='S' + result[] 매핑 (18필드, EmployeeCode 미매핑)")
    fun success() {
        val body = """
            {"resultCode":"S","resutlMsg":"OK","result":[
              {"LineNumber":"00012","ProductCode":"1000023","ProductName":"진라면",
               "LineItemStatus":"OK","TotalQuantity":"10.000","Unit":"BOX",
               "SAPOrderNumber":"0300004993","OrderSalesAmount":"120000",
               "DeliveryRequestDate":"20260506","OrderDate":"20260504",
               "ShippingDriverName":"홍길동","ShippingVehicle":"12가3456",
               "ShippingDriverPhone":"010-1234-5678","ShippingScheduleTime":"120000",
               "ShippingCompleteTime":"143000","TotalQuantity_Box":"10.000",
               "ShippingQuantity_Box":"10","DefaultReason":"",
               "EmployeeCode":"OTG_ABAP07"}
            ]}
        """.trimIndent()

        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderRequestDetail"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val result = sender.fetchDetail("OR-0001234")

        assertThat(result).hasSize(1)
        val line = result!![0]
        assertThat(line.productCode).isEqualTo("1000023")
        assertThat(line.sapOrderNumber).isEqualTo("0300004993")
        assertThat(line.shippingDriverName).isEqualTo("홍길동")
        assertThat(line.shippingScheduleTime).isEqualTo("120000")
        assertThat(line.shippingCompleteTime).isEqualTo("143000")
        assertThat(line.shippingQuantityBox).isEqualTo("10")
        server.verify()
    }

    @Test
    @DisplayName("resultCode != 'S' → null (호출 측이 빈 결과로 처리)")
    fun sapError() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderRequestDetail"))
            .andRespond(
                withSuccess(
                    """{"resultCode":"E","resutlMsg":"존재하지 않음","result":[]}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = sender.fetchDetail("OR-NOTFOUND")

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("HTML 응답 → null (HTML 가드 — 단순 try-catch fallback)")
    fun htmlResponse() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderRequestDetail"))
            .andRespond(
                withSuccess(
                    "<html><body>503 Service Unavailable</body></html>",
                    MediaType.TEXT_HTML,
                ),
            )

        val result = sender.fetchDetail("OR-0001234")

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("SAP 5xx → null (단순 try-catch fallback)")
    fun serverError() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderRequestDetail"))
            .andRespond(withServerError())

        val result = sender.fetchDetail("OR-0001234")

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("JSON 파싱 실패 → null")
    fun parseFailure() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderRequestDetail"))
            .andRespond(withSuccess("not-json-content", MediaType.APPLICATION_JSON))

        val result = sender.fetchDetail("OR-0001234")

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("정상 응답 + result[] 빈 배열 → emptyList()")
    fun emptyResultArray() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/OrderRequestDetail"))
            .andRespond(
                withSuccess(
                    """{"resultCode":"S","resutlMsg":"OK","result":[]}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = sender.fetchDetail("OR-0001234")

        assertThat(result).isNotNull
        assertThat(result).isEmpty()
    }
}
