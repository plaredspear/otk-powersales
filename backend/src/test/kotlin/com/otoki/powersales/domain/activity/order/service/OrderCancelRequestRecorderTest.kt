package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("OrderCancelRequestRecorder 테스트 (#858 취소 흔적 기록)")
class OrderCancelRequestRecorderTest {

    private lateinit var orderRequestProductRepository: OrderRequestProductRepository
    private lateinit var recorder: OrderCancelRequestRecorder

    private val orderRequestId = 100L
    private val requester = "E001"

    @BeforeEach
    fun setUp() {
        orderRequestProductRepository = mockk()
        recorder = OrderCancelRequestRecorder(orderRequestProductRepository)
    }

    @Test
    @DisplayName("전체 취소 — 전 라인에 cancel_requested 세팅 (line_change_type 미변경)")
    fun record_fullCancel() {
        val orderRequest = orderRequest()
        val line1 = product(101, "P001", orderRequest)
        val line2 = product(102, "P002", orderRequest)
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns
            listOf(line1, line2)

        recorder.recordCancelRequested(orderRequestId, listOf(101L, 102L), requester)

        assertThat(line1.cancelRequestedAt).isNotNull()
        assertThat(line1.cancelRequestedBy).isEqualTo(requester)
        assertThat(line2.cancelRequestedAt).isNotNull()
        // 흔적만 기록, 취소 마커는 미변경
        assertThat(line1.isCancelled()).isFalse()
        assertThat(line1.cancelledAt).isNull()
    }

    @Test
    @DisplayName("부분 취소 — 대상 라인만 세팅, 나머지 라인 NULL 유지")
    fun record_partialCancel() {
        val orderRequest = orderRequest()
        val target = product(101, "P001", orderRequest)
        val other = product(102, "P002", orderRequest)
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns
            listOf(target, other)

        recorder.recordCancelRequested(orderRequestId, listOf(101L), requester)

        assertThat(target.cancelRequestedAt).isNotNull()
        assertThat(other.cancelRequestedAt).isNull()
        assertThat(other.cancelRequestedBy).isNull()
    }

    @Test
    @DisplayName("흔적 롤백 — 대상 라인의 cancel_requested_at/by 를 NULL 로 되돌림 (SAP 명시적 거부)")
    fun clear_rollbackTrace() {
        val orderRequest = orderRequest()
        val line1 = product(101, "P001", orderRequest).also { it.markCancelRequested(requester) }
        val line2 = product(102, "P002", orderRequest).also { it.markCancelRequested(requester) }
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns
            listOf(line1, line2)

        recorder.clearCancelRequested(orderRequestId, listOf(101L, 102L))

        assertThat(line1.cancelRequestedAt).isNull()
        assertThat(line1.cancelRequestedBy).isNull()
        assertThat(line2.cancelRequestedAt).isNull()
        assertThat(line2.cancelRequestedBy).isNull()
    }

    @Test
    @DisplayName("흔적 롤백 부분 — 대상 라인만 NULL, 나머지 흔적 유지")
    fun clear_partial() {
        val orderRequest = orderRequest()
        val target = product(101, "P001", orderRequest).also { it.markCancelRequested(requester) }
        val other = product(102, "P002", orderRequest).also { it.markCancelRequested(requester) }
        every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId) } returns
            listOf(target, other)

        recorder.clearCancelRequested(orderRequestId, listOf(101L))

        assertThat(target.cancelRequestedAt).isNull()
        // 롤백 대상 아닌 라인 흔적은 유지.
        assertThat(other.cancelRequestedAt).isNotNull()
        assertThat(other.cancelRequestedBy).isEqualTo(requester)
    }

    // ───────── 헬퍼 ─────────

    private fun orderRequest() = OrderRequest(
        id = orderRequestId,
        orderRequestNumber = "OR00000006",
        orderDate = LocalDateTime.of(2026, 7, 1, 10, 0),
        totalAmount = BigDecimal("100000"),
        orderRequestStatus = OrderRequestStatus.APPROVED,
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
