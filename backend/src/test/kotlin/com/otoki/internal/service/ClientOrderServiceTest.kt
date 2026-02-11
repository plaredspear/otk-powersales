package com.otoki.internal.service

import com.otoki.internal.dto.response.ClientOrderDetailResponse
import com.otoki.internal.dto.response.ClientOrderSummaryResponse
import com.otoki.internal.entity.*
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.ForbiddenClientAccessException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.OrderNotFoundException
import com.otoki.internal.repository.OrderProcessingRecordRepository
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.StoreScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
@DisplayName("ClientOrderService 테스트")
class ClientOrderServiceTest {

    @Mock
    private lateinit var orderProcessingRecordRepository: OrderProcessingRecordRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var storeScheduleRepository: StoreScheduleRepository

    private lateinit var clientOrderService: ClientOrderService

    private val testUserId = 1L
    private val testClientId = 100L
    private val testSapOrderNumber = "SAP-2024-001"
    private val testDeliveryDate = LocalDate.of(2024, 1, 15)

    @BeforeEach
    fun setUp() {
        clientOrderService = ClientOrderService(
            orderProcessingRecordRepository = orderProcessingRecordRepository,
            storeRepository = storeRepository,
            storeScheduleRepository = storeScheduleRepository
        )
    }

    @Test
    @DisplayName("기본 조회 성공 - clientId + 오늘 납기일 기준 조회")
    fun getClientOrders_defaultParameters_success() {
        // Given
        val today = LocalDate.now()
        val yearMonth = YearMonth.from(today)
        val summaries = listOf(
            arrayOf<Any>(testSapOrderNumber, testClientId, "테스트 거래처", 1000000L),
            arrayOf<Any>("SAP-2024-002", testClientId, "테스트 거래처", 500000L)
        )

        whenever(storeRepository.existsById(testClientId)).thenReturn(true)
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(testClientId))
        whenever(
            orderProcessingRecordRepository.findClientOrderSummaries(
                storeId = eq(testClientId),
                deliveryDate = eq(today)
            )
        ).thenReturn(summaries)

