package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("OrderCancelHeaderStatusUpdater 테스트")
class OrderCancelHeaderStatusUpdaterTest {

    private lateinit var orderRequestRepository: OrderRequestRepository
    private lateinit var orderRequestProductRepository: OrderRequestProductRepository
    private lateinit var updater: OrderCancelHeaderStatusUpdater

    private val orderRequestId = 100L
    private val employeeCode = "E001"

    @BeforeEach
    fun setUp() {
        orderRequestRepository = mockk()
        orderRequestProductRepository = mockk()
        updater = OrderCancelHeaderStatusUpdater(orderRequestRepository, orderRequestProductRepository)
    }

    @Test
    @DisplayName("전량 커버(전 라인 cancel_requested) → CANCEL_REQUESTED 전이")
    fun fullyCovered_byCancelRequested_transitions() {
        val order = orderRequest(OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, order).also { it.markCancelRequested(employeeCode) },
            product(102, order).also { it.markCancelRequested(employeeCode) },
        )
        stub(order, lines)

        val result = updater.markCancelRequestedIfFullyCovered(orderRequestId)

        assertThat(result.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED)
    }

    @Test
    @DisplayName("커버 미달(일부만 cancel_requested) → 헤더 무변경")
    fun partiallyCovered_noChange() {
        val order = orderRequest(OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, order).also { it.markCancelRequested(employeeCode) },
            product(102, order), // 미커버
        )
        stub(order, lines)

        val result = updater.markCancelRequestedIfFullyCovered(orderRequestId)

        assertThat(result.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED)
    }

    @Test
    @DisplayName("마이그 'X'(확정 취소) 라인 포함 전량 커버 → CANCEL_REQUESTED 전이")
    fun fullyCovered_includingMigratedCancelled_transitions() {
        val order = orderRequest(OrderRequestStatus.APPROVED)
        val lines = listOf(
            product(101, order).also { it.cancel(employeeCode) },     // line_change_type='X'
            product(102, order).also { it.markCancelRequested(employeeCode) },
        )
        stub(order, lines)

        val result = updater.markCancelRequestedIfFullyCovered(orderRequestId)

        assertThat(result.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED)
        // 헤더 전이만 — 기존 'X' 라인 외 새 라인에 line_change_type 을 세팅하지 않는다.
        assertThat(lines[1].isCancelled()).isFalse()
    }

    @Test
    @DisplayName("라인 0개 방어 → 헤더 무변경(전량 판정에 라인 1개 이상 필요)")
    fun zeroLines_noChange() {
        val order = orderRequest(OrderRequestStatus.APPROVED)
        stub(order, emptyList())

        val result = updater.markCancelRequestedIfFullyCovered(orderRequestId)

        assertThat(result.orderRequestStatus).isEqualTo(OrderRequestStatus.APPROVED)
    }

    @Test
    @DisplayName("멱등 — 이미 CANCEL_REQUESTED 면 라인 조회 없이 그대로 반환")
    fun idempotent_alreadyCancelRequested() {
        val order = orderRequest(OrderRequestStatus.CANCEL_REQUESTED)
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order

        val result = updater.markCancelRequestedIfFullyCovered(orderRequestId)

        assertThat(result.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCEL_REQUESTED)
        // 이미 CANCEL_REQUESTED 면 라인 조회를 하지 않는다(멱등 short-circuit).
        io.mockk.verify(exactly = 0) {
            orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(any())
        }
    }

    @Test
    @DisplayName("주문 미존재 → OrderNotFoundException")
    fun orderNotFound_throws() {
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns null

        assertThatThrownBy { updater.markCancelRequestedIfFullyCovered(orderRequestId) }
            .isInstanceOf(OrderNotFoundException::class.java)
    }

    // ───────── 헬퍼 ─────────

    private fun stub(order: OrderRequest, lines: List<OrderRequestProduct>) {
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns order
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns lines
    }

    private fun orderRequest(status: OrderRequestStatus) = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "OR00000001",
        orderDate = LocalDateTime.of(2026, 4, 28, 10, 0),
        deliveryDate = LocalDate.of(2026, 5, 4),
        totalAmount = BigDecimal("100000"),
        orderRequestStatus = status,
        employee = Employee(id = 1L, employeeCode = employeeCode, name = "tester", role = AppAuthority.WOMAN),
        account = Account(id = 1, name = "A1", externalKey = "EXT-1"),
    )

    private fun product(id: Long, order: OrderRequest) = OrderRequestProduct(
        id = id,
        lineNumber = BigDecimal.valueOf(id),
        productCode = "P$id",
        unit = "EA",
        orderRequest = order,
    )
}
