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
    private lateinit var headerStatusUpdater: OrderCancelHeaderStatusUpdater
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
        headerStatusUpdater = mockk()
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
            orderCancelHeaderStatusUpdater = headerStatusUpdater,
        )
    }

    // ───────── HP1: 전체 취소 (빈 배열) ─────────
    @Test
    @DisplayName("HP1 — 전체 취소(빈 배열)+SAP성공 → 헤더 CANCEL_REQUESTED 전이, 라인 line_change_type 미변경")
    fun hp1_fullCancel() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, BigDecimal.valueOf(10L), "P001", orderRequest),
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
        )
        stubLoad(orderRequest, lines)
        stubEmployee()
        stubHeaderTransition(orderRequest)
        every { sender.send(any()) } returns Unit

        val response = service.cancel(orderRequestId, userId, emptyList())

        verify { sender.send(any()) }
        // 전량취소 커버 → 헤더 CANCEL_REQUESTED 전이. 응답은 요청 대상 라인 기준.
        verify { headerStatusUpdater.markCancelRequestedIfFullyCovered(orderRequestId) }
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED.name)
        assertThat(response.orderRequestStatusName).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED.clientDisplayName)
        assertThat(response.cancelledLines).hasSize(2)
        assertThat(response.cancelledLines.map { it.orderProductId }).containsExactly(101L, 102L)
        // 헤더 전이만 — 라인 line_change_type='X' 확정 없음(#845 비교 모델 유지).
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
        stubHeaderTransition(orderRequest)
        every { sender.send(any()) } returns Unit

        service.cancel(orderRequestId, userId, emptyList())

        // 흔적 기록(recorder) → SAP 송신(sender) 순서 보장 — SAP 미반영 시에도 "취소요청" 표시 근거 선기록.
        verifyOrder {
            requestRecorder.recordCancelRequested(eq(orderRequestId), listOf(101L, 102L), eq(employeeCode))
            sender.send(any())
        }
    }

    // ───────── EP7b: SAP 불확실 실패(timeout) 시 흔적 기록 + 유지 (#858) ─────────
    @Test
    @DisplayName("EP7b — SAP 불확실 실패(timeout, rejected=false) → 흔적 기록 선행 + 롤백 미호출(유지)")
    fun ep7b_recordEvenOnSapFailure() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        stubEmployee()
        // timeout = 결과 불확실(rejected=false 기본) → 흔적 유지.
        every { sender.send(any()) } throws OrderCancelSapFailedException("SAP timeout")

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)

        // SAP 실패 전에 흔적 기록은 이미 별도 트랜잭션으로 커밋됨 → "취소요청" 표시 근거 유지, 재시도 가능.
        verify { requestRecorder.recordCancelRequested(eq(orderRequestId), listOf(101L), eq(employeeCode)) }
        // 불확실 실패이므로 흔적 롤백은 호출되지 않음(실제 SAP 반영 가능성 대비).
        verify(exactly = 0) { requestRecorder.clearCancelRequested(any(), any()) }
    }

    // ───────── EP7c: SAP 명시적 거부(rejected=true) 시 흔적 롤백 (#858 보강) ─────────
    @Test
    @DisplayName("EP7c — SAP 명시적 거부(HTTP 200 + resultCode!='S', rejected=true) → 흔적 롤백")
    fun ep7c_clearOnExplicitReject() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, BigDecimal.valueOf(10L), "P001", orderRequest),
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
        )
        stubLoad(orderRequest, lines)
        stubEmployee()
        // HTTP 200 + resultCode != 'S' = SAP 확정 거부 → rejected=true.
        every { sender.send(any()) } throws OrderCancelSapFailedException("취소 불가", rejected = true)

        assertThatThrownBy { service.cancel(orderRequestId, userId, emptyList()) }
            .isInstanceOf(OrderCancelSapFailedException::class.java)

        // 흔적 기록 후 명시적 거부 → 동일 대상 라인 흔적 롤백으로 "취소요청중" 오표시 제거.
        verifyOrder {
            requestRecorder.recordCancelRequested(eq(orderRequestId), listOf(101L, 102L), eq(employeeCode))
            requestRecorder.clearCancelRequested(eq(orderRequestId), listOf(101L, 102L))
        }
        // 헤더 전이는 여전히 없음(실패 경로).
        verify(exactly = 0) { headerStatusUpdater.markCancelRequestedIfFullyCovered(any()) }
        assertThat(orderRequest.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED)
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
        // 커버 미달(3개 중 2개) → updater 는 헤더를 바꾸지 않고 그대로 반환.
        stubHeaderNoChange(orderRequest)
        every { sender.send(any()) } returns Unit

        val response = service.cancel(orderRequestId, userId, listOf(101L, 102L))

        // 요청 대상 라인 2개만 SAP 송신 + 응답. 헤더 APPROVED 유지.
        assertThat(response.cancelledLines.map { it.orderProductId }).containsExactly(101L, 102L)
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED.name)
        assertThat(response.orderRequestStatusName).isEqualTo(OrderRequestStatus.APPROVED.clientDisplayName)
        assertThat(response.cancelledLines).hasSize(2)
    }

    // ───────── HP2b: 누적 취소 (일부 이미 취소요청 + 나머지 취소 → 전량 커버) ─────────
    @Test
    @DisplayName("HP2b — 일부 라인이 이미 cancelRequested 인 상태에서 나머지 취소 → 전량 커버로 CANCEL_REQUESTED")
    fun hp2b_accumulatedFullCancel() {
        val orderRequest = orderRequest(status = OrderRequestStatus.APPROVED)
        // 101 은 앞선 취소요청으로 이미 흔적 보유, 102 를 이번에 취소 → updater 는 전량 커버로 판정.
        val already = product(101, BigDecimal.valueOf(10L), "P001", orderRequest)
            .also { it.markCancelRequested(employeeCode) }
        val lines = listOf(
            already,
            product(102, BigDecimal.valueOf(20L), "P002", orderRequest),
        )
        stubLoad(orderRequest, lines)
        stubEmployee()
        stubHeaderTransition(orderRequest)
        every { sender.send(any()) } returns Unit

        val response = service.cancel(orderRequestId, userId, listOf(102L))

        // 나머지 라인 취소로 전량 커버 → 헤더 CANCEL_REQUESTED 전이.
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED.name)
        assertThat(response.orderRequestStatusName).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED.clientDisplayName)
        // 이번 요청 대상 라인(102)만 응답에 포함.
        assertThat(response.cancelledLines.map { it.orderProductId }).containsExactly(102L)
    }

    // ───────── HP3: SEND_FAILED → 취소 가능 ─────────
    @Test
    @DisplayName("HP3 — SEND_FAILED 상태에서 전체 취소+SAP성공 → 헤더 CANCEL_REQUESTED 전이")
    fun hp3_sendFailed() {
        val orderRequest = orderRequest(status = OrderRequestStatus.SEND_FAILED)
        val lines = listOf(product(101, BigDecimal.valueOf(10L), "P001", orderRequest))
        stubLoad(orderRequest, lines)
        stubEmployee()
        stubHeaderTransition(orderRequest)
        every { sender.send(any()) } returns Unit

        val response = service.cancel(orderRequestId, userId, emptyList())

        // 전량취소 커버 → SEND_FAILED 에서도 CANCEL_REQUESTED 로 전이.
        assertThat(response.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED.name)
        assertThat(response.orderRequestStatusName).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED.clientDisplayName)
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

    // ───────── EP4: CANCEL_REQUESTED (이미 취소) ─────────
    @Test
    @DisplayName("EP4 — 이미 CANCEL_REQUESTED → ORD_CANCEL_INVALID_STATUS, SAP 미호출")
    fun ep4_invalidStatusCanceled() {
        val orderRequest = orderRequest(status = OrderRequestStatus.CANCEL_REQUESTED)
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
        // SAP 실패 경로 — 헤더 updater 미호출, 헤더 무변경(APPROVED), 라인 line_change_type 미변경.
        verify(exactly = 0) { headerStatusUpdater.markCancelRequestedIfFullyCovered(any()) }
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

    /** SAP 성공 후 헤더 updater 가 호출되지만 커버 미달로 헤더를 바꾸지 않는 경우(부분취소) 시뮬레이션. */
    private fun stubHeaderNoChange(orderRequest: OrderRequest) {
        every { headerStatusUpdater.markCancelRequestedIfFullyCovered(orderRequestId) } returns orderRequest
    }

    /** SAP 성공 후 헤더 updater 가 전량취소 커버로 CANCEL_REQUESTED 전이하는 경우 시뮬레이션. */
    private fun stubHeaderTransition(orderRequest: OrderRequest) {
        every { headerStatusUpdater.markCancelRequestedIfFullyCovered(orderRequestId) } answers {
            orderRequest.orderRequestStatus = OrderRequestStatus.CANCEL_REQUESTED
            orderRequest
        }
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
