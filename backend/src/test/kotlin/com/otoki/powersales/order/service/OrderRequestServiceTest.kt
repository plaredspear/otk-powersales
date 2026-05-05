package com.otoki.powersales.order.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestStatus
import com.otoki.powersales.order.exception.InvalidDateRangeException
import com.otoki.powersales.order.exception.InvalidOrderParameterException
import com.otoki.powersales.order.exception.OrderDateRangeTooWideException
import com.otoki.powersales.order.repository.OrderRequestRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderRequestService 테스트")
class OrderRequestServiceTest {

    @Mock
    private lateinit var orderRequestRepository: OrderRequestRepository

    private val fixedClock: Clock = Clock.fixed(
        LocalDateTime.of(2026, 5, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
        ZoneId.of("Asia/Seoul"),
    )

    private lateinit var service: OrderRequestService

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        service = OrderRequestService(orderRequestRepository, fixedClock)
    }

    @Nested
    @DisplayName("getMyOrderRequests - 입력 검증")
    inner class ValidationTests {

        @Test
        @DisplayName("실패 - deliveryDateFrom 미제공 -> ORD_INVALID_PARAM")
        fun missingFrom() {
            assertThatThrownBy {
                service.getMyOrderRequests(
                    userId = 1L,
                    accountId = null,
                    status = null,
                    deliveryDateFrom = null,
                    deliveryDateTo = LocalDate.of(2026, 5, 6),
                    sortBy = null,
                    sortDir = null,
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
        }

        @Test
        @DisplayName("실패 - 종료일 < 시작일 -> ORD_INVALID_DATE_RANGE")
        fun reversedRange() {
            assertThatThrownBy {
                service.getMyOrderRequests(
                    userId = 1L,
                    accountId = null,
                    status = null,
                    deliveryDateFrom = LocalDate.of(2026, 5, 10),
                    deliveryDateTo = LocalDate.of(2026, 5, 1),
                    sortBy = null,
                    sortDir = null,
                )
            }.isInstanceOf(InvalidDateRangeException::class.java)
        }

        @Test
        @DisplayName("실패 - 8일 초과 -> ORD_DATE_RANGE_TOO_WIDE")
        fun tooWideRange() {
            assertThatThrownBy {
                service.getMyOrderRequests(
                    userId = 1L,
                    accountId = null,
                    status = null,
                    deliveryDateFrom = LocalDate.of(2026, 5, 1),
                    deliveryDateTo = LocalDate.of(2026, 5, 9),
                    sortBy = null,
                    sortDir = null,
                )
            }.isInstanceOf(OrderDateRangeTooWideException::class.java)
        }

        @Test
        @DisplayName("실패 - 잘못된 sortBy -> ORD_INVALID_PARAM")
        fun invalidSortBy() {
            assertThatThrownBy {
                service.getMyOrderRequests(
                    userId = 1L,
                    accountId = null,
                    status = null,
                    deliveryDateFrom = LocalDate.of(2026, 5, 1),
                    deliveryDateTo = LocalDate.of(2026, 5, 7),
                    sortBy = "createdAt",
                    sortDir = null,
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
        }

        @Test
        @DisplayName("실패 - 잘못된 status -> ORD_INVALID_PARAM")
        fun invalidStatus() {
            assertThatThrownBy {
                service.getMyOrderRequests(
                    userId = 1L,
                    accountId = null,
                    status = "INVALID_STATUS",
                    deliveryDateFrom = LocalDate.of(2026, 5, 1),
                    deliveryDateTo = LocalDate.of(2026, 5, 7),
                    sortBy = null,
                    sortDir = null,
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
        }

        @Test
        @DisplayName("실패 - clientId < 1 -> ORD_INVALID_PARAM")
        fun invalidClientId() {
            assertThatThrownBy {
                service.getMyOrderRequests(
                    userId = 1L,
                    accountId = 0L,
                    status = null,
                    deliveryDateFrom = LocalDate.of(2026, 5, 1),
                    deliveryDateTo = LocalDate.of(2026, 5, 7),
                    sortBy = null,
                    sortDir = null,
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("getMyOrderRequests - 조회 + 매핑")
    inner class QueryTests {

        @Test
        @DisplayName("성공 - 본인 항목만 매핑되어 반환")
        fun returnsItems() {
            val record = createOrderRequest(id = 100L, accountId = 5, accountName = "ABC")
            whenever(
                orderRequestRepository.findMyOrderRequests(
                    any(), anyOrNull(), anyOrNull(), any(), any(), any(), any(), any(),
                ),
            ).thenReturn(listOf(record))

            val response = service.getMyOrderRequests(
                userId = 1L,
                accountId = null,
                status = null,
                deliveryDateFrom = LocalDate.of(2026, 5, 1),
                deliveryDateTo = LocalDate.of(2026, 5, 7),
                sortBy = null,
                sortDir = null,
            )

            assertThat(response.items).hasSize(1)
            assertThat(response.items[0].id).isEqualTo(100L)
            assertThat(response.items[0].clientId).isEqualTo(5L)
            assertThat(response.items[0].clientName).isEqualTo("ABC")
            assertThat(response.total).isEqualTo(1)
            assertThat(response.truncated).isFalse()
        }

        @Test
        @DisplayName("성공 - 응답 라인 수 상한 도달 시 truncated=true + 첫 2000건만 반환")
        fun truncatedAtLimit() {
            val records = (1..2001).map { createOrderRequest(id = it.toLong()) }
            whenever(
                orderRequestRepository.findMyOrderRequests(
                    any(), anyOrNull(), anyOrNull(), any(), any(), any(), any(), any(),
                ),
            ).thenReturn(records)

            val response = service.getMyOrderRequests(
                userId = 1L,
                accountId = null,
                status = null,
                deliveryDateFrom = LocalDate.of(2026, 5, 1),
                deliveryDateTo = LocalDate.of(2026, 5, 7),
                sortBy = null,
                sortDir = null,
            )

            assertThat(response.items).hasSize(2000)
            assertThat(response.total).isEqualTo(2000)
            assertThat(response.truncated).isTrue()
        }
    }

    @Nested
    @DisplayName("calculateIsClosed - 마감 처리")
    inner class IsClosedTests {

        @Test
        @DisplayName("오늘 < 납기일 -> false")
        fun beforeDeliveryDate() {
            val deliveryDate = LocalDate.of(2026, 5, 6)
            assertThat(service.calculateIsClosed(deliveryDate, "13:50")).isFalse()
        }

        @Test
        @DisplayName("오늘 > 납기일 -> true")
        fun afterDeliveryDate() {
            val deliveryDate = LocalDate.of(2026, 5, 4)
            assertThat(service.calculateIsClosed(deliveryDate, "13:50")).isTrue()
        }

        @Test
        @DisplayName("오늘 == 납기일 + clientDeadlineTime null -> false")
        fun sameDayNullDeadline() {
            val deliveryDate = LocalDate.of(2026, 5, 5)
            assertThat(service.calculateIsClosed(deliveryDate, null)).isFalse()
        }

        @Test
        @DisplayName("오늘 == 납기일 + 마감 -20분 경계 (10:00 vs 10:20-20=10:00) -> true")
        fun sameDayAtCutoff() {
            // fixedClock = 2026-05-05 10:00 KST. cutoff = 10:20 - 20분 = 10:00.
            val deliveryDate = LocalDate.of(2026, 5, 5)
            assertThat(service.calculateIsClosed(deliveryDate, "10:20")).isTrue()
        }

        @Test
        @DisplayName("오늘 == 납기일 + 마감 -19분 (cutoff 10:01) -> false")
        fun sameDayBeforeCutoff() {
            val deliveryDate = LocalDate.of(2026, 5, 5)
            assertThat(service.calculateIsClosed(deliveryDate, "10:21")).isFalse()
        }

        @Test
        @DisplayName("오늘 == 납기일 + clientDeadlineTime 파싱 실패 -> false")
        fun sameDayInvalidDeadline() {
            val deliveryDate = LocalDate.of(2026, 5, 5)
            assertThat(service.calculateIsClosed(deliveryDate, "INVALID")).isFalse()
        }
    }

    private fun createOrderRequest(
        id: Long = 1L,
        accountId: Int = 1,
        accountName: String = "Test Account",
    ): OrderRequest {
        val account = Account(id = accountId, name = accountName)
        val employee = Employee(id = 1L, employeeCode = "20030117", name = "Test")
        return OrderRequest(
            id = id,
            orderRequestNumber = "OR-${id.toString().padStart(7, '0')}",
            orderDate = LocalDateTime.of(2026, 5, 4, 10, 0),
            deliveryDate = LocalDate.of(2026, 5, 6),
            totalAmount = BigDecimal("1234567.00"),
            orderRequestStatus = OrderRequestStatus.APPROVED,
            employee = employee,
            account = account,
        )
    }
}
