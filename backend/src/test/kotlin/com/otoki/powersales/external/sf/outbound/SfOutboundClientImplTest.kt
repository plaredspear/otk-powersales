package com.otoki.powersales.external.sf.outbound

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

/**
 * [SfOutboundClientImpl] 응답 status 판정 / 로그 노출 테스트.
 *
 * 회귀 방지 대상: SF Apex REST 는 조회 계열도 `201 CREATED` 로 응답하는데, 과거 가드가
 * `!= HttpStatus.OK` (200 단독 비교) 라 정상 배치가 매시간 WARN 으로 오인되고 응답 본문
 * 전문(거래처목표 6천여건)이 절단 없이 stdout 에 노출됐다.
 */
@DisplayName("SfOutboundClientImpl 테스트")
class SfOutboundClientImplTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var client: SfOutboundClientImpl
    private lateinit var logAppender: ListAppender<ILoggingEvent>
    private lateinit var logger: Logger

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val properties = SfOutboundProperties(apexBaseUrl = APEX_BASE_URL)
        val tokenManager = mockk<SfOAuthTokenManager>()
        every { tokenManager.getAccessToken() } returns "Bearer test-token"

        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        client = SfOutboundClientImpl(properties, tokenManager, builder.build(), objectMapper)

        logger = LoggerFactory.getLogger(SfOutboundClientImpl::class.java) as Logger
        logAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logAppender)
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(logAppender)
        logAppender.stop()
    }

    @Test
    @DisplayName("201 CREATED — 정상 처리되고 WARN 로그를 남기지 않는다")
    fun createdIsTreatedAsSuccess() {
        server.expect(ExpectedCount.once(), requestTo(APEX_BASE_URL + ENDPOINT))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(LIST_BODY)
            )

        val response = client.callApi(ENDPOINT, mapOf("MOD_DT" to "20260729"))

        assertThat(response.rawBody).isEqualTo(LIST_BODY)
        assertThat(warnMessages()).isEmpty()
        server.verify()
    }

    @Test
    @DisplayName("200 OK — 정상 처리되고 WARN 로그를 남기지 않는다")
    fun okIsTreatedAsSuccess() {
        server.expect(ExpectedCount.once(), requestTo(APEX_BASE_URL + ENDPOINT))
            .andRespond(withSuccess(LIST_BODY, MediaType.APPLICATION_JSON))

        val response = client.callApi(ENDPOINT, mapOf("MOD_DT" to "20260729"))

        assertThat(response.rawBody).isEqualTo(LIST_BODY)
        assertThat(warnMessages()).isEmpty()
        server.verify()
    }

    @Test
    @DisplayName("비-2xx — WARN 을 남기되 본문은 200자로 절단해 전문 노출을 막는다")
    fun nonSuccessLogsTruncatedBody() {
        val longBody = """{"Result":[${"{\"AccountCode\":\"1010179\"}," .repeat(50)}{}]}"""
        server.expect(ExpectedCount.once(), requestTo(APEX_BASE_URL + ENDPOINT))
            .andRespond(
                withStatus(HttpStatus.MULTIPLE_CHOICES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(longBody)
            )

        client.callApi(ENDPOINT, mapOf("MOD_DT" to "20260729"))

        val warns = warnMessages()
        assertThat(warns).hasSize(1)
        // 포맷 인자(bodyHead)가 200자로 절단되어 원본 전문이 로그에 실리지 않아야 한다.
        val bodyHead = logAppender.list.single { it.level == Level.WARN }.argumentArray.last() as String
        assertThat(longBody.length).isGreaterThan(200)
        assertThat(bodyHead).hasSize(200).isEqualTo(longBody.take(200))
        assertThat(warns.single()).contains("비정상 응답")
        server.verify()
    }

    private fun warnMessages(): List<String> =
        logAppender.list.filter { it.level == Level.WARN }.map { it.formattedMessage }

    private companion object {
        const val APEX_BASE_URL = "http://sf-mock/services/apexrest"
        const val ENDPOINT = "/IF_salesprogresssend"
        const val LIST_BODY = """{"Result":[{"AccountCode":"1010179","Name":"SPR-00007081"}]}"""
    }
}
