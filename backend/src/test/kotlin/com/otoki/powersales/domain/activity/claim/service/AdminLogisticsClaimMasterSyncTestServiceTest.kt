package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.request.AdminLogisticsClaimMasterSyncTestRequest
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
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

@DisplayName("AdminLogisticsClaimMasterSyncTestService (SF IF_SendLogisticsClaimToPWS 조회 + pwrskey 우선/name fallback 매칭 갱신) 테스트")
class AdminLogisticsClaimMasterSyncTestServiceTest {

    private lateinit var sfOutboundClient: SfOutboundClient
    private lateinit var suggestionRepository: SuggestionRepository
    private lateinit var txTemplate: TransactionTemplate
    private lateinit var service: AdminLogisticsClaimMasterSyncTestService

    @BeforeEach
    fun setUp() {
        sfOutboundClient = mockk()
        suggestionRepository = mockk(relaxUnitFun = true)
        txTemplate = mockk()
        every { txTemplate.execute(any<TransactionCallback<*>>()) } answers {
            firstArg<TransactionCallback<*>>().doInTransaction(mockk(relaxed = true))
        }
        service = AdminLogisticsClaimMasterSyncTestService(
            sfOutboundClient, suggestionRepository, ObjectMapper(), txTemplate,
        )
    }

    private fun request(modDt: String = "20260410") = AdminLogisticsClaimMasterSyncTestRequest(modDt = modDt)

    private fun stubSf(rawBody: String) {
        every { sfOutboundClient.callApi("/IF_SendLogisticsClaimToPWS", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "SUCCESS", rawBody = rawBody)
    }

    private fun suggestion(id: Long) = Suggestion(id = id, proposalNumber = "PM-$id")

    // --- 조회/전송 (기존) ---

    @Test
    @DisplayName("MOD_DT 를 /IF_SendLogisticsClaimToPWS 로 POST — 응답 rawBody 그대로 노출")
    fun test_callsSfWithModDt() {
        val rawList = """[{"pwrskey":"1","DKRetailProductId":"P-1","ActionStatus":"접수"}]"""
        every { suggestionRepository.findById(1L) } returns Optional.of(suggestion(1L))
        stubSf(rawList)

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isTrue()
        assertThat(response.resultCode).isEqualTo("200")
        assertThat(response.rawResponse).isEqualTo(rawList)
        verify(exactly = 1) { sfOutboundClient.callApi("/IF_SendLogisticsClaimToPWS", any()) }
    }

    @Test
    @DisplayName("apiMap — MOD_DT 키 하나만 담아 전송 + requestPayload 에 반영")
    fun test_apiMapContainsOnlyModDt() {
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/IF_SendLogisticsClaimToPWS", capture(apiMapSlot)) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "[]")

        val response = service.test(userId = 1L, request = request("20260101"))

        assertThat(apiMapSlot.captured).containsExactlyEntriesOf(mapOf("MOD_DT" to "20260101"))
        assertThat(response.requestPayload).contains("\"MOD_DT\":\"20260101\"")
    }

    @Test
    @DisplayName("SF OAuth 실패 — success=false, 예외 throw 안 함")
    fun test_oauthFailure() {
        every { sfOutboundClient.callApi("/IF_SendLogisticsClaimToPWS", any()) } throws
            SfOAuthFailedException("재발급 후에도 401")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isFalse()
        assertThat(response.resultMsg).contains("401")
        assertThat(response.rawResponse).isNull()
    }

    @Test
    @DisplayName("SF 호출 일반 예외 — success=false, 메시지 노출")
    fun test_genericException() {
        every { sfOutboundClient.callApi("/IF_SendLogisticsClaimToPWS", any()) } throws
            RuntimeException("connection reset")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.success).isFalse()
        assertThat(response.resultMsg).isEqualTo("connection reset")
    }

    // --- pwrskey 매칭 갱신 (신규) ---

