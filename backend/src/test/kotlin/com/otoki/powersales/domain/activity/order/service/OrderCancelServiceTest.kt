package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.common.util.TimeZones
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelDeadlinePassedException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelInvalidStatusException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelLineNotFoundException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelSapFailedException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.OrderRequestCancelPayloadFactory
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender
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

@DisplayName("OrderCancelService 테스트 (#597)")
class OrderCancelServiceTest {

    private lateinit var orderRequestRepository: OrderRequestRepository
    private lateinit var orderRequestProductRepository: OrderRequestProductRepository
    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var deadlineCalculator: OrderDeadlineCalculator
    private lateinit var payloadFactory: OrderRequestCancelPayloadFactory
    private lateinit var sender: OrderRequestCancelSender
    private lateinit var committer: OrderCancelCommitter
    private lateinit var service: OrderCancelService

    private val userId = 1L
    private val orderRequestId = 100L
    private val employeeCode = "E001"

    @BeforeEach
    fun setUp() {
        orderRequestRepository = mockk()
        orderRequestProductRepository = mockk()
        employeeRepository = mockk()
        deadlineCalculator = OrderDeadlineCalculator(
            Clock.fixed(
                LocalDateTime.of(2026, 5, 1, 10, 0).atZone(TimeZones.SEOUL_ZONE).toInstant(),
                TimeZones.SEOUL_ZONE,
            ),
        )
        payloadFactory = OrderRequestCancelPayloadFactory()
        sender = mockk()
        committer = mockk()
        service = OrderCancelService(
            orderRequestRepository = orderRequestRepository,
            orderRequestProductRepository = orderRequestProductRepository,
            employeeRepository = employeeRepository,
            orderDeadlineCalculator = deadlineCalculator,
            orderRequestCancelPayloadFactory = payloadFactory,
            orderRequestCancelSender = sender,
            orderCancelCommitter = committer,
        )
    }

