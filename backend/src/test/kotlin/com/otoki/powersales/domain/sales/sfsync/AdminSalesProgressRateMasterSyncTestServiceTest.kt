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
    private lateinit var fetchClient: SalesProgressRateMasterFetchClient
    private lateinit var syncService: SalesProgressRateMasterSyncService
    private lateinit var service: AdminSalesProgressRateMasterSyncTestService

    @BeforeEach
    fun setUp() {
        sfOutboundClient = mockk()
        fetchClient = mockk()
        syncService = mockk()
        service = AdminSalesProgressRateMasterSyncTestService(
            sfOutboundClient, ObjectMapper(), fetchClient, syncService,
        )
    }

    private fun request(modDt: String = "20260410", save: Boolean = false) =
        AdminSalesProgressRateMasterSyncTestRequest(modDt = modDt, save = save)

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
    @DisplayName("save=false (기본) — DB 저장 경로 미호출 + syncResult null")
    fun test_saveFalse_noDbWrite() {
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "[]")

        val response = service.test(userId = 1L, request = request(save = false))

        assertThat(response.syncResult).isNull()
        verify(exactly = 0) { fetchClient.parse(any()) }
        verify(exactly = 0) { syncService.syncRecords(any(), any()) }
    }

    @Test
    @DisplayName("save=true — rawBody 를 parse 해 syncRecords 로 upsert + 통계 노출 (SF 재호출 없음)")
    fun test_saveTrue_upsertsAndReturnsSummary() {
        val rawList = """[{"AccountCode":"A-1","TargetYear":"2026","TargetMonth":"3"}]"""
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = rawList)
        val dtos = listOf(
            SalesProgressRateMasterFetchDto(
                sfid = null, name = null, externalKey = "20263A-1",
                targetYear = "2026", targetMonth = "3", accountCode = "A-1",
                rtTargetAmount = null, frTargetAmount = null, rmTargetAmount = null,
                foTargetAmount = null, targetSumAmount = null,
                currentMonthSalesAmount = null, previousMonthSalesAmount = null,
                businessRate = null, accountBranchView = null, accountBranchCode = null,
                isDeleted = false,
            ),
        )
        every { fetchClient.parse(rawList) } returns dtos
        every { syncService.syncRecords(dtos) } returns
            SalesProgressRateMasterSyncService.SyncResult(fetched = 1, inserted = 1, updated = 0, skipped = 0)

        val response = service.test(userId = 1L, request = request(save = true))

        assertThat(response.success).isTrue()
        assertThat(response.rawResponse).isEqualTo(rawList)
        assertThat(response.syncResult).isEqualTo(
            AdminSalesProgressRateMasterSyncTestResponse.SyncSummary(
                fetched = 1, inserted = 1, updated = 0, skipped = 0,
            ),
        )
        // 저장 경로도 SF 호출은 1회분을 공유 — 재호출 없음.
        verify(exactly = 1) { sfOutboundClient.callApi("/IF_salesprogresssend", any()) }
        verify(exactly = 1) { syncService.syncRecords(dtos) }
    }

    @Test
    @DisplayName("save=true + SF 응답 실패 (RESULT_CODE != 200) — DB 저장 안 함 + syncResult null")
    fun test_saveTrue_sfFailure_noDbWrite() {
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } returns
            SfApiResponse(resultCode = "0", resultMsg = "응답 본문 비어있음", rawBody = "")

        val response = service.test(userId = 1L, request = request(save = true))

        assertThat(response.success).isFalse()
        assertThat(response.syncResult).isNull()
        verify(exactly = 0) { syncService.syncRecords(any(), any()) }
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
