package com.otoki.powersales.order.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.util.TimeZones
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.enums.OrderRequestStatus
import com.otoki.powersales.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.order.exception.OrderCancelDeadlinePassedException
import com.otoki.powersales.order.exception.OrderCancelInvalidStatusException
import com.otoki.powersales.order.exception.OrderCancelLineNotFoundException
import com.otoki.powersales.order.exception.OrderCancelSapFailedException
import com.otoki.powersales.order.exception.OrderNotFoundException
import com.otoki.powersales.order.repository.OrderRequestProductRepository
import com.otoki.powersales.order.repository.OrderRequestRepository
import com.otoki.powersales.order.sap.OrderRequestCancelPayloadFactory
import com.otoki.powersales.order.util.OrderDeadlineCalculator
import com.otoki.powersales.sap.outbound.sender.OrderRequestCancelSender
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
        orderRequestRepository = mock()
        orderRequestProductRepository = mock()
        employeeRepository = mock()
        deadlineCalculator = OrderDeadlineCalculator(
            Clock.fixed(
                java.time.LocalDateTime.of(2026, 5, 1, 10, 0).atZone(TimeZones.SEOUL_ZONE).toInstant(),
                TimeZones.SEOUL_ZONE,
            ),
        )
        payloadFactory = OrderRequestCancelPayloadFactory()
        sender = mock()
        committer = mock()
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
        whenever(committer.commit(eq(orderRequestId), any(), eq(employeeCode)))
            .thenReturn(commitResult(orderRequest.copy(status = OrderRequestStatus.CANCELED), lines))

        val response = service.cancel(orderRequestId, userId, emptyList())

        verify(sender).send(any())
        val captor = argumentCaptor<List<Long>>()
        verify(committer).commit(eq(orderRequestId), captor.capture(), eq(employeeCode))
        assertThat(captor.firstValue).containsExactly(101L, 102L)
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
        whenever(committer.commit(eq(orderRequestId), any(), eq(employeeCode)))
            .thenReturn(commitResult(orderRequest, lines.take(2)))

        val response = service.cancel(orderRequestId, userId, listOf(101L, 102L))

        val captor = argumentCaptor<List<Long>>()
        verify(committer).commit(eq(orderRequestId), captor.capture(), eq(employeeCode))
        assertThat(captor.firstValue).containsExactly(101L, 102L)
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
        whenever(committer.commit(eq(orderRequestId), any(), eq(employeeCode)))
            .thenReturn(commitResult(orderRequest.copy(status = OrderRequestStatus.CANCELED), lines))

        val response = service.cancel(orderRequestId, userId, emptyList())

        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCELED)
        verify(sender).send(any())
    }

    // ───────── EP1: 본인 주문 아님 ─────────
    @Test
    @DisplayName("EP1 — 다른 사번 소유 → ORD_FORBIDDEN, SAP 미호출")
    fun ep1_forbidden() {
        val other = Employee(id = 99L, employeeCode = "E999", name = "other")
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED, employee = other)
        whenever(orderRequestRepository.findById(orderRequestId)).thenReturn(Optional.of(orderRequest))

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(ForbiddenOrderAccessException::class.java)
        verify(sender, never()).send(any())
    }

    // ───────── EP2: 마감 시각 초과 ─────────
    @Test
    @DisplayName("EP2 — 마감 시각 초과 → ORD_CANCEL_DEADLINE_PASSED, SAP 미호출")
    fun ep2_deadlinePassed() {
        // 납기일 = 2026-05-01 (오늘) → cancellable false (납기일 당일은 항상 false)
        val orderRequest = orderRequest(
            status = OrderRequestStatus.APPROVED,
            deliveryDate = LocalDate.of(2026, 5, 1),
        )
        whenever(orderRequestRepository.findById(orderRequestId)).thenReturn(Optional.of(orderRequest))

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelDeadlinePassedException::class.java)
        verify(sender, never()).send(any())
    }

    // ───────── EP3: DRAFT 거부 ─────────
    @Test
    @DisplayName("EP3 — DRAFT 상태 → ORD_CANCEL_INVALID_STATUS, SAP 미호출")
    fun ep3_invalidStatusDraft() {
        val orderRequest = orderRequest(status = OrderRequestStatus.DRAFT)
        whenever(orderRequestRepository.findById(orderRequestId)).thenReturn(Optional.of(orderRequest))

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelInvalidStatusException::class.java)
        verify(sender, never()).send(any())
    }

    // ───────── EP4: CANCELED (이미 취소) ─────────
    @Test
    @DisplayName("EP4 — 이미 CANCELED → ORD_CANCEL_INVALID_STATUS, SAP 미호출")
    fun ep4_invalidStatusCanceled() {
        val orderRequest = orderRequest(status = OrderRequestStatus.CANCELED)
        whenever(orderRequestRepository.findById(orderRequestId)).thenReturn(Optional.of(orderRequest))

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelInvalidStatusException::class.java)
        verify(sender, never()).send(any())
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
        verify(sender, never()).send(any())
    }

    // ───────── EP7: SAP 응답 'E' ─────────
    @Test
    @DisplayName("EP7 — SAP 응답 'E' → ORD_CANCEL_SAP_FAILED, 커미터 미호출")
    fun ep7_sapResultCodeE() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        doThrow(OrderCancelSapFailedException("SAP error")).whenever(sender).send(any())

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
        verify(committer, never()).commit(any(), any(), any())
    }

    // ───────── EP10: 존재하지 않는 orderRequestId ─────────
    @Test
    @DisplayName("EP10 — 미존재 orderRequestId → ORD_NOT_FOUND, SAP 미호출")
    fun ep10_orderNotFound() {
        whenever(orderRequestRepository.findById(orderRequestId)).thenReturn(Optional.empty())

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderNotFoundException::class.java)
        verify(sender, never()).send(any())
    }

    // ───────── 헬퍼 ─────────

    private fun stubLoad(orderRequest: OrderRequest, lines: List<OrderRequestProduct>) {
        whenever(orderRequestRepository.findById(orderRequestId)).thenReturn(Optional.of(orderRequest))
        whenever(orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId))
            .thenReturn(lines)
    }

    private fun stubEmployee() {
        val employee = Employee(id = userId, employeeCode = employeeCode, name = "tester", role = UserRole.WOMAN)
        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
    }

    private fun commitResult(
        orderRequest: OrderRequest,
        lines: List<OrderRequestProduct>,
    ): OrderCancelCommitter.CommitResult {
        // mutation 금지 — stub 평가 시점에 mutate 하면 service 의 resolveTargetLines 가 이미 cancelled 로 필터링됨
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
        employee: Employee = Employee(id = userId, employeeCode = employeeCode, name = "tester", role = UserRole.WOMAN),
    ) = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "ORD-20260504-000001",
        orderDate = java.time.LocalDateTime.of(2026, 4, 28, 10, 0),
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
        productName = "P-$productCode",
        unit = "EA",
        orderRequest = orderRequest,
    )
}
