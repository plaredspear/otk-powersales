package com.otoki.internal.service

import com.otoki.internal.entity.ApprovalStatus
import com.otoki.internal.entity.DeliveryStatus
import com.otoki.internal.entity.Order
import com.otoki.internal.entity.OrderItem
import com.otoki.internal.entity.OrderProcessingRecord
import com.otoki.internal.entity.OrderRejection
import com.otoki.internal.entity.Store
import com.otoki.internal.entity.User
import com.otoki.internal.dto.response.OrderCancelResponse
import com.otoki.internal.exception.AlreadyCancelledException
import com.otoki.internal.exception.ForbiddenOrderAccessException
import com.otoki.internal.exception.InvalidDateRangeException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.InvalidOrderStatusException
import com.otoki.internal.exception.OrderAlreadyClosedException
import com.otoki.internal.exception.OrderNotFoundException
import com.otoki.internal.exception.ProductNotInOrderException
import com.otoki.internal.repository.OrderItemRepository
import com.otoki.internal.repository.OrderProcessingRecordRepository
import com.otoki.internal.repository.OrderRejectionRepository
import com.otoki.internal.repository.OrderRepository
import com.otoki.internal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var orderItemRepository: OrderItemRepository

    @Mock
    private lateinit var orderProcessingRecordRepository: OrderProcessingRecordRepository

    @Mock
    private lateinit var orderRejectionRepository: OrderRejectionRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var clock: Clock

    private val testUser = User(
        id = 1L,
        employeeId = "12345678",
        password = "encoded",
        name = "홍길동",
        orgName = "서울지점"
    )

    private val testStore = Store(
        id = 100L,
        storeCode = "ST001",
        storeName = "천사푸드"
    )

    private fun createOrderService(customClock: Clock? = null): OrderService {
        return OrderService(
            orderRepository = orderRepository,
            orderItemRepository = orderItemRepository,
            orderProcessingRecordRepository = orderProcessingRecordRepository,
            orderRejectionRepository = orderRejectionRepository,
            userRepository = userRepository,
            clock = customClock ?: clock
        )
    }

    // ========== 기본 조회 Tests ==========

    @Nested
    @DisplayName("기본 조회")
    inner class BasicQuery {

        @Test
        @DisplayName("파라미터 없이 기본 조회 - 주문일 내림차순, 첫 페이지 20건")
        fun getMyOrders_defaultParams_returnsFirstPage() {
            // Given
            val orderService = createOrderService()
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
            val orderService = createOrderService()
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
            val orderService = createOrderService()
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
            val orderService = createOrderService()
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
            val orderService = createOrderService()
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
            val orderService = createOrderService()
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
            val orderService = createOrderService()
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
            val orderService = createOrderService()
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
            // Given
            val orderService = createOrderService()

            // When & Then
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
            // Given
            val orderService = createOrderService()

            // When & Then
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
            // Given
            val orderService = createOrderService()

            // When & Then
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
            // Given
            val orderService = createOrderService()

            // When & Then
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
            // Given
            val orderService = createOrderService()

            // When & Then
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
            // Given
            val orderService = createOrderService()

            // When & Then
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
            // Given
            val orderService = createOrderService()

            // When & Then
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
            val orderService = createOrderService()
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

    // ========== getOrderDetail Tests ==========

    @Nested
    @DisplayName("주문 상세 조회 - getOrderDetail")
    inner class GetOrderDetail {

        @Test
        @DisplayName("정상 조회 (마감 전) - items 반환, processingStatus null, rejectedItems null")
        fun getOrderDetail_beforeClosed_returnsItemsOnly() {
            // Given - 납기일 이전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 3, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = false
            )
            val items = listOf(
                createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5),
                createTestOrderItem(2L, order, "P002", "제품B", 5.0, 10)
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)
            whenever(orderProcessingRecordRepository.findByOrderId(1L)).thenReturn(emptyList())
            whenever(orderRejectionRepository.findByOrderId(1L)).thenReturn(emptyList())

            // When
            val result = orderService.getOrderDetail(userId = 1L, orderId = 1L)

            // Then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.orderRequestNumber).isEqualTo("OP00000001")
            assertThat(result.isClosed).isFalse()
            assertThat(result.orderedItems).hasSize(2)
            assertThat(result.orderedItems[0].productCode).isEqualTo("P001")
            assertThat(result.orderedItems[0].productName).isEqualTo("제품A")
            assertThat(result.orderedItems[0].totalQuantityBoxes).isEqualTo(10.0)
            assertThat(result.orderedItems[0].totalQuantityPieces).isEqualTo(5)
            assertThat(result.orderProcessingStatus).isNull()
            assertThat(result.rejectedItems).isNull()
        }

        @Test
        @DisplayName("정상 조회 (마감 후, SAP 데이터 있음) - processingStatus 포함")
        fun getOrderDetail_afterClosedWithSAPData_includesProcessingStatus() {
            // Given - 납기일 이후 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = true
            )
            val items = listOf(
                createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5)
            )
            val processingRecords = listOf(
                createTestProcessingRecord(1L, order, "SAP001", "P001", "제품A", "10", DeliveryStatus.DELIVERED)
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)
            whenever(orderProcessingRecordRepository.findByOrderId(1L)).thenReturn(processingRecords)
            whenever(orderRejectionRepository.findByOrderId(1L)).thenReturn(emptyList())

            // When
            val result = orderService.getOrderDetail(userId = 1L, orderId = 1L)

            // Then
            assertThat(result.isClosed).isTrue()
            assertThat(result.orderProcessingStatus).isNotNull
            assertThat(result.orderProcessingStatus!!.sapOrderNumber).isEqualTo("SAP001")
            assertThat(result.orderProcessingStatus!!.items).hasSize(1)
            assertThat(result.orderProcessingStatus!!.items[0].productCode).isEqualTo("P001")
            assertThat(result.orderProcessingStatus!!.items[0].deliveredQuantity).isEqualTo("10")
            assertThat(result.orderProcessingStatus!!.items[0].deliveryStatus).isEqualTo("DELIVERED")
        }

        @Test
        @DisplayName("정상 조회 (마감 후, 반려 있음) - rejectedItems 포함")
        fun getOrderDetail_afterClosedWithRejections_includesRejectedItems() {
            // Given - 납기일 이후 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = true
            )
            val items = listOf(
                createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5)
            )
            val rejections = listOf(
                createTestRejection(1L, order, "P002", "제품B", 5, "재고 부족")
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)
            whenever(orderProcessingRecordRepository.findByOrderId(1L)).thenReturn(emptyList())
            whenever(orderRejectionRepository.findByOrderId(1L)).thenReturn(rejections)

            // When
            val result = orderService.getOrderDetail(userId = 1L, orderId = 1L)

            // Then
            assertThat(result.isClosed).isTrue()
            assertThat(result.rejectedItems).isNotNull
            assertThat(result.rejectedItems).hasSize(1)
            assertThat(result.rejectedItems!![0].productCode).isEqualTo("P002")
            assertThat(result.rejectedItems!![0].productName).isEqualTo("제품B")
            assertThat(result.rejectedItems!![0].orderQuantityBoxes).isEqualTo(5)
            assertThat(result.rejectedItems!![0].rejectionReason).isEqualTo("재고 부족")
        }

        @Test
        @DisplayName("정상 조회 (마감 후, SAP + 반려 모두) - 둘 다 포함")
        fun getOrderDetail_afterClosedWithBothSAPAndRejections_includesBoth() {
            // Given - 납기일 이후 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = true
            )
            val items = listOf(
                createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5)
            )
            val processingRecords = listOf(
                createTestProcessingRecord(1L, order, "SAP001", "P001", "제품A", "10", DeliveryStatus.SHIPPING)
            )
            val rejections = listOf(
                createTestRejection(1L, order, "P002", "제품B", 5, "재고 부족")
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)
            whenever(orderProcessingRecordRepository.findByOrderId(1L)).thenReturn(processingRecords)
            whenever(orderRejectionRepository.findByOrderId(1L)).thenReturn(rejections)

            // When
            val result = orderService.getOrderDetail(userId = 1L, orderId = 1L)

            // Then
            assertThat(result.isClosed).isTrue()
            assertThat(result.orderProcessingStatus).isNotNull
            assertThat(result.orderProcessingStatus!!.items).hasSize(1)
            assertThat(result.rejectedItems).isNotNull
            assertThat(result.rejectedItems!!).hasSize(1)
        }

        @Test
        @DisplayName("존재하지 않는 주문 - OrderNotFoundException")
        fun getOrderDetail_orderNotFound_throwsException() {
            // Given
            val orderService = createOrderService()
            whenever(orderRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                orderService.getOrderDetail(userId = 1L, orderId = 999L)
            }.isInstanceOf(OrderNotFoundException::class.java)
                .hasMessageContaining("주문을 찾을 수 없습니다")
        }

        @Test
        @DisplayName("다른 사용자 주문 - ForbiddenOrderAccessException")
        fun getOrderDetail_differentUser_throwsException() {
            // Given
            val orderService = createOrderService()
            val otherUser = User(
                id = 2L,
                employeeId = "87654321",
                password = "encoded",
                name = "김철수",
                orgName = "서울지점"
            )
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                user = otherUser
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When & Then
            assertThatThrownBy {
                orderService.getOrderDetail(userId = 1L, orderId = 1L)
            }.isInstanceOf(ForbiddenOrderAccessException::class.java)
                .hasMessageContaining("접근 권한이 없습니다")
        }

        @Test
        @DisplayName("DTO 매핑 검증 - 모든 필드 확인")
        fun getOrderDetail_dtoMapping_allFieldsMapped() {
            // Given - 납기일 이전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 3, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 5L,
                orderRequestNumber = "OP00000074",
                approvalStatus = ApprovalStatus.APPROVED,
                totalAmount = 612000000L,
                isClosed = false,
                clientDeadlineTime = "13:40"
            )
            val items = listOf(
                createTestOrderItem(1L, order, "P001", "제품A", 10.5, 3)
            )

            whenever(orderRepository.findById(5L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(5L)).thenReturn(items)
            whenever(orderProcessingRecordRepository.findByOrderId(5L)).thenReturn(emptyList())
            whenever(orderRejectionRepository.findByOrderId(5L)).thenReturn(emptyList())

            // When
            val result = orderService.getOrderDetail(userId = 1L, orderId = 5L)

            // Then
            assertThat(result.id).isEqualTo(5L)
            assertThat(result.orderRequestNumber).isEqualTo("OP00000074")
            assertThat(result.clientId).isEqualTo(100L)
            assertThat(result.clientName).isEqualTo("천사푸드")
            assertThat(result.clientDeadlineTime).isEqualTo("13:40")
            assertThat(result.orderDate).isEqualTo("2026-02-01")
            assertThat(result.deliveryDate).isEqualTo("2026-02-04")
            assertThat(result.totalAmount).isEqualTo(612000000L)
            assertThat(result.approvalStatus).isEqualTo("APPROVED")
            assertThat(result.isClosed).isFalse()
            assertThat(result.orderedItems[0].productCode).isEqualTo("P001")
            assertThat(result.orderedItems[0].productName).isEqualTo("제품A")
            assertThat(result.orderedItems[0].totalQuantityBoxes).isEqualTo(10.5)
            assertThat(result.orderedItems[0].totalQuantityPieces).isEqualTo(3)
            assertThat(result.orderedItems[0].isCancelled).isFalse()
        }
    }

    // ========== resendOrder Tests ==========

    @Nested
    @DisplayName("주문 재전송 - resendOrder")
    inner class ResendOrder {

        @Test
        @DisplayName("성공 - SEND_FAILED 상태, 마감 전 → RESEND로 변경")
        fun resendOrder_sendFailedAndNotClosed_success() {
            // Given
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.SEND_FAILED,
                isClosed = false
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When
            orderService.resendOrder(userId = 1L, orderId = 1L)

            // Then
            assertThat(order.approvalStatus).isEqualTo(ApprovalStatus.RESEND)
            verify(orderRepository).save(order)
        }

        @Test
        @DisplayName("존재하지 않는 주문 - OrderNotFoundException")
        fun resendOrder_orderNotFound_throwsException() {
            // Given
            val orderService = createOrderService()
            whenever(orderRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                orderService.resendOrder(userId = 1L, orderId = 999L)
            }.isInstanceOf(OrderNotFoundException::class.java)
                .hasMessageContaining("주문을 찾을 수 없습니다")
        }

        @Test
        @DisplayName("다른 사용자 주문 - ForbiddenOrderAccessException")
        fun resendOrder_differentUser_throwsException() {
            // Given
            val orderService = createOrderService()
            val otherUser = User(
                id = 2L,
                employeeId = "87654321",
                password = "encoded",
                name = "김철수",
                orgName = "서울지점"
            )
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.SEND_FAILED,
                user = otherUser
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When & Then
            assertThatThrownBy {
                orderService.resendOrder(userId = 1L, orderId = 1L)
            }.isInstanceOf(ForbiddenOrderAccessException::class.java)
                .hasMessageContaining("접근 권한이 없습니다")
        }

        @Test
        @DisplayName("마감 후 - OrderAlreadyClosedException")
        fun resendOrder_orderClosed_throwsException() {
            // Given - 납기일(2026-02-04) 이후 시점의 Clock 사용
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.SEND_FAILED,
                isClosed = true
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When & Then
            assertThatThrownBy {
                orderService.resendOrder(userId = 1L, orderId = 1L)
            }.isInstanceOf(OrderAlreadyClosedException::class.java)
                .hasMessageContaining("마감된 주문은 재전송할 수 없습니다")

            verify(orderRepository, never()).save(any())
        }

        @Test
        @DisplayName("APPROVED 상태 - InvalidOrderStatusException")
        fun resendOrder_approvedStatus_throwsException() {
            // Given - 마감 전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = false
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When & Then
            assertThatThrownBy {
                orderService.resendOrder(userId = 1L, orderId = 1L)
            }.isInstanceOf(InvalidOrderStatusException::class.java)
                .hasMessageContaining("전송실패 상태의 주문만 재전송할 수 있습니다")

            verify(orderRepository, never()).save(any())
        }

        @Test
        @DisplayName("PENDING 상태 - InvalidOrderStatusException")
        fun resendOrder_pendingStatus_throwsException() {
            // Given - 마감 전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.PENDING,
                isClosed = false
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When & Then
            assertThatThrownBy {
                orderService.resendOrder(userId = 1L, orderId = 1L)
            }.isInstanceOf(InvalidOrderStatusException::class.java)
                .hasMessageContaining("전송실패 상태의 주문만 재전송할 수 있습니다")

            verify(orderRepository, never()).save(any())
        }
    }

    // ========== cancelOrder Tests ==========

    @Nested
    @DisplayName("주문 취소 - cancelOrder")
    inner class CancelOrder {

        @Test
        @DisplayName("단일 제품 취소 성공 - isCancelled=true, cancelledAt 설정")
        fun cancelOrder_singleProduct_success() {
            // Given - 마감 전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = false
            )
            val item = createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5, isCancelled = false)
            val items = listOf(item)

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))

            // When
            val result = orderService.cancelOrder(userId = 1L, orderId = 1L, productCodes = listOf("P001"))

            // Then
            assertThat(result.cancelledCount).isEqualTo(1)
            assertThat(result.cancelledProductCodes).containsExactly("P001")
            assertThat(item.isCancelled).isTrue()
            assertThat(item.cancelledAt).isNotNull()
            assertThat(item.cancelledBy).isEqualTo("12345678")
            verify(orderItemRepository).saveAll(items)
        }

        @Test
        @DisplayName("복수 제품 취소 성공 - 3개 모두 취소")
        fun cancelOrder_multipleProducts_success() {
            // Given - 마감 전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = false
            )
            val item1 = createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5)
            val item2 = createTestOrderItem(2L, order, "P002", "제품B", 5.0, 10)
            val item3 = createTestOrderItem(3L, order, "P003", "제품C", 8.0, 3)
            val items = listOf(item1, item2, item3)

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))

            // When
            val result = orderService.cancelOrder(
                userId = 1L,
                orderId = 1L,
                productCodes = listOf("P001", "P002", "P003")
            )

            // Then
            assertThat(result.cancelledCount).isEqualTo(3)
            assertThat(result.cancelledProductCodes).containsExactlyInAnyOrder("P001", "P002", "P003")
            assertThat(item1.isCancelled).isTrue()
            assertThat(item2.isCancelled).isTrue()
            assertThat(item3.isCancelled).isTrue()
            verify(orderItemRepository).saveAll(items)
        }

        @Test
        @DisplayName("존재하지 않는 주문 - OrderNotFoundException")
        fun cancelOrder_orderNotFound_throwsException() {
            // Given
            val orderService = createOrderService()
            whenever(orderRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                orderService.cancelOrder(userId = 1L, orderId = 999L, productCodes = listOf("P001"))
            }.isInstanceOf(OrderNotFoundException::class.java)
                .hasMessageContaining("주문을 찾을 수 없습니다")
        }

        @Test
        @DisplayName("다른 사용자 주문 - ForbiddenOrderAccessException")
        fun cancelOrder_differentUser_throwsException() {
            // Given
            val orderService = createOrderService()
            val otherUser = User(
                id = 2L,
                employeeId = "87654321",
                password = "encoded",
                name = "김철수",
                orgName = "서울지점"
            )
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                user = otherUser
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When & Then
            assertThatThrownBy {
                orderService.cancelOrder(userId = 1L, orderId = 1L, productCodes = listOf("P001"))
            }.isInstanceOf(ForbiddenOrderAccessException::class.java)
                .hasMessageContaining("접근 권한이 없습니다")
        }

        @Test
        @DisplayName("마감 후 취소 시도 - OrderAlreadyClosedException")
        fun cancelOrder_orderClosed_throwsException() {
            // Given - 납기일(2026-02-04) 이후 시점의 Clock 사용
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = true
            )

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

            // When & Then
            assertThatThrownBy {
                orderService.cancelOrder(userId = 1L, orderId = 1L, productCodes = listOf("P001"))
            }.isInstanceOf(OrderAlreadyClosedException::class.java)
                .hasMessageContaining("마감된 주문은 취소할 수 없습니다")

            verify(orderItemRepository, never()).saveAll<OrderItem>(any())
        }

        @Test
        @DisplayName("이미 취소된 제품 포함 - AlreadyCancelledException")
        fun cancelOrder_alreadyCancelledProduct_throwsException() {
            // Given - 마감 전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = false
            )
            val item = createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5, isCancelled = true)
            val items = listOf(item)

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)

            // When & Then
            assertThatThrownBy {
                orderService.cancelOrder(userId = 1L, orderId = 1L, productCodes = listOf("P001"))
            }.isInstanceOf(AlreadyCancelledException::class.java)

            verify(orderItemRepository, never()).saveAll<OrderItem>(any())
        }

        @Test
        @DisplayName("주문에 없는 제품코드 - ProductNotInOrderException")
        fun cancelOrder_productNotInOrder_throwsException() {
            // Given - 마감 전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = false
            )
            val item = createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5)
            val items = listOf(item)

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)

            // When & Then
            assertThatThrownBy {
                orderService.cancelOrder(userId = 1L, orderId = 1L, productCodes = listOf("P999"))
            }.isInstanceOf(ProductNotInOrderException::class.java)

            verify(orderItemRepository, never()).saveAll<OrderItem>(any())
        }

        @Test
        @DisplayName("전체 롤백 검증 - 3개 중 1개 이미 취소, saveAll 호출 안됨")
        fun cancelOrder_partialAlreadyCancelled_rollbackAll() {
            // Given - 마감 전 시점으로 clock 고정
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val order = createTestOrder(
                id = 1L,
                orderRequestNumber = "OP00000001",
                approvalStatus = ApprovalStatus.APPROVED,
                isClosed = false
            )
            val item1 = createTestOrderItem(1L, order, "P001", "제품A", 10.0, 5, isCancelled = false)
            val item2 = createTestOrderItem(2L, order, "P002", "제품B", 5.0, 10, isCancelled = true) // 이미 취소됨
            val item3 = createTestOrderItem(3L, order, "P003", "제품C", 8.0, 3, isCancelled = false)
            val items = listOf(item1, item2, item3)

            whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))
            whenever(orderItemRepository.findByOrderId(1L)).thenReturn(items)

            // When & Then
            assertThatThrownBy {
                orderService.cancelOrder(
                    userId = 1L,
                    orderId = 1L,
                    productCodes = listOf("P001", "P002", "P003")
                )
            }.isInstanceOf(AlreadyCancelledException::class.java)

            // 롤백 검증 - saveAll이 호출되지 않음
            verify(orderItemRepository, never()).saveAll<OrderItem>(any())
        }
    }

    // ========== calculateIsClosed Tests ==========

    @Nested
    @DisplayName("마감 여부 계산 - calculateIsClosed")
    inner class CalculateIsClosed {

        @Test
        @DisplayName("납기일 이전 → false")
        fun calculateIsClosed_beforeDeliveryDate_returnsFalse() {
            // Given - 2026-02-03 10:00, 납기일 2026-02-04
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 3, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val deliveryDate = LocalDate.of(2026, 2, 4)

            // When
            val result = orderService.calculateIsClosed(deliveryDate, "14:00")

            // Then
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("납기일 이후 → true")
        fun calculateIsClosed_afterDeliveryDate_returnsTrue() {
            // Given - 2026-02-05 10:00, 납기일 2026-02-04
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val deliveryDate = LocalDate.of(2026, 2, 4)

            // When
            val result = orderService.calculateIsClosed(deliveryDate, "14:00")

            // Then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("납기일 당일, 마감시간 없음 → false")
        fun calculateIsClosed_onDeliveryDateNoDeadline_returnsFalse() {
            // Given - 2026-02-04 10:00, 납기일 2026-02-04, 마감시간 없음
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val deliveryDate = LocalDate.of(2026, 2, 4)

            // When
            val result = orderService.calculateIsClosed(deliveryDate, null)

            // Then
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("납기일 당일, 마감시간 전 (20분 이상 여유) → false")
        fun calculateIsClosed_onDeliveryDateBeforeCutoff_returnsFalse() {
            // Given - 2026-02-04 13:00, 납기일 2026-02-04, 마감시간 14:00 (cutoff 13:40)
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 13, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val deliveryDate = LocalDate.of(2026, 2, 4)

            // When
            val result = orderService.calculateIsClosed(deliveryDate, "14:00")

            // Then
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("납기일 당일, 마감시간 20분 전 정확히 → true")
        fun calculateIsClosed_onDeliveryDateAtCutoff_returnsTrue() {
            // Given - 2026-02-04 13:40, 납기일 2026-02-04, 마감시간 14:00 (cutoff 13:40)
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 13, 40).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val deliveryDate = LocalDate.of(2026, 2, 4)

            // When
            val result = orderService.calculateIsClosed(deliveryDate, "14:00")

            // Then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("납기일 당일, 마감시간 이후 → true")
        fun calculateIsClosed_onDeliveryDateAfterDeadline_returnsTrue() {
            // Given - 2026-02-04 14:30, 납기일 2026-02-04, 마감시간 14:00 (cutoff 13:40)
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 14, 30).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val deliveryDate = LocalDate.of(2026, 2, 4)

            // When
            val result = orderService.calculateIsClosed(deliveryDate, "14:00")

            // Then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("납기일 당일, 잘못된 마감시간 형식 → false")
        fun calculateIsClosed_onDeliveryDateInvalidDeadlineFormat_returnsFalse() {
            // Given - 2026-02-04 14:00, 납기일 2026-02-04, 잘못된 마감시간
            val fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 2, 4, 14, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
            )
            val orderService = createOrderService(fixedClock)
            val deliveryDate = LocalDate.of(2026, 2, 4)

            // When
            val result = orderService.calculateIsClosed(deliveryDate, "invalid")

            // Then
            assertThat(result).isFalse()
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun createTestOrder(
        id: Long,
        orderRequestNumber: String,
        approvalStatus: ApprovalStatus,
        totalAmount: Long = 100000L,
        isClosed: Boolean = false,
        clientDeadlineTime: String? = null,
        user: User = testUser
    ): Order {
        return Order(
            id = id,
            orderRequestNumber = orderRequestNumber,
            user = user,
            store = testStore,
            orderDate = LocalDate.of(2026, 2, 1),
            deliveryDate = LocalDate.of(2026, 2, 4),
            totalAmount = totalAmount,
            approvalStatus = approvalStatus,
            isClosed = isClosed,
            clientDeadlineTime = clientDeadlineTime
        )
    }

    private fun createTestOrderItem(
        id: Long,
        order: Order,
        productCode: String,
        productName: String,
        quantityBoxes: Double,
        quantityPieces: Int,
        isCancelled: Boolean = false
    ): OrderItem {
        return OrderItem(
            id = id,
            order = order,
            productCode = productCode,
            productName = productName,
            quantityBoxes = quantityBoxes,
            quantityPieces = quantityPieces,
            isCancelled = isCancelled
        )
    }

    private fun createTestProcessingRecord(
        id: Long,
        order: Order,
        sapOrderNumber: String,
        productCode: String,
        productName: String,
        deliveredQuantity: String,
        deliveryStatus: DeliveryStatus
    ): OrderProcessingRecord {
        return OrderProcessingRecord(
            id = id,
            order = order,
            sapOrderNumber = sapOrderNumber,
            productCode = productCode,
            productName = productName,
            deliveredQuantity = deliveredQuantity,
            deliveryStatus = deliveryStatus
        )
    }

    private fun createTestRejection(
        id: Long,
        order: Order,
        productCode: String,
        productName: String,
        orderQuantityBoxes: Int,
        rejectionReason: String
    ): OrderRejection {
        return OrderRejection(
            id = id,
            order = order,
            productCode = productCode,
            productName = productName,
            orderQuantityBoxes = orderQuantityBoxes,
            rejectionReason = rejectionReason
        )
    }
}
