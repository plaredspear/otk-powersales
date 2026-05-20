package com.otoki.powersales.sap.outbox

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.util.Optional

@DisplayName("SapOutboxBatchService 테스트 (#592 / #692)")
class SapOutboxBatchServiceTest {

    private val outboxRepository: SapOutboxRepository = mockk()
    private val interfaceRegistry: SapInterfaceRegistry = mockk()
    private val statusHandlerRegistry: SapOutboxStatusHandlerRegistry = mockk()
    private val restClient: RestClient = mockk()
    private val statusHandler: SapOutboxStatusHandler = mockk()

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
        every { outboxRepository.findPendingOrRetry(any<PageRequest>()) } returns listOf(outbox)
        every { outboxRepository.findById(1L) } returns Optional.of(outbox)
        every { interfaceRegistry.resolveEndpoint("UNKNOWN_IF") } returns null
        every { statusHandlerRegistry.resolve("ORDER_REQUEST_REGISTER") } returns statusHandler
        every { outboxRepository.save(any<SapOutbox>()) } returns outbox
        every { statusHandler.handle(any(), any(), any()) } returns Unit

        val result = service.execute()

        assertThat(result.picked).isEqualTo(1)
        assertThat(result.failed).isEqualTo(1)
        assertThat(outbox.status).isEqualTo(SapOutbox.STATUS_FAILED)
        assertThat(outbox.lastError).isEqualTo("UNREGISTERED_INTERFACE_ID")
        verify { outboxRepository.save(outbox) }
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
        every { outboxRepository.findPendingOrRetry(any<PageRequest>()) } returns listOf(outbox)
        every { outboxRepository.findById(1L) } returns Optional.of(outbox)

        val result = service.execute()

        assertThat(result.picked).isEqualTo(1)
        assertThat(result.failed).isEqualTo(1)
        verify(exactly = 0) { outboxRepository.save(any<SapOutbox>()) }
    }

    @Test
    @DisplayName("폴링 결과 없음 → 워커 무동작")
    fun noPendingRows() {
        every { outboxRepository.findPendingOrRetry(any<PageRequest>()) } returns emptyList()

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
        val captor = slot<SapOutbox>()
        every { outboxRepository.findById(1L) } returns Optional.of(outbox)
        every { interfaceRegistry.resolveEndpoint("UNKNOWN") } returns null
        every { statusHandlerRegistry.resolve("ORDER_REQUEST_REGISTER") } returns statusHandler
        every { outboxRepository.save(capture(captor)) } returns outbox
        every { statusHandler.handle(any(), any(), any()) } returns Unit

        val ok = service.processOne(1L)

        assertThat(ok).isFalse
        verify { outboxRepository.save(any<SapOutbox>()) }
        assertThat(captor.captured.status).isEqualTo(SapOutbox.STATUS_FAILED)
        verify(exactly = 1) { statusHandler.handle(any(), any(), any()) }
    }
}
