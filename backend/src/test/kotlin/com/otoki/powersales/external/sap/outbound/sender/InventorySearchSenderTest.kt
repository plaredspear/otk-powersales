package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.domain.activity.order.exception.InventorySapErrorException
import com.otoki.powersales.domain.activity.order.exception.InventorySapUnavailableException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

/**
 * [InventorySearchSender] 테스트 — 특히 한 호출당 50항목 상한 분할(chunk) 검증.
 *
 * 50건 상한은 레거시 SF 재고조회 화면(InventorySearchController) 정합. 대량 배열(99항목)
 * 요청이 SAP SD03070 ABAP 내부 오류(HTTP 500)로 터진 운영 사건의 재발 방어.
 */
@DisplayName("InventorySearchSender 테스트 — 50항목 분할 호출")
class InventorySearchSenderTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var sender: InventorySearchSender
    private val objectMapper = ObjectMapper()

    private val deliveryDate = LocalDate.of(2026, 7, 16)

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl("http://sap-mock")
        server = MockRestServiceServer.bindTo(builder).build()
        sender = InventorySearchSender(builder.build(), objectMapper)
    }

    @Test
    @DisplayName("50건 이하 — 단일 호출")
    fun singleCallUpTo50() {
        val codes = (1..50).map { "P%03d".format(it) }
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03070"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.request.length()").value(50))
            .andRespond(withSuccess(successBody(codes), MediaType.APPLICATION_JSON))

        val result = sender.search("1005139", codes, deliveryDate)

        assertThat(result).hasSize(50)
        server.verify()
    }

    @Test
    @DisplayName("51건 — 50 + 1 로 분할 2회 호출 후 결과 병합")
    fun chunkedInto50And1() {
        val codes = (1..51).map { "P%03d".format(it) }
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03070"))
            .andExpect(jsonPath("$.request.length()").value(50))
            .andExpect(jsonPath("$.request[0].ProductCode").value("P001"))
            .andRespond(withSuccess(successBody(codes.take(50)), MediaType.APPLICATION_JSON))
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03070"))
            .andExpect(jsonPath("$.request.length()").value(1))
            .andExpect(jsonPath("$.request[0].ProductCode").value("P051"))
            .andRespond(withSuccess(successBody(codes.drop(50)), MediaType.APPLICATION_JSON))

        val result = sender.search("1005139", codes, deliveryDate)

        // 두 호출 결과가 순서대로 병합되어야 한다 (라인 검증은 productCode 키 기반이라 누락 없이 전달).
        assertThat(result).hasSize(51)
        assertThat(result.map { it.productCode }).containsExactlyElementsOf(codes)
        server.verify()
    }

    @Test
    @DisplayName("분할 중 한 호출 실패 — 예외 전파 (전체 실패)")
    fun chunkFailurePropagates() {
        val codes = (1..51).map { "P%03d".format(it) }
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03070"))
            .andRespond(withSuccess(successBody(codes.take(50)), MediaType.APPLICATION_JSON))
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03070"))
            .andRespond(withServerError())

        assertThatThrownBy { sender.search("1005139", codes, deliveryDate) }
            .isInstanceOf(InventorySapUnavailableException::class.java)
    }

    @Test
    @DisplayName("빈 목록 — SAP 미호출")
    fun emptyCodesNoCall() {
        val result = sender.search("1005139", emptyList(), deliveryDate)

        assertThat(result).isEmpty()
        server.verify()
    }

    @Test
    @DisplayName("resultCode != 'S' → INVENTORY_SAP_ERROR + [SAP재고조회] prefix + resutlMsg 패스스루")
    fun sapErrorPrefixed() {
        server.expect(ExpectedCount.once(), requestTo("http://sap-mock/SD03070"))
            .andRespond(
                withSuccess(
                    """{"resultCode":"E","resutlMsg":"조회 가능한 거래처가 아닙니다"}""",
                    MediaType.APPLICATION_JSON
                )
            )

        assertThatThrownBy { sender.search("1005139", listOf("P001"), deliveryDate) }
            .isInstanceOf(InventorySapErrorException::class.java)
            .hasMessage("[SAP재고조회] 조회 가능한 거래처가 아닙니다")
    }

    private fun successBody(codes: List<String>): String {
        val items = codes.joinToString(",") {
            """{"ProductCode":"$it","ProductName":"N-$it","MinOrderingUnit":"EA","ConversionQuantity":"1","SupplyLimitQTY":"1000"}"""
        }
        return """{"resultCode":"S","resutlMsg":"OK","result":[$items]}"""
    }
}
