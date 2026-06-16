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

@DisplayName("SalesProgressRateMasterFetchClientImpl (SF IF_salesprogresssend fetch) 테스트")
class SalesProgressRateMasterFetchClientImplTest {

    private lateinit var sfOutboundClient: SfOutboundClient
    private lateinit var client: SalesProgressRateMasterFetchClientImpl

    @BeforeEach
    fun setUp() {
        sfOutboundClient = mockk()
        client = SalesProgressRateMasterFetchClientImpl(sfOutboundClient, ObjectMapper())
    }

    private fun stub(rawBody: String) {
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = rawBody)
    }

    @Test
    @DisplayName("MOD_DT 를 /IF_salesprogresssend 로 전송하고 최상위 배열 응답을 FetchDto 로 변환")
    fun parsesTopLevelArray() {
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/IF_salesprogresssend", capture(apiMapSlot)) } returns
            SfApiResponse(
                resultCode = "200",
                resultMsg = "OK",
                rawBody = """
                    [
                      {
                        "Name": "SPR-00000001",
                        "AccountCode": "1025008",
                        "TargetYear": "2026",
                        "TargetMonth": "3",
                        "BusinessRate": "60.5",
                        "FOTartgetAmount": "400",
                        "FRTargetAmount": "200",
                        "RMTartgetAmount": "300",
                        "RTTargetAmount": "100",
                        "TargetSumAmount": "1,000",
                        "ProgressRate": "12.5"
                      }
                    ]
                """.trimIndent(),
            )

        val result = client.fetch("20260410")

        assertThat(apiMapSlot.captured).containsExactlyEntriesOf(mapOf("MOD_DT" to "20260410"))
        assertThat(result).hasSize(1)
        val dto = result.single()
        assertThat(dto.name).isEqualTo("SPR-00000001")
        assertThat(dto.accountCode).isEqualTo("1025008")
        assertThat(dto.targetYear).isEqualTo("2026")
        assertThat(dto.targetMonth).isEqualTo("3")
        assertThat(dto.businessRate).isEqualTo(60.5)
        assertThat(dto.foTargetAmount).isEqualTo(400.0)
        assertThat(dto.rtTargetAmount).isEqualTo(100.0)
        // 천단위 콤마 제거 후 파싱.
        assertThat(dto.targetSumAmount).isEqualTo(1000.0)
    }

    @Test
    @DisplayName("data wrapper 로 감싼 배열 응답도 추출")
    fun parsesDataWrapper() {
        stub("""{"RESULT_CODE":"200","data":[{"AccountCode":"A-1","TargetYear":"2026","TargetMonth":"1"}]}""")

        val result = client.fetch("20260101")

        assertThat(result).hasSize(1)
        assertThat(result.single().accountCode).isEqualTo("A-1")
    }

    @Test
    @DisplayName("빈 배열 응답 — 빈 리스트")
    fun emptyArray() {
        stub("[]")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("배열을 찾을 수 없는 응답(RESULT 래퍼만) — 빈 리스트 (예외 없음)")
    fun noArrayFound() {
        stub("""{"RESULT_CODE":"200","RESULT_MSG":"OK"}""")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("빈 응답 본문 — 빈 리스트")
    fun blankBody() {
        stub("")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("깨진 JSON — 빈 리스트 (예외 흡수)")
    fun malformedJson() {
        stub("{not-json")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("SF 호출 예외(OAuth 실패 등) — 빈 리스트 (배치 비중단)")
    fun callFailureSwallowed() {
        every { sfOutboundClient.callApi("/IF_salesprogresssend", any()) } throws
            SfOAuthFailedException("재발급 후에도 401")

        assertThat(client.fetch("20260101")).isEmpty()
        verify(exactly = 1) { sfOutboundClient.callApi("/IF_salesprogresssend", any()) }
    }
}
