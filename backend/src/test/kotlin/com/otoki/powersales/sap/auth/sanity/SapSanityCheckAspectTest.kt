package com.otoki.powersales.sap.auth.sanity

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.exception.SapSanityCheckFailedException
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@DisplayName("SapSanityCheckAspect 테스트")
class SapSanityCheckAspectTest {

    private val auditService: SapInboundAuditService = mock()
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

        whenever(auditService.record(any())).thenAnswer { it.getArgument<SapInboundAudit>(0) }
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
    }

    private fun joinPointWith(args: Array<Any?>, threshold: Int = 20): ProceedingJoinPoint {
        val joinPoint: ProceedingJoinPoint = mock()
        val signature: MethodSignature = mock()
        val method = SampleHandler::class.java.getDeclaredMethod("handle", java.util.List::class.java)
        whenever(signature.method).thenReturn(method)
        whenever(signature.parameterNames).thenReturn(arrayOf("records"))
        whenever(joinPoint.signature).thenReturn(signature)
        whenever(joinPoint.args).thenReturn(args)

        // method annotation 의 threshold 를 변경할 수 없으므로, 별도 메서드로 차수 분기
        if (threshold != 20) {
            val altMethod = SampleHandler::class.java.getDeclaredMethod("handleStrict", java.util.List::class.java)
            whenever(signature.method).thenReturn(altMethod)
        }
        whenever(joinPoint.proceed()).thenReturn(Unit)
        return joinPoint
    }

    @Nested
    @DisplayName("Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("첫 호출 - previous 없음 + received 100 -> REQUEST_ACCEPTED 적재")
        fun firstCall_pass() {
            whenever(auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)))
                .thenReturn(null)
            val jp = joinPointWith(arrayOf(List(100) { it }))

            aspect.around(jp)

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(captor.capture())
            assertThat(captor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(captor.firstValue.receivedCount).isEqualTo(100)
            assertThat(captor.firstValue.previousCount).isNull()
        }

        @Test
        @DisplayName("정상 변동 - previous 100 + received 110 -> 통과")
        fun normalDelta_pass() {
            whenever(auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)))
                .thenReturn(100)
            val jp = joinPointWith(arrayOf(List(110) { it }))

            aspect.around(jp)

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(captor.capture())
            assertThat(captor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(captor.firstValue.receivedCount).isEqualTo(110)
            assertThat(captor.firstValue.previousCount).isEqualTo(100)
        }

        @Test
        @DisplayName("경계 정확 20% - previous 100 + received 120 -> 통과 (> 비교)")
        fun exactlyAtThreshold_pass() {
            whenever(auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)))
                .thenReturn(100)
            val jp = joinPointWith(arrayOf(List(120) { it }))

            aspect.around(jp)

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(captor.capture())
            assertThat(captor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
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

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(captor.capture())
            assertThat(captor.firstValue.eventType)
                .isEqualTo(SapInboundAuditEventType.REQUEST_REJECTED_SANITY)
            assertThat(captor.firstValue.receivedCount).isEqualTo(0)
        }

        @Test
        @DisplayName("임계 초과 - previous 100 + received 50 (50% 감소) -> SANITY_CHECK_FAILED")
        fun overThreshold_reject() {
            whenever(auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)))
                .thenReturn(100)
            val jp = joinPointWith(arrayOf(List(50) { it }))

            assertThatThrownBy { aspect.around(jp) }
                .isInstanceOf(SapSanityCheckFailedException::class.java)
        }

        @Test
        @DisplayName("경계 직후 21% - previous 100 + received 121 -> SANITY_CHECK_FAILED")
        fun justOverBoundary_reject() {
            whenever(auditService.findLatestAcceptedCount(eq(endpoint), eq(clientId)))
                .thenReturn(100)
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
