package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelDeadlinePassedException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelInFlightException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelInvalidStatusException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelLineNotFoundException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelSapFailedException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.external.sap.outbox.SapOutboxRepository
import com.otoki.powersales.domain.activity.order.sap.OrderRequestCancelPayloadFactory
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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
    private lateinit var requestRecorder: OrderCancelRequestRecorder
    private lateinit var sapOutboxRepository: SapOutboxRepository
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
        requestRecorder = mockk(relaxed = true)
        sapOutboxRepository = mockk()
        // 기본: 등록 outbox in-flight 아님 (취소 허용). in-flight 게이트 테스트에서만 true 로 override.
        every {
            sapOutboxRepository.existsByDomainTypeAndAggregateIdAndStatusIn(any(), any(), any())
        } returns false
        service = OrderCancelService(
            orderRequestRepository = orderRequestRepository,
            orderRequestProductRepository = orderRequestProductRepository,
            employeeRepository = employeeRepository,
            orderCancelPolicy = OrderCancelPolicy(deadlineCalculator, sapOutboxRepository),
            orderRequestCancelPayloadFactory = payloadFactory,
            orderRequestCancelSender = sender,
            orderCancelRequestRecorder = requestRecorder,
        )
    }

    // ───────── HP1: 전체 취소 (빈 배열) ─────────
    @Test
    @DisplayName("HP1 — 전체 취소(빈 배열) → 모든 라인 SAP 송신, 헤더 상태 무변경(#845 로컬 확정 제거)")
    fun hp1_fullCancel() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, BigDecimal.valueOf(10L), "P001", orderRequest),
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
        )
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } returns Unit

        val response = service.cancel(orderRequestId, userId, emptyList())

        verify { sender.send(any()) }
        // #845 — 로컬 확정 없음: 헤더 상태 무변경(APPROVED 그대로), 응답은 요청 대상 라인 기준.
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED.name)
        assertThat(response.orderRequestStatusName).isEqualTo(OrderRequestStatus.APPROVED.clientDisplayName)
        assertThat(response.cancelledLines).hasSize(2)
        assertThat(response.cancelledLines.map { it.orderProductId }).containsExactly(101L, 102L)
        // 라인 로컬 확정 없음 — line_change_type 미변경.
        assertThat(lines.all { !it.isCancelled() }).isTrue()
    }

    // ───────── HP1b: 취소 흔적 기록이 SAP 호출 전에 수행 (#858) ─────────
    @Test
    @DisplayName("HP1b — 취소 요청 흔적 기록이 SAP 송신 전에 호출됨 (#858)")
    fun hp1b_recordCancelRequestedBeforeSap() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, BigDecimal.valueOf(10L), "P001", orderRequest),
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
        )
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } returns Unit

        service.cancel(orderRequestId, userId, emptyList())

        // 흔적 기록(recorder) → SAP 송신(sender) 순서 보장 — SAP 미반영 시에도 "취소요청" 표시 근거 선기록.
        verifyOrder {
            requestRecorder.recordCancelRequested(eq(orderRequestId), listOf(101L, 102L), eq(employeeCode))
            sender.send(any())
        }
    }

    // ───────── EP7b: SAP 실패 시에도 흔적은 기록됨 (#858) ─────────
    @Test
    @DisplayName("EP7b — SAP 실패(timeout 포함) 시에도 흔적 기록은 선행됨 (#858)")
    fun ep7b_recordEvenOnSapFailure() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } throws OrderCancelSapFailedException("SAP timeout")

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)

        // SAP 실패 전에 흔적 기록은 이미 별도 트랜잭션으로 커밋됨 → "취소요청" 표시 근거 유지, 재시도 가능.
        verify { requestRecorder.recordCancelRequested(eq(orderRequestId), listOf(101L), eq(employeeCode)) }
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

        val response = service.cancel(orderRequestId, userId, listOf(101L, 102L))

        // 요청 대상 라인 2개만 SAP 송신 + 응답. 헤더 APPROVED 유지.
        assertThat(response.cancelledLines.map { it.orderProductId }).containsExactly(101L, 102L)
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED.name)
        assertThat(response.orderRequestStatusName).isEqualTo(OrderRequestStatus.APPROVED.clientDisplayName)
        assertThat(response.cancelledLines).hasSize(2)
    }

    // ───────── HP3: SEND_FAILED → 취소 가능 ─────────
    @Test
    @DisplayName("HP3 — SEND_FAILED 상태에서 전체 취소 → SAP 송신, 헤더 상태 무변경(#845)")
    fun hp3_sendFailed() {
        val orderRequest = orderRequest(status = OrderRequestStatus.SEND_FAILED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } returns Unit

        val response = service.cancel(orderRequestId, userId, emptyList())

        // #845 — 헤더 상태 무변경(SEND_FAILED 그대로 반환).
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.SEND_FAILED.name)
        assertThat(response.orderRequestStatusName).isEqualTo(OrderRequestStatus.SEND_FAILED.clientDisplayName)
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
    @DisplayName("EP7 — SAP 응답 'E' → ORD_CANCEL_SAP_FAILED, 라인 로컬 확정 없음")
    fun ep7_sapResultCodeE() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        stubEmployee()
        every { sender.send(any()) } throws OrderCancelSapFailedException("SAP error")

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)
        // #845 — 실패든 성공이든 line_change_type/헤더 확정 없음.
        assertThat(orderRequest.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED)
        assertThat(lines.all { !it.isCancelled() }).isTrue()
    }

    // ───────── EP8: 등록 SAP 전송 진행중 (outbox in-flight) ─────────
    @Test
    @DisplayName("EP8 — 등록 outbox PENDING/RETRY → ORD_CANCEL_IN_FLIGHT, SAP 미호출")
    fun ep8_registerInFlight() {
        val orderRequest = orderRequest(status = OrderRequestStatus.SENT)
        every { orderRequestRepository.findById(orderRequestId) } returns Optional.of(orderRequest)
        every {
            sapOutboxRepository.existsByDomainTypeAndAggregateIdAndStatusIn(any(), any(), any())
        } returns true

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelInFlightException::class.java)
        verify(exactly = 0) { sender.send(any()) }
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

    private fun orderRequest(
        status: OrderRequestStatus,
        deliveryDate: LocalDate = LocalDate.of(2026, 5, 4),
        employee: Employee = Employee(id = userId, employeeCode = employeeCode, name = "tester", role = AppAuthority.WOMAN),
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
