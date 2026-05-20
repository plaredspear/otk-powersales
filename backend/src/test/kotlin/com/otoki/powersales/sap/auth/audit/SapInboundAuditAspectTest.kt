package com.otoki.powersales.sap.auth.audit

import com.otoki.powersales.sap.auth.exception.SapSanityCheckFailedException
import com.otoki.powersales.sap.inbound.dto.SapInboundChunkedResult
import com.otoki.powersales.sap.inbound.dto.SapInboundUpsertResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@DisplayName("SapInboundAuditAspect 테스트")
class SapInboundAuditAspectTest {

    private val auditService: SapInboundAuditService = mockk()
    private val aspect = SapInboundAuditAspect(auditService)

    private val endpoint = "/api/v1/sap/account"
    private val clientId = "otoki-sap-client"
    private val clientIp = "10.0.0.1"

    @BeforeEach
    fun setUp() {
        val request = MockHttpServletRequest("POST", endpoint).apply {
            remoteAddr = clientIp
        }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(clientId, null, emptyList())

        every { auditService.record(any()) } answers { firstArg<SapInboundAudit>() }
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
    }

    @Nested
    @DisplayName("Happy path - 정상 처리 + audit 1회 기록")
    inner class HappyPath {

        @Test
        @DisplayName("simple result - reason='success=N failure=M' + received=arg.size")
        fun simple_recordsAccepted() {
            val items = List(5) { it }
            val result = SimpleResult(successCount = 4, failureCount = 1)
            val jp = jpForSimple(items = items, result = result)

            val ret = aspect.around(jp)

            assertThat(ret).isSameAs(result)
            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            val audit = captor.captured
            assertThat(audit.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(audit.endpoint).isEqualTo(endpoint)
            assertThat(audit.httpMethod).isEqualTo("POST")
            assertThat(audit.clientId).isEqualTo(clientId)
            assertThat(audit.clientIp).isEqualTo(clientIp)
            assertThat(audit.receivedCount).isEqualTo(5)
            assertThat(audit.reason).isEqualTo("success=4 failure=1")
        }

        @Test
        @DisplayName("chunked result - reason='success=N failure=M chunks=C'")
        fun chunked_recordsAcceptedWithChunks() {
            val items = List(2500) { it }
            val result = ChunkedResult(successCount = 2400, failureCount = 100, chunkCount = 3)
            val jp = jpForChunked(items = items, result = result)

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            val audit = captor.captured
            assertThat(audit.receivedCount).isEqualTo(2500)
            assertThat(audit.reason).isEqualTo("success=2400 failure=100 chunks=3")
        }

        @Test
        @DisplayName("non-result return type - placeholder 0 으로 치환")
        fun nonResultReturn_placeholdersZero() {
            val items = List(2) { it }
            val jp = jpForSimple(items = items, result = "string return")

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.reason).isEqualTo("success=0 failure=0")
            assertThat(captor.captured.receivedCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Throw path - 실패 audit + rethrow")
    inner class ThrowPath {

        @Test
        @DisplayName("RuntimeException - reason='success=0 failure=<received>' + 예외 재전파")
        fun runtimeException_recordsFailureAndRethrows() {
            val items = List(7) { it }
            val ex = IllegalStateException("DB connection lost")
            val jp = jpForSimple(items = items, throwOnProceed = ex)

            assertThatThrownBy { aspect.around(jp) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("DB connection lost")

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            val audit = captor.captured
            assertThat(audit.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(audit.receivedCount).isEqualTo(7)
            assertThat(audit.reason).isEqualTo("success=0 failure=7")
        }

        @Test
        @DisplayName("chunked reasonTemplate + throw - chunks placeholder 0 으로 치환")
        fun chunkedThrow_chunksPlaceholderZero() {
            val items = List(3) { it }
            val ex = RuntimeException("payload too large")
            val jp = jpForChunked(items = items, throwOnProceed = ex)

            assertThatThrownBy { aspect.around(jp) }
                .isInstanceOf(RuntimeException::class.java)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.reason).isEqualTo("success=0 failure=3 chunks=0")
        }

        @Test
        @DisplayName("SapSanityCheckFailedException - audit 미기록 + 즉시 rethrow")
        fun sanityException_skipAudit() {
            val items = List(0) { it }
            val ex = SapSanityCheckFailedException("받은 건수가 0입니다")
            val jp = jpForSimple(items = items, throwOnProceed = ex)

            assertThatThrownBy { aspect.around(jp) }
                .isInstanceOf(SapSanityCheckFailedException::class.java)

            verify(exactly = 0) { auditService.record(any()) }
        }
    }

    @Nested
    @DisplayName("countArgName 처리")
    inner class CountArgName {

        @Test
        @DisplayName("countArgName 미일치 - received=0 으로 기록")
        fun unknownArgName_received0() {
            val items = List(9) { it }
            val result = SimpleResult(successCount = 9, failureCount = 0)
            val jp = jpForSimple(items = items, result = result, countArgName = "unknown")

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.receivedCount).isEqualTo(0)
        }

        @Test
        @DisplayName("countArgName 빈문자열 - 첫 Collection 인자 사용")
        fun blankArgName_firstCollection() {
            val items = List(3) { it }
            val result = SimpleResult(successCount = 3, failureCount = 0)
            val jp = jpForSimple(items = items, result = result, countArgName = "")

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.receivedCount).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("Request context 부재")
    inner class NoRequestContext {

        @Test
        @DisplayName("RequestContextHolder null - endpoint='' + clientIp=''")
        fun noRequestContext_emptyEndpoint() {
            RequestContextHolder.resetRequestAttributes()
            val items = List(2) { it }
            val result = SimpleResult(successCount = 2, failureCount = 0)
            val jp = jpForSimple(items = items, result = result)

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            val audit = captor.captured
            assertThat(audit.endpoint).isEmpty()
            assertThat(audit.httpMethod).isNull()
            assertThat(audit.clientIp).isEmpty()
        }

        @Test
        @DisplayName("Authentication null - clientId=null")
        fun noAuthentication_nullClientId() {
            SecurityContextHolder.clearContext()
            val items = List(1) { it }
            val result = SimpleResult(successCount = 1, failureCount = 0)
            val jp = jpForSimple(items = items, result = result)

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.clientId).isNull()
        }
    }

    @Nested
    @DisplayName("reasonTemplate 치환")
    inner class ReasonTemplate {

        @Test
        @DisplayName("커스텀 placeholder 순서 - 모두 치환")
        fun customTemplate_allPlaceholdersReplaced() {
            val items = List(10) { it }
            val result = ChunkedResult(successCount = 7, failureCount = 3, chunkCount = 2)
            val jp = jpForChunked(
                items = items,
                result = result,
                reasonTemplate = "chunks={chunks} | failure={failure} | success={success}"
            )

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.reason).isEqualTo("chunks=2 | failure=3 | success=7")
        }
    }

    private fun jpForSimple(
        items: List<Int>,
        result: Any? = SimpleResult(0, 0),
        throwOnProceed: Throwable? = null,
        countArgName: String = "items"
    ): ProceedingJoinPoint {
        val annotation = SimpleHandler::class.java.getDeclaredMethod("handle", java.util.List::class.java)
            .let {
                if (countArgName == "items") it
                else if (countArgName.isBlank()) SimpleHandler::class.java.getDeclaredMethod("handleNoName", java.util.List::class.java)
                else SimpleHandler::class.java.getDeclaredMethod("handleUnknown", java.util.List::class.java)
            }
        return buildJoinPoint(annotation, args = arrayOf(items), result = result, throwOnProceed = throwOnProceed)
    }

    private fun jpForChunked(
        items: List<Int>,
        result: Any? = ChunkedResult(0, 0, 0),
        throwOnProceed: Throwable? = null,
        reasonTemplate: String = "success={success} failure={failure} chunks={chunks}"
    ): ProceedingJoinPoint {
        val method = if (reasonTemplate == "success={success} failure={failure} chunks={chunks}") {
            ChunkedHandler::class.java.getDeclaredMethod("handle", java.util.List::class.java)
        } else {
            ChunkedHandler::class.java.getDeclaredMethod("handleCustom", java.util.List::class.java)
        }
        return buildJoinPoint(method, args = arrayOf(items), result = result, throwOnProceed = throwOnProceed)
    }

    private fun buildJoinPoint(
        method: java.lang.reflect.Method,
        args: Array<Any?>,
        result: Any?,
        throwOnProceed: Throwable?
    ): ProceedingJoinPoint {
        val joinPoint: ProceedingJoinPoint = mockk()
        val signature: MethodSignature = mockk()
        every { signature.method } returns method
        every { signature.parameterNames } returns arrayOf("items")
        every { joinPoint.signature } returns signature
        every { joinPoint.args } returns args
        if (throwOnProceed != null) {
            every { joinPoint.proceed() } throws throwOnProceed
        } else {
            every { joinPoint.proceed() } returns result
        }
        return joinPoint
    }

    private data class SimpleResult(
        override val successCount: Int,
        override val failureCount: Int
    ) : SapInboundUpsertResult

    private data class ChunkedResult(
        override val successCount: Int,
        override val failureCount: Int,
        override val chunkCount: Int
    ) : SapInboundChunkedResult

    private class SimpleHandler {
        @SapInboundAccepted("items")
        fun handle(items: List<Int>) = Unit

        @SapInboundAccepted("")
        fun handleNoName(items: List<Int>) = Unit

        @SapInboundAccepted("unknown")
        fun handleUnknown(items: List<Int>) = Unit
    }

    private class ChunkedHandler {
        @SapInboundAccepted("items", reasonTemplate = "success={success} failure={failure} chunks={chunks}")
        fun handle(items: List<Int>) = Unit

        @SapInboundAccepted("items", reasonTemplate = "chunks={chunks} | failure={failure} | success={success}")
        fun handleCustom(items: List<Int>) = Unit
    }
}