    @Test
    @DisplayName("pwrskey 매칭 성공 — 조치 계열 6필드를 신규 데이터로 갱신 (ActionStatus enum 변환)")
    fun updatesMatchedSuggestion() {
        val s = suggestion(42L)
        every { suggestionRepository.findById(42L) } returns Optional.of(s)
        stubSf(
            """
            [
              {
                "pwrskey": "42",
                "ActionNum": "AN-1",
                "ActionStatus": "조치 완료",
                "ActionManager": "과장 홍길동",
                "LogisticsResponsibility": "물류 귀책",
                "ClaimTypeMeasures": "재포장",
                "ActionContent": "교체 발송 완료"
              }
            ]
            """.trimIndent()
        )

        val response = service.test(userId = 1L, request = request())

        assertThat(response.fetchedCount).isEqualTo(1)
        assertThat(response.updatedCount).isEqualTo(1)
        // 6필드 갱신 검증
        assertThat(s.actionNum).isEqualTo("AN-1")
        assertThat(s.actionStatus).isEqualTo(SuggestionActionStatus.COMPLETED) // "조치 완료" → enum
        assertThat(s.actionManager).isEqualTo("과장 홍길동")
        assertThat(s.logisticsResponsibility).isEqualTo("물류 귀책")
        assertThat(s.claimTypeMeasures).isEqualTo("재포장")
        assertThat(s.actionContent).isEqualTo("교체 발송 완료")
    }

    @Test
    @DisplayName("ActionStatus 가 미매칭 문자열이면 enum null (갱신은 진행)")
    fun unknownActionStatusYieldsNull() {
        val s = suggestion(42L).apply { actionStatus = SuggestionActionStatus.IN_PROGRESS }
        every { suggestionRepository.findById(42L) } returns Optional.of(s)
        stubSf("""[{ "pwrskey": "42", "ActionStatus": "알수없는상태" }]""")

        service.test(userId = 1L, request = request())

        assertThat(s.actionStatus).isNull()
    }

    @Test
    @DisplayName("등록 확정 필드(제목/상세내용)는 응답에 와도 갱신하지 않는다")
    fun doesNotTouchNonActionFields() {
        val s = suggestion(42L).apply { title = "원래 제목"; content = "원래 내용" }
        every { suggestionRepository.findById(42L) } returns Optional.of(s)
        stubSf("""[{ "pwrskey": "42", "ActionStatus": "조치중", "title": "덮어쓰면안됨", "description": "덮어쓰면안됨" }]""")

        service.test(userId = 1L, request = request())

        assertThat(s.actionStatus).isEqualTo(SuggestionActionStatus.IN_PROGRESS)
        assertThat(s.title).isEqualTo("원래 제목")
        assertThat(s.content).isEqualTo("원래 내용")
    }

    @Test
    @DisplayName("pwrskey 가 가리키는 제안이 없으면 notFound 집계")
    fun countsNotFound() {
        every { suggestionRepository.findById(999L) } returns Optional.empty()
        stubSf("""[{ "pwrskey": "999", "ActionStatus": "조치중" }]""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.fetchedCount).isEqualTo(1)
        assertThat(response.updatedCount).isEqualTo(0)
        assertThat(response.notFoundCount).isEqualTo(1)
    }

    @Test
    @DisplayName("pwrskey 도 name 도 없으면(또는 pwrskey 무효 + name 부재) skipped 집계")
    fun countsSkippedWhenNoMatchKey() {
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

    // --- name(제안번호) fallback 매칭 (신규) ---

    @Test
    @DisplayName("pwrskey 없고 name 만 있으면 name(제안번호) 으로 조회해 갱신")
    fun matchesByProposalNumberWhenNoPwrskey() {
        val s = suggestion(55L)
        every { suggestionRepository.findByProposalNumber("PM-2026-0001") } returns s
        stubSf("""[{ "Name": "PM-2026-0001", "ActionStatus": "조치중", "ActionNum": "AN-9" }]""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.updatedCount).isEqualTo(1)
        assertThat(response.skippedCount).isEqualTo(0)
        assertThat(response.notFoundCount).isEqualTo(0)
        assertThat(s.actionStatus).isEqualTo(SuggestionActionStatus.IN_PROGRESS)
        assertThat(s.actionNum).isEqualTo("AN-9")
        verify(exactly = 0) { suggestionRepository.findById(any()) }
        verify(exactly = 1) { suggestionRepository.findByProposalNumber("PM-2026-0001") }
    }

