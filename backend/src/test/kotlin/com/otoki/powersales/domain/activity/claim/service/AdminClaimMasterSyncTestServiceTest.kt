package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.request.AdminClaimMasterSyncTestRequest
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

@DisplayName("AdminClaimMasterSyncTestService (SF IF_SendClaimToPWS 조회 테스트 도구) 테스트")
class AdminClaimMasterSyncTestServiceTest {

    private lateinit var sfOutboundClient: SfOutboundClient
    private lateinit var service: AdminClaimMasterSyncTestService

    @BeforeEach
    fun setUp() {
        sfOutboundClient = mockk()
        service = AdminClaimMasterSyncTestService(sfOutboundClient, ObjectMapper())
    }

    private fun request(modDt: String = "20260410") = AdminClaimMasterSyncTestRequest(modDt = modDt)

    @Test
    @DisplayName("MOD_DT 를 /IF_SendClaimToPWS 로 POST — 응답 rawBody 그대로 노출")
    fun test_callsSfWithModDt() {
        val rawList = """[{"ProoductCode":"P-1","Status":"접수"}]"""
        every { sfOutboundClient.callApi("/IF_SendClaimToPWS", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = rawList)

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isTrue()
        assertThat(response.resultCode).isEqualTo("200")
        assertThat(response.rawResponse).isEqualTo(rawList)
        verify(exactly = 1) { sfOutboundClient.callApi("/IF_SendClaimToPWS", any()) }
    }

    @Test
    @DisplayName("apiMap — MOD_DT 키 하나만 담아 전송 + requestPayload 에 반영")
    fun test_apiMapContainsOnlyModDt() {
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/IF_SendClaimToPWS", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "[]")

        val response = service.test(userId = 1L, request = request("20260101"))

        assertThat(apiMapSlot.captured).containsExactlyEntriesOf(mapOf("MOD_DT" to "20260101"))
        assertThat(response.requestPayload).contains("\"MOD_DT\":\"20260101\"")
    }

    @Test
    @DisplayName("SF OAuth 실패 — success=false, 예외 throw 안 함")
    fun test_oauthFailure() {
        every { sfOutboundClient.callApi("/IF_SendClaimToPWS", any()) } throws
            SfOAuthFailedException("재발급 후에도 401")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isFalse()
        assertThat(response.resultMsg).contains("401")
        assertThat(response.rawResponse).isNull()
    }

    @Test
    @DisplayName("SF 호출 일반 예외 — success=false, 메시지 노출")
    fun test_genericException() {
        every { sfOutboundClient.callApi("/IF_SendClaimToPWS", any()) } throws
            RuntimeException("connection reset")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isFalse()
        assertThat(response.resultMsg).isEqualTo("connection reset")
    }
}