        // When
        val result = clientOrderService.getClientOrders(
            userId = testUserId,
            clientId = testClientId,
            deliveryDate = null,
            page = null,
            size = null
        )

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.number).isEqualTo(0)
        assertThat(result.size).isEqualTo(20) // DEFAULT_PAGE_SIZE
        assertThat(result.content[0].sapOrderNumber).isEqualTo(testSapOrderNumber)
        assertThat(result.content[0].clientId).isEqualTo(testClientId)
        assertThat(result.content[0].clientName).isEqualTo("테스트 거래처")
        assertThat(result.content[0].totalAmount).isEqualTo(1000000L)

        verify(storeRepository).existsById(testClientId)
        verify(orderProcessingRecordRepository).findClientOrderSummaries(
            storeId = testClientId,
            deliveryDate = today
        )
    }

    @Test
    @DisplayName("납기일 지정 조회 성공")
    fun getClientOrders_withSpecificDeliveryDate_success() {
        // Given
        val specificDate = LocalDate.of(2024, 2, 20)
        val yearMonth = YearMonth.from(LocalDate.now())
        val summaries = listOf(
            arrayOf<Any>(testSapOrderNumber, testClientId, "테스트 거래처", 2000000L)
        )

        whenever(storeRepository.existsById(testClientId)).thenReturn(true)
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(testClientId))
        whenever(
            orderProcessingRecordRepository.findClientOrderSummaries(
                storeId = eq(testClientId),
                deliveryDate = eq(specificDate)
            )
        ).thenReturn(summaries)

        // When
        val result = clientOrderService.getClientOrders(
            userId = testUserId,
            clientId = testClientId,
            deliveryDate = specificDate,
            page = 0,
            size = 20
        )

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].sapOrderNumber).isEqualTo(testSapOrderNumber)
        verify(orderProcessingRecordRepository).findClientOrderSummaries(
            storeId = testClientId,
            deliveryDate = specificDate
        )
    }

    @Test
    @DisplayName("페이지네이션 동작 - 2페이지, 10건")
    fun getClientOrders_withPagination_success() {
        // Given
        val yearMonth = YearMonth.from(LocalDate.now())
        val summaries = (1..25).map { index ->
            arrayOf<Any>("SAP-2024-${index.toString().padStart(3, '0')}", testClientId, "테스트 거래처", 100000L * index)
        }

        whenever(storeRepository.existsById(testClientId)).thenReturn(true)
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(testClientId))
        whenever(
            orderProcessingRecordRepository.findClientOrderSummaries(
                storeId = eq(testClientId),
                deliveryDate = any()
            )
        ).thenReturn(summaries)

        // When
        val result = clientOrderService.getClientOrders(
            userId = testUserId,
            clientId = testClientId,
            deliveryDate = testDeliveryDate,
            page = 2,
            size = 10
        )

        // Then
        assertThat(result.content).hasSize(5) // 3rd page (0-indexed page 2) has remaining 5 items
        assertThat(result.totalElements).isEqualTo(25)
        assertThat(result.totalPages).isEqualTo(3)
        assertThat(result.number).isEqualTo(2)
        assertThat(result.size).isEqualTo(10)
        assertThat(result.content[0].sapOrderNumber).isEqualTo("SAP-2024-021")
    }

    @Test
    @DisplayName("빈 결과 - totalElements=0")
    fun getClientOrders_emptyResult_success() {
        // Given
        val yearMonth = YearMonth.from(LocalDate.now())
        whenever(storeRepository.existsById(testClientId)).thenReturn(true)
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(testClientId))
        whenever(
            orderProcessingRecordRepository.findClientOrderSummaries(
                storeId = eq(testClientId),
                deliveryDate = any()
            )
        ).thenReturn(emptyList())

        // When
        val result = clientOrderService.getClientOrders(
            userId = testUserId,
            clientId = testClientId,
            deliveryDate = testDeliveryDate,
            page = 0,
            size = 20
        )

        // Then
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
        assertThat(result.totalPages).isEqualTo(0)
    }

    @Test
    @DisplayName("거래처 존재하지 않음 - ClientNotFoundException")
    fun getClientOrders_clientNotFound_throwsException() {
        // Given
        whenever(storeRepository.existsById(testClientId)).thenReturn(false)

        // When & Then
        assertThatThrownBy {
            clientOrderService.getClientOrders(
                userId = testUserId,
                clientId = testClientId,
                deliveryDate = testDeliveryDate,
                page = 0,
                size = 20
            )
        }.isInstanceOf(ClientNotFoundException::class.java)

        verify(storeRepository).existsById(testClientId)
    }

    @Test
    @DisplayName("거래처 접근 권한 없음 - ForbiddenClientAccessException")
    fun getClientOrders_forbiddenAccess_throwsException() {
        // Given
        val yearMonth = YearMonth.from(LocalDate.now())
        whenever(storeRepository.existsById(testClientId)).thenReturn(true)
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(200L, 300L)) // testClientId not included

        // When & Then
        assertThatThrownBy {
            clientOrderService.getClientOrders(
                userId = testUserId,
                clientId = testClientId,
                deliveryDate = testDeliveryDate,
                page = 0,
                size = 20
            )
        }.isInstanceOf(ForbiddenClientAccessException::class.java)
    }

    @Test
    @DisplayName("음수 페이지 번호 - InvalidOrderParameterException")
    fun getClientOrders_negativePageNumber_throwsException() {
        // When & Then
        assertThatThrownBy {
            clientOrderService.getClientOrders(
                userId = testUserId,
                clientId = testClientId,
                deliveryDate = testDeliveryDate,
                page = -1,
                size = 20
            )
        }.isInstanceOf(InvalidOrderParameterException::class.java)
            .hasMessageContaining("페이지 번호는 0 이상이어야 합니다")
    }

    @Test
    @DisplayName("페이지 크기 0 - InvalidOrderParameterException")
    fun getClientOrders_pageSizeZero_throwsException() {
        // When & Then
        assertThatThrownBy {
            clientOrderService.getClientOrders(
                userId = testUserId,
                clientId = testClientId,
                deliveryDate = testDeliveryDate,
                page = 0,
                size = 0
            )
        }.isInstanceOf(InvalidOrderParameterException::class.java)
            .hasMessageContaining("페이지 크기는 1~100 범위여야 합니다")
    }

    @Test
    @DisplayName("페이지 크기 101 - InvalidOrderParameterException")
    fun getClientOrders_pageSizeExceedsMax_throwsException() {
        // When & Then
        assertThatThrownBy {
            clientOrderService.getClientOrders(
                userId = testUserId,
                clientId = testClientId,
                deliveryDate = testDeliveryDate,
                page = 0,
                size = 101
            )
        }.isInstanceOf(InvalidOrderParameterException::class.java)
            .hasMessageContaining("페이지 크기는 1~100 범위여야 합니다")
    }

    @Test
    @DisplayName("정상 조회 성공 - 제품 목록 포함")
    fun getClientOrderDetail_success() {
        // Given
        val yearMonth = YearMonth.from(LocalDate.now())
        val store = Store(
            id = testClientId,
            storeCode = "ST001",
            storeName = "테스트 거래처",
            address = "서울시 강남구",
            representativeName = "홍길동",
            phoneNumber = "02-1234-5678",
            creditLimit = 10000000L,
            usedCredit = 5000000L
        )
        val user = User(
            id = testUserId,
            employeeId = "EMP001",
            password = "password",
            name = "테스트 사용자",
            department = "영업팀",
            branchName = "서울지점",
            role = UserRole.USER,
            workerType = WorkerType.PATROL
        )
        val order = Order(
            id = 1L,
            orderRequestNumber = "ORD-001",
            user = user,
            store = store,
            orderDate = LocalDate.of(2024, 1, 10),
            deliveryDate = LocalDate.of(2024, 1, 15),
            totalAmount = 1500000L,
            totalApprovedAmount = 1500000L,
            approvalStatus = ApprovalStatus.APPROVED,
            isClosed = false,
            clientDeadlineTime = "14:00"
        )
        val records = listOf(
            OrderProcessingRecord(
                id = 1L,
                order = order,
                sapOrderNumber = testSapOrderNumber,
                productCode = "PROD001",
                productName = "진라면 순한맛",
                deliveredQuantity = "100",
                deliveryStatus = DeliveryStatus.DELIVERED
            ),
            OrderProcessingRecord(
                id = 2L,
                order = order,
                sapOrderNumber = testSapOrderNumber,
                productCode = "PROD002",
                productName = "진라면 매운맛",
                deliveredQuantity = "50",
                deliveryStatus = DeliveryStatus.SHIPPING
            )
        )

        whenever(orderProcessingRecordRepository.findBySapOrderNumber(testSapOrderNumber))
            .thenReturn(records)
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(testClientId))

        // When
        val result = clientOrderService.getClientOrderDetail(
            userId = testUserId,
            sapOrderNumber = testSapOrderNumber
        )

        // Then
        assertThat(result.sapOrderNumber).isEqualTo(testSapOrderNumber)
        assertThat(result.clientId).isEqualTo(testClientId)
        assertThat(result.clientName).isEqualTo("테스트 거래처")
        assertThat(result.clientDeadlineTime).isEqualTo("14:00")
        assertThat(result.orderDate).isEqualTo("2024-01-10")
        assertThat(result.deliveryDate).isEqualTo("2024-01-15")
        assertThat(result.totalApprovedAmount).isEqualTo(1500000L)
        assertThat(result.orderedItemCount).isEqualTo(2)
        assertThat(result.orderedItems).hasSize(2)
        assertThat(result.orderedItems[0].productCode).isEqualTo("PROD001")
        assertThat(result.orderedItems[0].productName).isEqualTo("진라면 순한맛")
        assertThat(result.orderedItems[0].deliveredQuantity).isEqualTo("100")
        assertThat(result.orderedItems[0].deliveryStatus).isEqualTo("DELIVERED")
        assertThat(result.orderedItems[1].productCode).isEqualTo("PROD002")
        assertThat(result.orderedItems[1].deliveryStatus).isEqualTo("SHIPPING")

        verify(orderProcessingRecordRepository).findBySapOrderNumber(testSapOrderNumber)
    }

    @Test
    @DisplayName("존재하지 않는 SAP 주문번호 - OrderNotFoundException")
    fun getClientOrderDetail_orderNotFound_throwsException() {
        // Given
        whenever(orderProcessingRecordRepository.findBySapOrderNumber(testSapOrderNumber))
            .thenReturn(emptyList())

        // When & Then
        assertThatThrownBy {
            clientOrderService.getClientOrderDetail(
                userId = testUserId,
                sapOrderNumber = testSapOrderNumber
            )
        }.isInstanceOf(OrderNotFoundException::class.java)

        verify(orderProcessingRecordRepository).findBySapOrderNumber(testSapOrderNumber)
    }

    @Test
    @DisplayName("접근 권한 없음 - ForbiddenClientAccessException")
    fun getClientOrderDetail_forbiddenAccess_throwsException() {
        // Given
        val yearMonth = YearMonth.from(LocalDate.now())
        val store = Store(
            id = testClientId,
            storeCode = "ST001",
            storeName = "테스트 거래처"
        )
        val user = User(
            id = testUserId,
            employeeId = "EMP001",
            password = "password",
            name = "테스트 사용자",
            department = "영업팀",
            branchName = "서울지점"
        )
        val order = Order(
            id = 1L,
            orderRequestNumber = "ORD-001",
            user = user,
            store = store,
            orderDate = LocalDate.of(2024, 1, 10),
            deliveryDate = LocalDate.of(2024, 1, 15),
            totalAmount = 1000000L
        )
        val record = OrderProcessingRecord(
            id = 1L,
            order = order,
            sapOrderNumber = testSapOrderNumber,
            productCode = "PROD001",
            productName = "진라면",
            deliveredQuantity = "100"
        )

        whenever(orderProcessingRecordRepository.findBySapOrderNumber(testSapOrderNumber))
            .thenReturn(listOf(record))
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(200L, 300L)) // testClientId not included

        // When & Then
        assertThatThrownBy {
            clientOrderService.getClientOrderDetail(
                userId = testUserId,
                sapOrderNumber = testSapOrderNumber
            )
        }.isInstanceOf(ForbiddenClientAccessException::class.java)
    }

    @Test
    @DisplayName("DTO 매핑 검증 - 모든 필드 정확한지 확인")
    fun getClientOrderDetail_dtoMapping_allFieldsCorrect() {
        // Given
        val yearMonth = YearMonth.from(LocalDate.now())
        val expectedOrderDate = LocalDate.of(2024, 3, 1)
        val expectedDeliveryDate = LocalDate.of(2024, 3, 5)
        val store = Store(
            id = testClientId,
            storeCode = "ST999",
            storeName = "매핑 테스트 거래처",
            address = "부산시 해운대구",
            representativeName = "김철수",
            phoneNumber = "051-9999-8888",
            creditLimit = 20000000L,
            usedCredit = 10000000L
        )
        val user = User(
            id = testUserId,
            employeeId = "EMP999",
            password = "password",
            name = "매핑 테스트 사용자",
            department = "영업2팀",
            branchName = "부산지점",
            role = UserRole.ADMIN,
            workerType = WorkerType.PATROL
        )
        val order = Order(
            id = 999L,
            orderRequestNumber = "ORD-999",
            user = user,
            store = store,
            orderDate = expectedOrderDate,
            deliveryDate = expectedDeliveryDate,
            totalAmount = 3000000L,
            totalApprovedAmount = 2800000L,
            approvalStatus = ApprovalStatus.APPROVED,
            isClosed = true,
            clientDeadlineTime = "16:30"
        )
        val record = OrderProcessingRecord(
            id = 999L,
            order = order,
            sapOrderNumber = testSapOrderNumber,
            productCode = "PROD999",
            productName = "매핑 테스트 제품",
            deliveredQuantity = "999",
            deliveryStatus = DeliveryStatus.WAITING
        )

        whenever(orderProcessingRecordRepository.findBySapOrderNumber(testSapOrderNumber))
            .thenReturn(listOf(record))
        whenever(
            storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(
                eq(testUserId),
                eq(yearMonth.atDay(1)),
                eq(yearMonth.atEndOfMonth())
            )
        ).thenReturn(listOf(testClientId))

        // When
        val result = clientOrderService.getClientOrderDetail(
            userId = testUserId,
            sapOrderNumber = testSapOrderNumber
        )

        // Then - 모든 필드 매핑 검증
        assertThat(result).isNotNull
        assertThat(result.sapOrderNumber).isEqualTo(testSapOrderNumber)
        assertThat(result.clientId).isEqualTo(testClientId)
        assertThat(result.clientName).isEqualTo("매핑 테스트 거래처")
        assertThat(result.clientDeadlineTime).isEqualTo("16:30")
        assertThat(result.orderDate).isEqualTo("2024-03-01")
        assertThat(result.deliveryDate).isEqualTo("2024-03-05")
        assertThat(result.totalApprovedAmount).isEqualTo(2800000L)
        assertThat(result.orderedItemCount).isEqualTo(1)
        assertThat(result.orderedItems).hasSize(1)

        val item = result.orderedItems[0]
        assertThat(item.productCode).isEqualTo("PROD999")
        assertThat(item.productName).isEqualTo("매핑 테스트 제품")
        assertThat(item.deliveredQuantity).isEqualTo("999")
        assertThat(item.deliveryStatus).isEqualTo("WAITING")
    }
}
