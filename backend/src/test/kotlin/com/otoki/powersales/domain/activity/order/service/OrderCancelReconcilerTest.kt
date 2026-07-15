package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("OrderCancelReconciler 테스트 (#858 상세조회 정합)")
class OrderCancelReconcilerTest {

    private lateinit var orderRequestRepository: OrderRequestRepository
    private lateinit var orderRequestProductRepository: OrderRequestProductRepository
    private lateinit var reconciler: OrderCancelReconciler

    private val orderRequestId = 100L
    private val requester = "E001"

    @BeforeEach
    fun setUp() {
        orderRequestRepository = mockk(relaxed = true)
        orderRequestProductRepository = mockk()
        reconciler = OrderCancelReconciler(orderRequestRepository, orderRequestProductRepository)
    }

    @Test
    @DisplayName("정합 승격 — 4조건 모두 만족 라인은 line_change_type='X' + cancelled 세팅")
    fun promote_whenAllConditionsMet() {
        val orderRequest = orderRequest(OrderRequestStatus.APPROVED)
        // 취소 요청 흔적 O, line_change_type NULL, cancelled_at NULL → 정합 가능
        val line = product(101, "P001", orderRequest).apply { markCancelRequested(requester) }
        stubLines(listOf(line, product(102, "P002", orderRequest))) // P002 는 흔적 없음

        val promoted = reconciler.reconcileTimedOutCancels(orderRequestId, setOf("P001"))

        assertThat(promoted).containsExactly("P001")
        assertThat(line.isCancelled()).isTrue()
        assertThat(line.cancelledAt).isNotNull()
        assertThat(line.cancelledBy).isEqualTo(requester) // 요청자 승계
    }

    @Test
    @DisplayName("진짜 결품 오정합 방지 — 취소 요청 흔적 없는 라인은 DefaultReason 있어도 미승격")
    fun skip_whenNoCancelRequestTrace() {
        val orderRequest = orderRequest(OrderRequestStatus.APPROVED)
        val line = product(101, "P001", orderRequest) // 흔적 없음
        stubLines(listOf(line))

        val promoted = reconciler.reconcileTimedOutCancels(orderRequestId, setOf("P001"))

        assertThat(promoted).isEmpty()
        assertThat(line.isCancelled()).isFalse()
    }

    @Test
    @DisplayName("멱등 — 이미 확정된 라인(line_change_type='X')은 재정합 안 함")
    fun idempotent_whenAlreadyCancelledByLineChangeType() {
        val orderRequest = orderRequest(OrderRequestStatus.APPROVED)
        val line = product(101, "P001", orderRequest).apply {
            markCancelRequested(requester)
            cancel(requester) // line_change_type='X' + cancelled_at 세팅
        }
        stubLines(listOf(line))

        val promoted = reconciler.reconcileTimedOutCancels(orderRequestId, setOf("P001"))

        assertThat(promoted).isEmpty()
    }

    @Test
    @DisplayName("멱등 — cancelled_at 세팅된 라인은 재정합 안 함 (line_change_type 비정상이어도)")
    fun idempotent_whenCancelledAtSet() {
        val orderRequest = orderRequest(OrderRequestStatus.APPROVED)
        val line = product(101, "P001", orderRequest).apply {
            markCancelRequested(requester)
            cancelledAt = LocalDateTime.now() // line_change_type 는 NULL 이지만 cancelled_at 세팅됨
        }
        stubLines(listOf(line))

        val promoted = reconciler.reconcileTimedOutCancels(orderRequestId, setOf("P001"))

        assertThat(promoted).isEmpty()
    }

    @Test
    @DisplayName("부분 취소 — 취소 요청한 라인만 승격, 미요청 라인은 DefaultReason 있어도 미승격 + 헤더 유지")
    fun partialCancel_onlyRequestedLinePromoted() {
        val orderRequest = orderRequest(OrderRequestStatus.APPROVED)
        val requested = product(101, "P001", orderRequest).apply { markCancelRequested(requester) }
        val notRequested = product(102, "P002", orderRequest) // 흔적 없음
        stubLines(listOf(requested, notRequested))

        // SAP 응답에 두 productCode 모두 DefaultReason 있음
        val promoted = reconciler.reconcileTimedOutCancels(orderRequestId, setOf("P001", "P002"))

        assertThat(promoted).containsExactly("P001")
        assertThat(requested.isCancelled()).isTrue()
        assertThat(notRequested.isCancelled()).isFalse()
        // 미취소 라인 남음 → 헤더 전이 없음 (findByIdForUpdate 미호출)
        verify(exactly = 0) { orderRequestRepository.findByIdForUpdate(any()) }
    }

    @Test
    @DisplayName("전 라인 정합 시 헤더 CANCELED 전이")
    fun headerCanceled_whenAllLinesCancelled() {
        val orderRequest = orderRequest(OrderRequestStatus.APPROVED)
        val line1 = product(101, "P001", orderRequest).apply { markCancelRequested(requester) }
        val line2 = product(102, "P002", orderRequest).apply { markCancelRequested(requester) }
        stubLines(listOf(line1, line2))
        every { orderRequestRepository.findByIdForUpdate(orderRequestId) } returns orderRequest

        val promoted = reconciler.reconcileTimedOutCancels(orderRequestId, setOf("P001", "P002"))

        assertThat(promoted).containsExactlyInAnyOrder("P001", "P002")
        assertThat(orderRequest.orderRequestStatus).isEqualTo(OrderRequestStatus.CANCELED)
    }

    @Test
    @DisplayName("SAP DefaultReason 집합이 비면 정합 없이 빈 집합 반환 (조회 미수행)")
    fun noop_whenNoDefaultReason() {
        val promoted = reconciler.reconcileTimedOutCancels(orderRequestId, emptySet())

        assertThat(promoted).isEmpty()
        verify(exactly = 0) { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(any()) }
    }

    // ───────── 헬퍼 ─────────

    private fun stubLines(lines: List<OrderRequestProduct>) {
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns lines
    }

    private fun orderRequest(status: OrderRequestStatus) = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "OR00000006",
        orderDate = LocalDateTime.of(2026, 7, 1, 10, 0),
        totalAmount = BigDecimal("100000"),
        orderRequestStatus = status,
        account = Account(id = 1, name = "A1", externalKey = "EXT-1"),
    )

    private fun product(id: Long, productCode: String, orderRequest: OrderRequest) = OrderRequestProduct(
        id = id,
        lineNumber = BigDecimal.valueOf(id),
        productCode = productCode,
        unit = "EA",
        orderRequest = orderRequest,
    )
}
