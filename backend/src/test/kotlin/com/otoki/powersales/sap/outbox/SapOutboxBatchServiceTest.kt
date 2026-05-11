package com.otoki.powersales.sap.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageRequest
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SapOutboxBatchService 테스트 (#592 / #692)")
class SapOutboxBatchServiceTest {

    @Mock private lateinit var outboxRepository: SapOutboxRepository
    @Mock private lateinit var interfaceRegistry: SapInterfaceRegistry
    @Mock private lateinit var statusHandlerRegistry: SapOutboxStatusHandlerRegistry
    @Mock private lateinit var restClient: RestClient
    @Mock private lateinit var statusHandler: SapOutboxStatusHandler

    private val objectMapper = ObjectMapper()
    private lateinit var service: SapOutboxBatchService

    @BeforeEach
    fun setUp() {
        service = SapOutboxBatchService(
            outboxRepository = outboxRepository,
            interfaceRegistry = interfaceRegistry,
            statusHandlerRegistry = statusHandlerRegistry,
            restClient = restClient,
            objectMapper = objectMapper,
            batchSize = 100,
        )
    }

    @Test
    @DisplayName("interfaceId 미등록 → FAILED 마킹 + 핸들러 호출")
    fun unregisteredInterfaceId() {
        val outbox = SapOutbox(
            id = 1L,
            domainType = "ORDER_REQUEST_REGISTER",
            aggregateId = 100L,
            interfaceId = "UNKNOWN_IF",
            payload = "{}",
        )
        whenever(outboxRepository.findPendingOrRetry(any<PageRequest>())).thenReturn(listOf(outbox))
        whenever(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox))
        whenever(interfaceRegistry.resolveEndpoint("UNKNOWN_IF")).thenReturn(null)
        whenever(statusHandlerRegistry.resolve("ORDER_REQUEST_REGISTER")).thenReturn(statusHandler)

        val result = service.execute()

        assertThat(result.picked).isEqualTo(1)
        assertThat(result.failed).isEqualTo(1)
        assertThat(outbox.status).isEqualTo(SapOutbox.STATUS_FAILED)
        assertThat(outbox.lastError).isEqualTo("UNREGISTERED_INTERFACE_ID")
        verify(outboxRepository).save(outbox)
    }

    @Test
    @DisplayName("이미 SENT 상태 → 처리 건너뜀")
    fun alreadySent() {
        val outbox = SapOutbox(
            id = 1L,
            domainType = "ORDER_REQUEST_REGISTER",
            aggregateId = 100L,
            interfaceId = "X",
            payload = "{}",
            status = SapOutbox.STATUS_SENT,
        )
        whenever(outboxRepository.findPendingOrRetry(any<PageRequest>())).thenReturn(listOf(outbox))
        whenever(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox))

        val result = service.execute()

        assertThat(result.picked).isEqualTo(1)
        assertThat(result.failed).isEqualTo(1)
        verify(outboxRepository, never()).save(any())
    }

    @Test
    @DisplayName("폴링 결과 없음 → 워커 무동작")
    fun noPendingRows() {
        whenever(outboxRepository.findPendingOrRetry(any<PageRequest>())).thenReturn(emptyList())

        val result = service.execute()

        assertThat(result.picked).isEqualTo(0)
        assertThat(result.sent).isEqualTo(0)
        assertThat(result.failed).isEqualTo(0)
    }

    @Test
    @DisplayName("SapOutbox markSent / markRetry / markFailed 상태 전이")
    fun outboxStateMachine() {
        val outbox = SapOutbox(
            id = 1L, domainType = "X", aggregateId = 1L, interfaceId = "Y", payload = "{}",
        )

        outbox.markRetry("HTTP_500")
        assertThat(outbox.status).isEqualTo(SapOutbox.STATUS_RETRY)
        assertThat(outbox.retryCount).isEqualTo(1)
        assertThat(outbox.lastError).isEqualTo("HTTP_500")

        outbox.markFailed("max retry")
        assertThat(outbox.status).isEqualTo(SapOutbox.STATUS_FAILED)
        assertThat(outbox.retryCount).isEqualTo(2)

        outbox.markSent()
        assertThat(outbox.status).isEqualTo(SapOutbox.STATUS_SENT)
        assertThat(outbox.sentAt).isNotNull
        assertThat(outbox.lastError).isNull()
    }

    @Test
    @DisplayName("SapOutboxStatusHandlerRegistry — supports() 키로 lookup")
    fun handlerRegistry() {
        val h = object : SapOutboxStatusHandler {
            override fun supports(): String = "TEST_DOMAIN"
            override fun handle(outbox: SapOutbox, success: Boolean, resultMessage: String?) {}
        }
        val registry = SapOutboxStatusHandlerRegistry(listOf(h))
        assertThat(registry.resolve("TEST_DOMAIN")).isSameAs(h)
        assertThat(registry.resolve("UNKNOWN")).isNull()
    }

    @Test
    @DisplayName("SapInterfaceRegistry — register / resolveEndpoint")
    fun interfaceRegistryBehavior() {
        val registry = SapInterfaceRegistry()
        registry.register("IF_X", "/IF_X")
        assertThat(registry.resolveEndpoint("IF_X")).isEqualTo("/IF_X")
        assertThat(registry.isRegistered("IF_X")).isTrue
        assertThat(registry.resolveEndpoint("MISSING")).isNull()
    }

    @Test
    @Suppress("unused")
    @DisplayName("processOne — 호출자 역할 verify")
    fun processOneVerify() {
        val outbox = SapOutbox(
            id = 1L, domainType = "ORDER_REQUEST_REGISTER",
            aggregateId = 100L, interfaceId = "UNKNOWN", payload = "{}",
        )
        whenever(outboxRepository.findById(1L)).thenReturn(Optional.of(outbox))
        whenever(interfaceRegistry.resolveEndpoint("UNKNOWN")).thenReturn(null)
        whenever(statusHandlerRegistry.resolve("ORDER_REQUEST_REGISTER")).thenReturn(statusHandler)

        val ok = service.processOne(1L)

        assertThat(ok).isFalse
        val captor = argumentCaptor<SapOutbox>()
        verify(outboxRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(SapOutbox.STATUS_FAILED)
        verify(statusHandler, times(1)).handle(any(), any(), org.mockito.kotlin.anyOrNull())
    }
}