    @Test
    @DisplayName("pwrskey 와 name 둘 다 있으면 pwrskey(신규 PK) 를 우선 조회 — name 조회 안 함")
    fun prefersPwrskeyOverName() {
        val s = suggestion(88L)
        every { suggestionRepository.findById(88L) } returns Optional.of(s)
        stubSf("""[{ "pwrskey": "88", "Name": "PM-2026-0002", "ActionStatus": "조치중" }]""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.updatedCount).isEqualTo(1)
        assertThat(s.actionStatus).isEqualTo(SuggestionActionStatus.IN_PROGRESS)
        verify(exactly = 1) { suggestionRepository.findById(88L) }
        verify(exactly = 0) { suggestionRepository.findByProposalNumber(any()) }
    }

    @Test
    @DisplayName("pwrskey 무효(숫자 아님)여도 name 이 있으면 name 으로 fallback 조회")
    fun fallsBackToNameWhenPwrskeyInvalid() {
        val s = suggestion(77L)
        every { suggestionRepository.findByProposalNumber("PM-2026-0003") } returns s
        stubSf("""[{ "pwrskey": "not-a-number", "Name": "PM-2026-0003", "ActionStatus": "조치중" }]""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.updatedCount).isEqualTo(1)
        assertThat(response.skippedCount).isEqualTo(0)
        assertThat(s.actionStatus).isEqualTo(SuggestionActionStatus.IN_PROGRESS)
        verify(exactly = 1) { suggestionRepository.findByProposalNumber("PM-2026-0003") }
    }

    @Test
    @DisplayName("name 으로도 매칭 제안이 없으면 notFound 집계")
    fun countsNotFoundWhenNameUnmatched() {
        every { suggestionRepository.findByProposalNumber("PM-NONE") } returns null
        stubSf("""[{ "Name": "PM-NONE", "ActionStatus": "조치중" }]""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.notFoundCount).isEqualTo(1)
        assertThat(response.updatedCount).isEqualTo(0)
        assertThat(response.skippedCount).isEqualTo(0)
    }

    @Test
    @DisplayName("응답이 wrapper 객체(result 키)여도 배열을 추출해 갱신")
    fun extractsArrayFromWrapper() {
        val s = suggestion(7L)
        every { suggestionRepository.findById(7L) } returns Optional.of(s)
        stubSf("""{ "RESULT_CODE": "200", "result": [{ "pwrskey": "7", "ActionStatus": "조치중" }] }""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.updatedCount).isEqualTo(1)
        assertThat(s.actionStatus).isEqualTo(SuggestionActionStatus.IN_PROGRESS)
    }

    @Test
    @DisplayName("운영 SF 응답의 \"Result\" 래퍼(대문자 R)도 대소문자 무시로 추출해 갱신")
    fun extractsArrayFromResultWrapperCaseInsensitive() {
        val s = suggestion(7L)
        every { suggestionRepository.findById(7L) } returns Optional.of(s)
        stubSf("""{ "RESULT_CODE": "200", "Result": [{ "pwrskey": "7", "ActionStatus": "조치중" }] }""")

        val response = service.test(userId = 1L, request = request())

        assertThat(response.updatedCount).isEqualTo(1)
        assertThat(s.actionStatus).isEqualTo(SuggestionActionStatus.IN_PROGRESS)
    }
}
