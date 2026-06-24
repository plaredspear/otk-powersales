package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.request.AdminClaimMasterSyncTestRequest
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
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
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.Optional

@DisplayName("AdminClaimMasterSyncTestService (SF IF_SendClaimToPWS 조회 + pwrskey 매칭 갱신) 테스트")
class AdminClaimMasterSyncTestServiceTest {

    private lateinit var sfOutboundClient: SfOutboundClient
    private lateinit var claimRepository: ClaimRepository
    private lateinit var txTemplate: TransactionTemplate
    private lateinit var service: AdminClaimMasterSyncTestService

    @BeforeEach
    fun setUp() {
        sfOutboundClient = mockk()
        claimRepository = mockk(relaxUnitFun = true)
        txTemplate = mockk()
        // txTemplate.execute { ... } 가 람다를 실제 실행하도록 stub.
        every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
            firstArg<TransactionCallback<*>>().doInTransaction(mockk(relaxed = true))
        }
        service = AdminClaimMasterSyncTestService(sfOutboundClient, claimRepository, ObjectMapper(), txTemplate)
    }

    private fun request(modDt: String = "20260410") = AdminClaimMasterSyncTestRequest(modDt = modDt)

    private fun stubSf(rawBody: String) {
        every { sfOutboundClient.callApi("/IF_SendClaimToPWS", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = rawBody)
    }

    // --- 조회/전송 (기존) ---

    @Test
    @DisplayName("MOD_DT 를 /IF_SendClaimToPWS 로 POST — 응답 rawBody 그대로 노출")
    fun test_callsSfWithModDt() {
        val rawList = """[{"pwrskey":"1","ProoductCode":"P-1","Status":"접수"}]"""
        every { claimRepository.findById(1L) } returns Optional.of(Claim(id = 1L))
        stubSf(rawList)

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

    // --- pwrskey 매칭 갱신 (신규) ---

    @Test
    @DisplayName("pwrskey 매칭 성공 — 조치/상담 6필드를 신규 데이터로 갱신")
    fun updatesMatchedClaim() {
        val claim = Claim(id = 42L)
        every { claimRepository.findById(42L) } returns Optional.of(claim)
        stubSf(
            """
            [
              {
                "pwrskey": "42",
                "ActionStatus": "처리완료",
                "ActionCode": "AC01",
                "counselNumber": "CS-100",
                "ReasonType": "포장불량",
                "ActContent": "교환 처리함",
                "CosmosKey": "COS-9"
              }
            ]
            """.trimIndent()
        )

        val response = service.test(userId = 1L, request = request())

        assertThat(response.fetchedCount).isEqualTo(1)
        assertThat(response.updatedCount).isEqualTo(1)
        assertThat(response.notFoundCount).isEqualTo(0)
        assertThat(response.skippedCount).isEqualTo(0)
        // 6필드 갱신 검증
        assertThat(claim.actionStatus).isEqualTo("처리완료")
        assertThat(claim.actionCode).isEqualTo("AC01")
        assertThat(claim.counselNumber).isEqualTo("CS-100")
        assertThat(claim.reasonType).isEqualTo("포장불량")
        assertThat(claim.actContent).isEqualTo("교환 처리함")
        assertThat(claim.cosmosKey).isEqualTo("COS-9")
    }

    @Test
    @DisplayName("등록 확정 필드(불만내역 등)는 응답에 와도 갱신하지 않는다")
    fun doesNotTouchNonInboundFields() {
        val claim = Claim(id = 42L, defectDescription = "원래 불만내역")
        every { claimRepository.findById(42L) } returns Optional.of(claim)
        stubSf("""[{ "pwrskey": "42", "ActionStatus": "처리완료", "Description": "덮어쓰면 안됨", "Quantity": "999" }]""")

        service.test(userId = 1L, request = request())

        assertThat(claim.actionStatus).isEqualTo("처리완료")
        // defectDescription(불만내역) 은 갱신 대상이 아니므로 원본 유지
        assertThat(claim.defectDescription).isEqualTo("원래 불만내역")
    }

    @Test
    @DisplayName("pwrskey 가 가리키는 claim 이 없으면 notFound 집계, 갱신 없음")
    fun countsNotFound() {
        every { claimRepository.findById(999L) } returns Optional.empty()
        stubSf("""[{ "pwrskey": "999", "ActionStatus": "처리완료" }]""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.fetchedCount).isEqualTo(1)
        assertThat(response.updatedCount).isEqualTo(0)
        assertThat(response.notFoundCount).isEqualTo(1)
    }

    @Test
    @DisplayName("pwrskey 가 비었거나 숫자가 아니면 skipped 집계")
    fun countsSkippedForInvalidPwrskey() {
        stubSf(
            """[
              { "pwrskey": "", "ActionStatus": "A" },
              { "pwrskey": "abc", "ActionStatus": "B" },
              { "ActionStatus": "C" }
            ]"""
        )

        val response = service.test(userId = 1L, request = request())

        assertThat(response.fetchedCount).isEqualTo(3)
        assertThat(response.skippedCount).isEqualTo(3)
        assertThat(response.updatedCount).isEqualTo(0)
    }

    @Test
    @DisplayName("응답이 wrapper 객체(result 키)여도 배열을 추출해 갱신")
    fun extractsArrayFromWrapper() {
        val claim = Claim(id = 7L)
        every { claimRepository.findById(7L) } returns Optional.of(claim)
        stubSf("""{ "RESULT_CODE": "200", "result": [{ "pwrskey": "7", "ActionStatus": "처리완료" }] }""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.updatedCount).isEqualTo(1)
        assertThat(claim.actionStatus).isEqualTo("처리완료")
    }

    @Test
    @DisplayName("응답에서 배열을 못 찾으면 갱신 0 (성공 응답은 유지)")
    fun noArrayYieldsZeroUpdates() {
        stubSf("""{ "RESULT_CODE": "200", "RESULT_MSG": "SUCCESS" }""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isTrue()
        assertThat(response.fetchedCount).isEqualTo(0)
        assertThat(response.updatedCount).isEqualTo(0)
    }
}