    // ───────── HP1: 전체 취소 (빈 배열) ─────────
    @Test
    @DisplayName("HP1 — 전체 취소(빈 배열) → 모든 라인 SAP 송신, 헤더 CANCELED")
    fun hp1_fullCancel() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, BigDecimal.valueOf(10L), "P001", orderRequest),
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
        )
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } returns Unit
        val captor = slot<List<Long>>()
        every { committer.commit(eq(orderRequestId), capture(captor), eq(employeeCode)) } returns
            commitResult(orderRequest.copy(status = OrderRequestStatus.CANCELED), lines)

        val response = service.cancel(orderRequestId, userId, emptyList())

        verify { sender.send(any()) }
        assertThat(captor.captured).containsExactly(101L, 102L)
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCELED)
        assertThat(response.cancelledLines).hasSize(2)
    }

    // ───────── HP2: 부분 취소 (3개 중 2개 라인) ─────────
    @Test
    @DisplayName("HP2 — 부분 취소(2/3) → 헤더 APPROVED 유지, 송신 라인 2개")
    fun hp2_partialCancel() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, BigDecimal.valueOf(10L), "P001", orderRequest),
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
            product(103, BigDecimal.valueOf(30L), "P003", orderRequest),
        )
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } returns Unit
        val captor = slot<List<Long>>()
        every { committer.commit(eq(orderRequestId), capture(captor), eq(employeeCode)) } returns
            commitResult(orderRequest, lines.take(2))

        val response = service.cancel(orderRequestId, userId, listOf(101L, 102L))

        assertThat(captor.captured).containsExactly(101L, 102L)
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED)
        assertThat(response.cancelledLines).hasSize(2)
    }

    // ───────── HP3: SEND_FAILED → 취소 가능 ─────────
    @Test
    @DisplayName("HP3 — SEND_FAILED 상태에서 전체 취소 → 헤더 CANCELED")
    fun hp3_sendFailed() {
        val orderRequest = orderRequest(status = OrderRequestStatus.SEND_FAILED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } returns Unit
        every { committer.commit(eq(orderRequestId), any(), eq(employeeCode)) } returns
            commitResult(orderRequest.copy(status = OrderRequestStatus.CANCELED), lines)

        val response = service.cancel(orderRequestId, userId, emptyList())

        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCELED)
        verify { sender.send(any()) }
    }

    // ───────── EP1: 본인 주문 아님 ─────────
    @Test
    @DisplayName("EP1 — 다른 사번 소유 → ORD_FORBIDDEN, SAP 미호출")
    fun ep1_forbidden() {
        val other = Employee(id = 99L, employeeCode = "E999", name = "other")
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED, employee = other)
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(ForbiddenOrderAccessException::class.java)
        verify(exactly = 0) { sender.send(any()) }
    }

    // ───────── EP2: 마감 시각 초과 ─────────
    @Test
    @DisplayName("EP2 — 마감 시각 초과 → ORD_CANCEL_DEADLINE_PASSED, SAP 미호출")
    fun ep2_deadlinePassed() {
        val orderRequest = orderRequest(
            status = OrderRequestStatus.APPROVED,
            deliveryDate = LocalDate.of(2026, 5, 1),
        )
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelDeadlinePassedException::class.java)
        verify(exactly = 0) { sender.send(any()) }
    }

    // ───────── EP3: DRAFT 거부 ─────────
    @Test
    @DisplayName("EP3 — DRAFT 상태 → ORD_CANCEL_INVALID_STATUS, SAP 미호출")
    fun ep3_invalidStatusDraft() {
        val orderRequest = orderRequest(status = OrderRequestStatus.DRAFT)
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelInvalidStatusException::class.java)
        verify(exactly = 0) { sender.send(any()) }
    }

    // ───────── EP4: CANCELED (이미 취소) ─────────
    @Test
    @DisplayName("EP4 — 이미 CANCELED → ORD_CANCEL_INVALID_STATUS, SAP 미호출")
    fun ep4_invalidStatusCanceled() {
        val orderRequest = orderRequest(status = OrderRequestStatus.CANCELED)
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelInvalidStatusException::class.java)
        verify(exactly = 0) { sender.send(any()) }
    }

    // ───────── EP5: 다른 주문의 라인 ID 포함 ─────────
    @Test
    @DisplayName("EP5/EP6 — 미존재 라인 PK → ORD_CANCEL_LINE_NOT_FOUND, SAP 미호출")
    fun ep5_lineNotInOrder() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)

        assertThatThrownBy { service.cancel(orderRequestId, userId, listOf(101L, 999L)) }
            .isInstanceOf(OrderCancelLineNotFoundException::class.java)
        verify(exactly = 0) { sender.send(any()) }
    }

    // ───────── EP7: SAP 응답 'E' ─────────
    @Test
    @DisplayName("EP7 — SAP 응답 'E' → ORD_CANCEL_SAP_FAILED, 커미터 미호출")
    fun ep7_sapResultCodeE() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } throws OrderCancelSapFailedException("SAP error")

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
        verify(exactly = 0) { committer.commit(any(), any(), any()) }
    }

    // ───────── EP10: 존재하지 않는 orderRequestId ─────────
    @Test
    @DisplayName("EP10 — 미존재 orderRequestId → ORD_NOT_FOUND, SAP 미호출")
    fun ep10_orderNotFound() {
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.empty()

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderNotFoundException::class.java)
        verify(exactly = 0) { sender.send(any()) }
    }

    // ───────── 헬퍼 ─────────

    private fun stubLoad(orderRequest: OrderRequest, lines: List<OrderRequestProduct>) {
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns lines
    }

    private fun stubEmployee() {
        val employee = Employee(id = userId, employeeCode = employeeCode, name = "tester", role = AppAuthority.WOMAN)
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
    }

    private fun commitResult(
        orderRequest: OrderRequest,
        lines: List<OrderRequestProduct>,
    ): OrderCancelCommitter.CommitResult {
        return OrderCancelCommitter.CommitResult(orderRequest, lines)
    }

    private fun OrderRequest.copy(status: OrderRequestStatus): OrderRequest {
        return OrderRequest(
            id = id,
            orderRequestNumber = orderRequestNumber,
            orderDate = orderDate,
            deliveryDate = deliveryDate,
            totalAmount = totalAmount,
            orderRequestStatus = status,
            employee = employee,
            account = account,
        )
    }

    private fun orderRequest(
        status: OrderRequestStatus,
        deliveryDate: LocalDate = LocalDate.of(2026, 5, 4),
        employee: Employee = Employee(id = userId, employeeCode = employeeCode, name = "tester", role = AppAuthority.WOMAN),
    ) = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "ORD-20260504-000001",
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
