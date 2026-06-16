package com.otoki.powersales.domain.sales.sfsync

import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("AdminSalesProgressRateMasterSyncTestService (SF IF_salesprogresssend 조회 테스트 도구) 테스트")
class AdminSalesProgressRateMasterSyncTestServiceTest {

    private lateinit var sfOutboundClient: SfOutboundClient
    private lateinit var service: AdminSalesProgressRateMasterSyncTestService

    @BeforeEach
    fun setUp() {
        sfOutboundClient = mockk()
        service = AdminSalesProgressRateMasterSyncTestService(sfOutboundClient, ObjectMapper())
    }

    private fun request(modDt: String = "20260410") =
        AdminSalesProgressRateMasterSyncTestRequest(modDt = modDt)

    @Test
    @DisplayName("MOD_DT 를 /IF_salesprogresssend 로 POST — 응답 rawBody 그대로 노출")
    fun test_callsSfWithModDt() {
        val rawList = """[{"AccountCode":"A-1","TargetSumAmount":"1000"}]"""
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = rawList)

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isTrue()
        assertThat(response.resultCode).isEqualTo("200")
        assertThat(response.rawResponse).isEqualTo(rawList)
        verify(exactly = 1) { sfOutboundClient.callApi("/IF_salesprogresssend", any()) }
    }

    @Test
    @DisplayName("apiMap — MOD_DT 키 하나만 담아 전송 + requestPayload 에 반영")
    fun test_apiMapContainsOnlyModDt() {
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/IF_salesprogresssend", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "[]")

        val response = service.test(userId = 1L, request = request("20260101"))

        assertThat(apiMapSlot.captured).containsExactlyEntriesOf(mapOf("MOD_DT" to "20260101"))
        assertThat(response.requestPayload).contains("\"MOD_DT\":\"20260101\"")
    }

    @Test
    @DisplayName("RESULT_CODE 가 200 이 아니면 success=false")
    fun test_nonSuccessResultCode() {
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } returns
            SfApiResponse(resultCode = "0", resultMsg = "응답 본문 비어있음", rawBody = "")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isFalse()
        assertThat(response.resultCode).isEqualTo("0")
    }

    @Test
    @DisplayName("SF OAuth 실패 — success=false, 예외 throw 안 함")
    fun test_oauthFailure() {
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } throws
            SfOAuthFailedException("재발급 후에도 401")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isFalse()
        assertThat(response.resultMsg).contains("401")
        assertThat(response.rawResponse).isNull()
    }

    @Test
    @DisplayName("SF 호출 일반 예외 — success=false, 메시지 노출")
    fun test_genericException() {
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } throws
            RuntimeException("connection reset")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isFalse()
        assertThat(response.resultMsg).isEqualTo("connection reset")
    }
}
