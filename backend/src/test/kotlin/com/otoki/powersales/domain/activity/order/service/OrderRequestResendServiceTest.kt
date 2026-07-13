package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderStatusException
import com.otoki.powersales.domain.activity.order.exception.OrderAlreadyClosedException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import com.otoki.powersales.external.sap.outbox.SapOutbox
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("OrderRequestResendService 테스트 (F18 재전송)")
class OrderRequestResendServiceTest {

    private lateinit var orderRequestRepository: OrderRequestRepository
    private lateinit var orderRequestProductRepository: OrderRequestProductRepository
    private lateinit var deadlineCalculator: OrderDeadlineCalculator
    private lateinit var registerSender: OrderRequestRegisterSender
    private lateinit var service: OrderRequestResendService

    private val userId = 1L
    private val orderRequestId = 100L

    @BeforeEach
    fun setUp() {
        orderRequestRepository = mockk()
        orderRequestProductRepository = mockk()
        deadlineCalculator = OrderDeadlineCalculator(
            Clock.fixed(
                LocalDateTime.of(2026, 5, 1, 10, 0).atZone(TimeZones.SEOUL_ZONE).toInstant(),
                TimeZones.SEOUL_ZONE,
            ),
        )
        registerSender = mockk()
        service = OrderRequestResendService(
            orderRequestRepository = orderRequestRepository,
            orderRequestProductRepository = orderRequestProductRepository,
            orderDeadlineCalculator = deadlineCalculator,
            orderRequestRegisterSender = registerSender,
        )
    }

    // ───────── HP1: 재전송 성공 ─────────
    @Test
    @DisplayName("HP1 — SEND_FAILED + 마감 전 → 상태 SENT 복귀 후 outbox 재적재")
    fun hp1_resendSuccess() {
        val orderRequest = orderRequest(status = OrderRequestStatus.SEND_FAILED)
        val lines = listOf(
            product(101, BigDecimal.valueOf(10L), "P001", orderRequest),
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
        )
        stubLoad(orderRequest, lines)
        val savedCaptor = slot<OrderRequest>()
        every { orderRequestRepository.save(capture(savedCaptor)) } answers { savedCaptor.captured }
        val enqueueLines = slot<List<OrderRequestProduct>>()
        every { registerSender.enqueue(any(), capture(enqueueLines)) } returns mockk<SapOutbox>()

        service.resend(orderRequestId, userId)

        assertThat(orderRequest.orderRequestStatus).isEqualTo(OrderRequestStatus.SENT)
        verify(exactly = 1) { orderRequestRepository.save(orderRequest) }
        verify(exactly = 1) { registerSender.enqueue(orderRequest, lines) }
        assertThat(enqueueLines.captured).hasSize(2)
    }

    // ───────── EP1: 본인 주문 아님 ─────────
    @Test
    @DisplayName("EP1 — 다른 사번 소유 → FORBIDDEN, 재적재 미호출")
    fun ep1_forbidden() {
        val other = Employee(id = 99L, employeeCode = "E999", name = "other")
        val orderRequest = orderRequest(status = OrderRequestStatus.SEND_FAILED, employee = other)
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.resend(orderRequestId, userId) }
            .isInstanceOf(ForbiddenOrderAccessException::class.java)
        verify(exactly = 0) { registerSender.enqueue(any(), any()) }
    }

    // ───────── EP2: 마감 후 ─────────
    @Test
    @DisplayName("EP2 — 마감 후 → ORDER_ALREADY_CLOSED, 재적재 미호출")
    fun ep2_alreadyClosed() {
        val orderRequest = orderRequest(
            status = OrderRequestStatus.SEND_FAILED,
            deliveryDate = LocalDate.of(2026, 5, 1),
        )
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.resend(orderRequestId, userId) }
            .isInstanceOf(OrderAlreadyClosedException::class.java)
        verify(exactly = 0) { registerSender.enqueue(any(), any()) }
    }

    // ───────── EP3: SEND_FAILED 아닌 상태 거부 ─────────
    @Test
    @DisplayName("EP3 — APPROVED 상태 → INVALID_ORDER_STATUS, 재적재 미호출")
    fun ep3_invalidStatusApproved() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.resend(orderRequestId, userId) }
            .isInstanceOf(InvalidOrderStatusException::class.java)
        verify(exactly = 0) { registerSender.enqueue(any(), any()) }
    }

    @Test
    @DisplayName("EP4 — SENT 상태 → INVALID_ORDER_STATUS (중복 재전송 차단)")
    fun ep4_invalidStatusSent() {
        val orderRequest = orderRequest(status = OrderRequestStatus.SENT)
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.resend(orderRequestId, userId) }
            .isInstanceOf(InvalidOrderStatusException::class.java)
        verify(exactly = 0) { registerSender.enqueue(any(), any()) }
    }

    // ───────── EP5: 미존재 주문 ─────────
    @Test
    @DisplayName("EP5 — 미존재 orderRequestId → ORDER_NOT_FOUND")
    fun ep5_orderNotFound() {
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.empty()

        assertThatThrownBy { service.resend(orderRequestId, userId) }
            .isInstanceOf(OrderNotFoundException::class.java)
        verify(exactly = 0) { registerSender.enqueue(any(), any()) }
    }

    // ───────── 헬퍼 ─────────

    private fun stubLoad(orderRequest: OrderRequest, lines: List<OrderRequestProduct>) {
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns lines
    }

    private fun orderRequest(
        status: OrderRequestStatus,
        deliveryDate: LocalDate = LocalDate.of(2026, 5, 4),
        employee: Employee = Employee(id = userId, employeeCode = "E001", name = "tester", role = AppAuthority.WOMAN),
    ) = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "OR00000001",
        orderDate = LocalDateTime.of(2026, 4, 28, 10, 0),
        deliveryDate = deliveryDate,
        totalAmount = BigDecimal("100000"),
        orderRequestStatus = status,
        employee = employee,
        account = Account(id = 1, name = "A1", externalKey = "EXT-1"),
    )

    private fun product(
        id: Long,
        lineNumber: BigDecimal,
        productCode: String,
        orderRequest: OrderRequest,
    ) = OrderRequestProduct(
        id = id,
        lineNumber = lineNumber,
        productCode = productCode,
        unit = "EA",
        orderRequest = orderRequest,
    )
}
