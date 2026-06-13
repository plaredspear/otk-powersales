package com.otoki.powersales.external.sap.auth.sanity

import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.auth.exception.SapSanityCheckFailedException
import com.otoki.powersales.external.sap.auth.sanity.SapDestructiveEndpoint
import com.otoki.powersales.external.sap.auth.sanity.SapSanityCheckAspect
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

@DisplayName("SapSanityCheckAspect 테스트")
class SapSanityCheckAspectTest {

    private val auditService: SapInboundAuditService = mockk()
    private val aspect = SapSanityCheckAspect(auditService)

    private val endpoint = "/api/v1/sap/org-master"
    private val clientId = "otoki-sap-client"

    @BeforeEach
    fun setUp() {
        val request = MockHttpServletRequest("POST", endpoint).apply {
            remoteAddr = "10.0.0.1"
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

    private fun joinPointWith(args: Array<Any?>, threshold: Int = 20): ProceedingJoinPoint {
        val joinPoint: ProceedingJoinPoint = mockk()
        val signature: MethodSignature = mockk()
        val method = SampleHandler::class.java.getDeclaredMethod("handle", java.util.List::class.java)
        every { signature.method } returns method
        every { signature.parameterNames } returns arrayOf("records")
        every { joinPoint.signature } returns signature
        every { joinPoint.args } returns args

        // method annotation 의 threshold 를 변경할 수 없으므로, 별도 메서드로 차수 분기
        if (threshold != 20) {
            val altMethod = SampleHandler::class.java.getDeclaredMethod("handleStrict", java.util.List::class.java)
            every { signature.method } returns altMethod
        }
        every { joinPoint.proceed() } returns Unit
        return joinPoint
    }

    @Nested
    @DisplayName("Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("첫 호출 - previous 없음 + received 100 -> REQUEST_ACCEPTED 적재")
        fun firstCall_pass() {
            every { auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)) } returns null
            val jp = joinPointWith(arrayOf(List(100) { it }))

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(captor.captured.receivedCount).isEqualTo(100)
            assertThat(captor.captured.previousCount).isNull()
        }

        @Test
        @DisplayName("정상 변동 - previous 100 + received 110 -> 통과")
        fun normalDelta_pass() {
            every { auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)) } returns 100
            val jp = joinPointWith(arrayOf(List(110) { it }))

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(captor.captured.receivedCount).isEqualTo(110)
            assertThat(captor.captured.previousCount).isEqualTo(100)
        }

        @Test
        @DisplayName("경계 정확 20% - previous 100 + received 120 -> 통과 (> 비교)")
        fun exactlyAtThreshold_pass() {
            every { auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)) } returns 100
            val jp = joinPointWith(arrayOf(List(120) { it }))

            aspect.around(jp)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
        }
    }

    @Nested
    @DisplayName("Error Path")
    inner class ErrorPath {

        @Test
        @DisplayName("받은 건수 0 -> SANITY_CHECK_FAILED + REQUEST_REJECTED_SANITY 적재")
        fun zeroReceived_reject() {
            val jp = joinPointWith(arrayOf(emptyList<Int>()))

            assertThatThrownBy { aspect.around(jp) }
                .isInstanceOf(SapSanityCheckFailedException::class.java)

            val captor = slot<SapInboundAudit>()
            verify { auditService.record(capture(captor)) }
            assertThat(captor.captured.eventType)
                .isEqualTo(SapInboundAuditEventType.REQUEST_REJECTED_SANITY)
            assertThat(captor.captured.receivedCount).isEqualTo(0)
        }

        @Test
        @DisplayName("임계 초과 - previous 100 + received 50 (50% 감소) -> SANITY_CHECK_FAILED")
        fun overThreshold_reject() {
            every { auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)) } returns 100
            val jp = joinPointWith(arrayOf(List(50) { it }))

            assertThatThrownBy { aspect.around(jp) }
                .isInstanceOf(SapSanityCheckFailedException::class.java)
        }

        @Test
        @DisplayName("경계 직후 21% - previous 100 + received 121 -> SANITY_CHECK_FAILED")
        fun justOverBoundary_reject() {
            every { auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)) } returns 100
            val jp = joinPointWith(arrayOf(List(121) { it }))

            assertThatThrownBy { aspect.around(jp) }
                .isInstanceOf(SapSanityCheckFailedException::class.java)
        }
    }

    private class SampleHandler {
        @SapDestructiveEndpoint(threshold = 20)
        fun handle(records: List<Int>) = Unit

        @SapDestructiveEndpoint(threshold = 10)
        fun handleStrict(records: List<Int>) = Unit
    }
}
