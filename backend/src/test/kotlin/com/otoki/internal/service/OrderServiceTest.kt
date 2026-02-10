package com.otoki.internal.service

import com.otoki.internal.entity.ApprovalStatus
import com.otoki.internal.entity.Order
import com.otoki.internal.entity.Store
import com.otoki.internal.entity.User
import com.otoki.internal.entity.UserRole
import com.otoki.internal.entity.WorkerType
import com.otoki.internal.exception.InvalidDateRangeException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.repository.OrderRepository
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @InjectMocks
    private lateinit var orderService: OrderService

    private val testUser = User(
        id = 1L,
        employeeId = "12345678",
        password = "encoded",
        name = "홍길동",
        department = "영업부",
        branchName = "서울지점",
        role = UserRole.USER,
        workerType = WorkerType.PATROL
    )

    private val testStore = Store(
        id = 100L,
        storeCode = "ST001",
        storeName = "천사푸드"
    )

    // ========== 기본 조회 Tests ==========

    @Nested
    @DisplayName("기본 조회")
    inner class BasicQuery {

        @Test
        @DisplayName("파라미터 없이 기본 조회 - 주문일 내림차순, 첫 페이지 20건")
        fun getMyOrders_defaultParams_returnsFirstPage() {
            // Given
            val orders = listOf(
                createTestOrder(1L, "OP00000001", ApprovalStatus.APPROVED),
                createTestOrder(2L, "OP00000002", ApprovalStatus.PENDING)
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))
            val page = PageImpl(orders, pageable, 2)
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any()
            )).thenReturn(page)

            // When
            val result = orderService.getMyOrders(
                userId = 1L,
                storeId = null,
                status = null,
                deliveryDateFrom = null,
                deliveryDateTo = null,
                sortBy = null,
                sortDir = null,
                page = null,
                size = null
            )

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].orderRequestNumber).isEqualTo("OP00000001")
            assertThat(result.content[0].clientId).isEqualTo(100L)
            assertThat(result.content[0].clientName).isEqualTo("천사푸드")
            assertThat(result.content[0].approvalStatus).isEqualTo("APPROVED")
            assertThat(result.totalElements).isEqualTo(2)
        }

        @Test
        @DisplayName("빈 결과 - 빈 Page 반환 (totalElements=0)")
        fun getMyOrders_noOrders_returnsEmptyPage() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate"))
            val page = PageImpl<Order>(emptyList(), pageable, 0)
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any()
            )).thenReturn(page)

            // When
            val result = orderService.getMyOrders(
                userId = 1L, storeId = null, status = null,
                deliveryDateFrom = null, deliveryDateTo = null,
                sortBy = null, sortDir = null, page = null, size = null
            )

            // Then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    // ========== 필터링 Tests ==========

    @Nested
    @DisplayName("필터링")
    inner class Filtering {

        @Test
        @DisplayName("거래처 필터 - storeId로 Repository 호출")
        fun getMyOrders_withClientId_passesStoreIdToRepo() {
            // Given
            val orders = listOf(createTestOrder(1L, "OP00000001", ApprovalStatus.APPROVED))
            val page = PageImpl(orders, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate")), 1)
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), eq(100L), isNull(), isNull(), isNull(), any()
            )).thenReturn(page)

            // When
            val result = orderService.getMyOrders(
                userId = 1L, storeId = 100L, status = null,
                deliveryDateFrom = null, deliveryDateTo = null,
                sortBy = null, sortDir = null, page = null, size = null
            )

            // Then
            assertThat(result.content).hasSize(1)
            verify(orderRepository).findByUserIdWithFilters(
                eq(1L), eq(100L), isNull(), isNull(), isNull(), any()
            )
        }

        @Test
        @DisplayName("상태 필터 - APPROVED로 Repository 호출")
        fun getMyOrders_withStatus_passesStatusToRepo() {
            // Given
            val orders = listOf(createTestOrder(1L, "OP00000001", ApprovalStatus.APPROVED))
            val page = PageImpl(orders, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate")), 1)
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), isNull(), eq(ApprovalStatus.APPROVED), isNull(), isNull(), any()
            )).thenReturn(page)

            // When
            val result = orderService.getMyOrders(
                userId = 1L, storeId = null, status = "APPROVED",
                deliveryDateFrom = null, deliveryDateTo = null,
                sortBy = null, sortDir = null, page = null, size = null
            )

            // Then
            assertThat(result.content).hasSize(1)
            verify(orderRepository).findByUserIdWithFilters(
                eq(1L), isNull(), eq(ApprovalStatus.APPROVED), isNull(), isNull(), any()
            )
        }

        @Test
        @DisplayName("납기일 범위 필터 - From/To로 Repository 호출")
        fun getMyOrders_withDateRange_passesDateRangeToRepo() {
            // Given
            val from = LocalDate.of(2026, 2, 1)
            val to = LocalDate.of(2026, 2, 28)
            val orders = listOf(createTestOrder(1L, "OP00000001", ApprovalStatus.APPROVED))
            val page = PageImpl(orders, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate")), 1)
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), eq(from), eq(to), any()
            )).thenReturn(page)

            // When
            val result = orderService.getMyOrders(
                userId = 1L, storeId = null, status = null,
                deliveryDateFrom = from, deliveryDateTo = to,
                sortBy = null, sortDir = null, page = null, size = null
            )

            // Then
            assertThat(result.content).hasSize(1)
            verify(orderRepository).findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), eq(from), eq(to), any()
            )
        }

        @Test
        @DisplayName("복합 필터 - clientId + status + 날짜 범위 모두 적용")
        fun getMyOrders_withAllFilters_passesAllToRepo() {
            // Given
            val from = LocalDate.of(2026, 2, 1)
            val to = LocalDate.of(2026, 2, 28)
            val orders = listOf(createTestOrder(1L, "OP00000001", ApprovalStatus.APPROVED))
            val page = PageImpl(orders, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate")), 1)
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), eq(100L), eq(ApprovalStatus.APPROVED), eq(from), eq(to), any()
            )).thenReturn(page)

            // When
            val result = orderService.getMyOrders(
                userId = 1L, storeId = 100L, status = "APPROVED",
                deliveryDateFrom = from, deliveryDateTo = to,
                sortBy = null, sortDir = null, page = null, size = null
            )

            // Then
            assertThat(result.content).hasSize(1)
            verify(orderRepository).findByUserIdWithFilters(
                eq(1L), eq(100L), eq(ApprovalStatus.APPROVED), eq(from), eq(to), any()
            )
        }
    }

    // ========== 정렬 Tests ==========

    @Nested
    @DisplayName("정렬")
    inner class Sorting {

        @Test
        @DisplayName("금액 내림차순 정렬 - totalAmount DESC")
        fun getMyOrders_sortByTotalAmountDesc_success() {
            // Given
            val page = PageImpl<Order>(
                emptyList(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "totalAmount")),
                0
            )
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any()
            )).thenReturn(page)

            // When
            orderService.getMyOrders(
                userId = 1L, storeId = null, status = null,
                deliveryDateFrom = null, deliveryDateTo = null,
                sortBy = "totalAmount", sortDir = "DESC",
                page = null, size = null
            )

            // Then
            verify(orderRepository).findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any()
            )
        }

        @Test
        @DisplayName("납기일 오름차순 정렬 - deliveryDate ASC")
        fun getMyOrders_sortByDeliveryDateAsc_success() {
            // Given
            val page = PageImpl<Order>(
                emptyList(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "deliveryDate")),
                0
            )
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any()
            )).thenReturn(page)

            // When
            orderService.getMyOrders(
                userId = 1L, storeId = null, status = null,
                deliveryDateFrom = null, deliveryDateTo = null,
                sortBy = "deliveryDate", sortDir = "ASC",
                page = null, size = null
            )

            // Then
            verify(orderRepository).findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any()
            )
        }
    }

    // ========== 유효성 검증 Tests ==========

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {

        @Test
        @DisplayName("잘못된 sortBy - INVALID_PARAMETER 예외")
        fun getMyOrders_invalidSortBy_throwsException() {
            assertThatThrownBy {
                orderService.getMyOrders(
                    userId = 1L, storeId = null, status = null,
                    deliveryDateFrom = null, deliveryDateTo = null,
                    sortBy = "invalidField", sortDir = null,
                    page = null, size = null
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
                .hasMessageContaining("정렬 기준")
        }

        @Test
        @DisplayName("잘못된 sortDir - INVALID_PARAMETER 예외")
        fun getMyOrders_invalidSortDir_throwsException() {
            assertThatThrownBy {
                orderService.getMyOrders(
                    userId = 1L, storeId = null, status = null,
                    deliveryDateFrom = null, deliveryDateTo = null,
                    sortBy = null, sortDir = "INVALID",
                    page = null, size = null
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
                .hasMessageContaining("정렬 방향")
        }

        @Test
        @DisplayName("잘못된 status - INVALID_PARAMETER 예외")
        fun getMyOrders_invalidStatus_throwsException() {
            assertThatThrownBy {
                orderService.getMyOrders(
                    userId = 1L, storeId = null, status = "INVALID_STATUS",
                    deliveryDateFrom = null, deliveryDateTo = null,
                    sortBy = null, sortDir = null,
                    page = null, size = null
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
                .hasMessageContaining("승인상태")
        }

        @Test
        @DisplayName("납기일 종료 < 시작 - INVALID_DATE_RANGE 예외")
        fun getMyOrders_invalidDateRange_throwsException() {
            assertThatThrownBy {
                orderService.getMyOrders(
                    userId = 1L, storeId = null, status = null,
                    deliveryDateFrom = LocalDate.of(2026, 2, 28),
                    deliveryDateTo = LocalDate.of(2026, 2, 1),
                    sortBy = null, sortDir = null,
                    page = null, size = null
                )
            }.isInstanceOf(InvalidDateRangeException::class.java)
        }

        @Test
        @DisplayName("음수 페이지 번호 - INVALID_PARAMETER 예외")
        fun getMyOrders_negativePage_throwsException() {
            assertThatThrownBy {
                orderService.getMyOrders(
                    userId = 1L, storeId = null, status = null,
                    deliveryDateFrom = null, deliveryDateTo = null,
                    sortBy = null, sortDir = null,
                    page = -1, size = null
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
                .hasMessageContaining("페이지 번호")
        }

        @Test
        @DisplayName("페이지 크기 0 - INVALID_PARAMETER 예외")
        fun getMyOrders_zeroSize_throwsException() {
            assertThatThrownBy {
                orderService.getMyOrders(
                    userId = 1L, storeId = null, status = null,
                    deliveryDateFrom = null, deliveryDateTo = null,
                    sortBy = null, sortDir = null,
                    page = null, size = 0
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
                .hasMessageContaining("페이지 크기")
        }

        @Test
        @DisplayName("페이지 크기 초과 (101) - INVALID_PARAMETER 예외")
        fun getMyOrders_oversizeSize_throwsException() {
            assertThatThrownBy {
                orderService.getMyOrders(
                    userId = 1L, storeId = null, status = null,
                    deliveryDateFrom = null, deliveryDateTo = null,
                    sortBy = null, sortDir = null,
                    page = null, size = 101
                )
            }.isInstanceOf(InvalidOrderParameterException::class.java)
                .hasMessageContaining("페이지 크기")
        }
    }

    // ========== DTO 변환 Tests ==========

    @Nested
    @DisplayName("DTO 변환")
    inner class DtoMapping {

        @Test
        @DisplayName("Order -> OrderSummaryResponse 변환 검증")
        fun getMyOrders_dtoMapping_allFieldsMapped() {
            // Given
            val order = createTestOrder(
                id = 5L,
                orderRequestNumber = "OP00000074",
                approvalStatus = ApprovalStatus.APPROVED,
                totalAmount = 612000000L,
                isClosed = false,
                clientDeadlineTime = "13:40"
            )
            val page = PageImpl(
                listOf(order),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderDate")),
                1
            )
            whenever(orderRepository.findByUserIdWithFilters(
                eq(1L), isNull(), isNull(), isNull(), isNull(), any()
            )).thenReturn(page)

            // When
            val result = orderService.getMyOrders(
                userId = 1L, storeId = null, status = null,
                deliveryDateFrom = null, deliveryDateTo = null,
                sortBy = null, sortDir = null, page = null, size = null
            )

            // Then
            val dto = result.content[0]
            assertThat(dto.id).isEqualTo(5L)
            assertThat(dto.orderRequestNumber).isEqualTo("OP00000074")
            assertThat(dto.clientId).isEqualTo(100L)
            assertThat(dto.clientName).isEqualTo("천사푸드")
            assertThat(dto.clientDeadlineTime).isEqualTo("13:40")
            assertThat(dto.orderDate).isEqualTo("2026-02-01")
            assertThat(dto.deliveryDate).isEqualTo("2026-02-04")
            assertThat(dto.totalAmount).isEqualTo(612000000L)
            assertThat(dto.approvalStatus).isEqualTo("APPROVED")
            assertThat(dto.isClosed).isFalse()
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun createTestOrder(
        id: Long,
        orderRequestNumber: String,
        approvalStatus: ApprovalStatus,
        totalAmount: Long = 100000L,
        isClosed: Boolean = false,
        clientDeadlineTime: String? = null
    ): Order {
        return Order(
            id = id,
            orderRequestNumber = orderRequestNumber,
            user = testUser,
            store = testStore,
            orderDate = LocalDate.of(2026, 2, 1),
            deliveryDate = LocalDate.of(2026, 2, 4),
            totalAmount = totalAmount,
            approvalStatus = approvalStatus,
            isClosed = isClosed,
            clientDeadlineTime = clientDeadlineTime
        )
    }
}
