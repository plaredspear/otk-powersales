package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestDetailSapSender
import com.otoki.powersales.external.sap.outbound.sender.SapOrderRequestDetailLine
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.InvalidDateRangeException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderParameterException
import com.otoki.powersales.domain.activity.order.exception.OrderDateRangeTooWideException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderHistoryRow
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.external.sap.outbox.SapOutboxRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@DisplayName("OrderRequestService 테스트")
class OrderRequestServiceTest {

    private val orderRequestRepository: OrderRequestRepository = mockk()
    private val orderRequestProductRepository: OrderRequestProductRepository = mockk()
    private val orderRequestDetailSapSender: OrderRequestDetailSapSender = mockk()
    private val orderRequestDetailMapper = OrderRequestDetailMapper()
    private val productRepository: ProductRepository = mockk()
    private val sapOutboxRepository: SapOutboxRepository = mockk()
    private val orderCancelReconciler: OrderCancelReconciler = mockk()

    private val fixedClock: Clock = Clock.fixed(
        LocalDateTime.of(2026, 5, 5, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
        ZoneId.of("Asia/Seoul"),
    )

    private val orderCancelPolicy =
        OrderCancelPolicy(OrderDeadlineCalculator(fixedClock), sapOutboxRepository)

    private lateinit var service: OrderRequestService

    @BeforeEach
    fun setUp() {
        service = OrderRequestService(
            orderRequestRepository,
            orderRequestProductRepository,
            orderRequestDetailSapSender,
            orderRequestDetailMapper,
            productRepository,
            orderCancelPolicy,
            orderCancelReconciler,
            fixedClock,
        )
        // 기본값 — 상세 조회 시 제품명 일괄조회. 개별 테스트에서 필요 시 override.
        every { productRepository.findByProductCodeIn(any()) } returns emptyList()
        // 기본값 — 등록 outbox in-flight 아님 (취소 가능 판정용).
        every {
            sapOutboxRepository.existsByDomainTypeAndAggregateIdAndStatusIn(any(), any(), any())
        } returns false
        // 기본값 — 정합 대상 없음. 개별 테스트에서 필요 시 override (#858).
        every { orderCancelReconciler.reconcileTimedOutCancels(any(), any()) } returns emptySet()
    }

    @Nested
    @DisplayName("getAccountOrderHistory - 거래처 주문이력")
    inner class AccountOrderHistoryTests {

        private val accountCode = "0001234567"
        private val from = LocalDate.of(2026, 5, 4)
        private val to = LocalDate.of(2026, 5, 6)

        @Test
        @DisplayName("정상 - 주문일 내림차순 그룹 + 그룹내 제품코드 중복제거 + EndDate +1일 적용")
        fun success() {
            every {
                orderRequestRepository.findOrderHistory(
                    employeeId = 1L,
                    accountCode = accountCode,
                    orderDateFrom = from.atStartOfDay(),
                    orderDateToExclusive = to.plusDays(1).atStartOfDay(),
                )
            } returns listOf(
                OrderHistoryRow(LocalDateTime.of(2026, 5, 6, 9, 30), "P001", "참깨라면"),
                OrderHistoryRow(LocalDateTime.of(2026, 5, 6, 14, 0), "P001", "참깨라면"), // 중복
                OrderHistoryRow(LocalDateTime.of(2026, 5, 6, 14, 0), "P002", "진라면"),
                OrderHistoryRow(LocalDateTime.of(2026, 5, 4, 11, 0), "P003", "열라면"),
            )

            val result = service.getAccountOrderHistory(1L, accountCode, from, to)

            assertThat(result).hasSize(2)
            assertThat(result[0].orderDate).isEqualTo("2026-05-06")
            assertThat(result[1].orderDate).isEqualTo("2026-05-04")
            // 제품코드 중복제거 (P001 1건)
            assertThat(result[0].products).hasSize(2)
            assertThat(result[0].products[0].productCode).isEqualTo("P001")
            assertThat(result[1].products).hasSize(1)
            assertThat(result[1].products[0].productCode).isEqualTo("P003")
        }

        @Test
        @DisplayName("정상 - 결과 없음 → 빈 목록")
        fun empty() {
            every { orderRequestRepository.findOrderHistory(any(), any(), any(), any()) } returns emptyList()

            assertThat(service.getAccountOrderHistory(1L, accountCode, from, to)).isEmpty()
        }

        @Test
        @DisplayName("실패 - 종료일이 시작일보다 빠르면 InvalidDateRangeException")
        fun invalidRange() {
            assertThatThrownBy {
                service.getAccountOrderHistory(1L, accountCode, to, from)
            }.isInstanceOf(InvalidDateRangeException::class.java)
        }
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
            every {
                orderRequestRepository.findMyOrderRequests(
                    any(), any(), any(), any(), any(), any(), any(), any(),
                )
            } returns listOf(record)

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
            every {
                orderRequestRepository.findMyOrderRequests(
                    any(), any(), any(), any(), any(), any(), any(), any(),
                )
            } returns records

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

    @Nested
    @DisplayName("getOrderRequestDetail - 본인 주문요청 상세 (#595)")
    inner class GetOrderRequestDetailTests {

        @Test
        @DisplayName("실패 — orderRequestId 가 0 이하 → ORD_INVALID_PARAM")
        fun invalidId() {
            assertThatThrownBy { service.getOrderRequestDetail(0L, userId = 1L) }
                .isInstanceOf(InvalidOrderParameterException::class.java)
        }

        @Test
        @DisplayName("실패 — 미존재 ID → ORD_NOT_FOUND")
        fun notFound() {
            every { orderRequestRepository.findById(eq(999L)) } returns Optional.empty()
            assertThatThrownBy { service.getOrderRequestDetail(999L, userId = 1L) }
                .isInstanceOf(OrderNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 — 본인 외 접근 → ORD_FORBIDDEN")
        fun forbidden() {
            val other = createOrderRequestWithEmployeeId(employeeId = 99L)
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(other)
            assertThatThrownBy { service.getOrderRequestDetail(100L, userId = 1L) }
                .isInstanceOf(ForbiddenOrderAccessException::class.java)
        }

        @Test
        @DisplayName("성공 — SAP 정상 + 마감 후 → orderProcessingStatusList 길이 1")
        fun successWithSapAfterClose() {
            val orderRequest = createOrderRequestWithEmployeeId(employeeId = 1L, deliveryDate = LocalDate.of(2026, 5, 4))
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(orderRequest)
            every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(100L) } returns
                listOf(buildCrmProduct("1000023", "진라면", 30, orderRequest))
            every { orderRequestDetailSapSender.fetchDetail(any()) } returns
                listOf(buildSapLine("1000023", "0300004993", "143000"))

            val response = service.getOrderRequestDetail(100L, userId = 1L)

            assertThat(response.isClosed).isTrue()
            assertThat(response.orderProcessingStatusList).hasSize(1)
            assertThat(response.orderProcessingStatusList!![0].sapOrderNumber).isEqualTo("0300004993")
            assertThat(response.orderedItems).hasSize(1)
            assertThat(response.orderedItems[0].productCode).isEqualTo("1000023")
        }

        @Test
        @DisplayName("성공 — 마감 전(isClosed=false) → orderProcessingStatusList = null (Q6, SAP 호출은 수행)")
        fun beforeCloseListNull() {
            val orderRequest = createOrderRequestWithEmployeeId(employeeId = 1L, deliveryDate = LocalDate.of(2026, 5, 10))
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(orderRequest)
            every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(100L) } returns
                listOf(buildCrmProduct("1000023", "진라면", 30, orderRequest))
            every { orderRequestDetailSapSender.fetchDetail(any()) } returns
                listOf(buildSapLine("1000023", "0300004993", "143000"))

            val response = service.getOrderRequestDetail(100L, userId = 1L)

            assertThat(response.isClosed).isFalse()
            assertThat(response.orderProcessingStatusList).isNull()
            // SAP 호출은 수행되었어야 함
            verify { orderRequestDetailSapSender.fetchDetail(any()) }
        }

        @Test
        @DisplayName("성공 — 마감 전 + SD03052 전 라인 납품완료 → cancelable=false (완료 건 취소 버튼 비활성화)")
        fun fullyDeliveredDisablesCancel() {
            // 마감 전(취소 가능 시점)이고 APPROVED 라 원래는 cancelable=true 지만, SAP 응답상 전 라인
            // 납품완료(CompleteTime 채워짐)면 취소 버튼을 내린다.
            val orderRequest = createOrderRequestWithEmployeeId(employeeId = 1L, deliveryDate = LocalDate.of(2026, 5, 10))
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(orderRequest)
            every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(100L) } returns
                listOf(buildCrmProduct("1000023", "진라면", 30, orderRequest))
            every { orderRequestDetailSapSender.fetchDetail(any()) } returns
                listOf(buildSapLine("1000023", "0300004993", "143000"))

            val response = service.getOrderRequestDetail(100L, userId = 1L)

            assertThat(response.isClosed).isFalse() // 마감 전
            assertThat(response.cancelable).isFalse() // 납품완료라 취소 버튼 비활성화
        }

        @Test
        @DisplayName("성공 — 마감 전 + SD03052 미납품(CompleteTime 없음) → cancelable=true (정상 취소 가능)")
        fun notFullyDeliveredKeepsCancel() {
            val orderRequest = createOrderRequestWithEmployeeId(employeeId = 1L, deliveryDate = LocalDate.of(2026, 5, 10))
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(orderRequest)
            every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(100L) } returns
                listOf(buildCrmProduct("1000023", "진라면", 30, orderRequest))
            // CompleteTime '000000' (미납품) → 납품완료 아님
            every { orderRequestDetailSapSender.fetchDetail(any()) } returns
                listOf(buildSapLine("1000023", "0300004993", "000000"))

            val response = service.getOrderRequestDetail(100L, userId = 1L)

            assertThat(response.isClosed).isFalse()
            assertThat(response.cancelable).isTrue() // 미납품이라 취소 가능 유지
        }

        @Test
        @DisplayName("성공 — SAP null 반환 → orderProcessingStatusList = null, rejectedItems = null, 200 유지")
        fun sapFailureFallback() {
            val orderRequest = createOrderRequestWithEmployeeId(employeeId = 1L, deliveryDate = LocalDate.of(2026, 5, 4))
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(orderRequest)
            every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(100L) } returns emptyList()
            every { orderRequestDetailSapSender.fetchDetail(any()) } returns null

            val response = service.getOrderRequestDetail(100L, userId = 1L)

            assertThat(response.orderProcessingStatusList).isNull()
            assertThat(response.rejectedItems).isNull()
        }

        @Test
        @DisplayName("성공 — 라인 product FK 가 null 이어도 product_code 로 제품명 복구 (레거시 CRM_ProductName 동등)")
        fun productNameResolvedByCode() {
            val orderRequest = createOrderRequestWithEmployeeId(employeeId = 1L, deliveryDate = LocalDate.of(2026, 5, 10))
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(orderRequest)
            // product FK 미설정 라인 (주문 등록 시 product_id 누락 시나리오)
            every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(100L) } returns
                listOf(buildCrmProductWithoutFk("19310235", orderRequest))
            every { orderRequestDetailSapSender.fetchDetail(any()) } returns null
            every { productRepository.findByProductCodeIn(listOf("19310235")) } returns
                listOf(Product(id = 9L, productCode = "19310235", name = "오뚜기밥"))

            val response = service.getOrderRequestDetail(100L, userId = 1L)

            assertThat(response.orderedItems).hasSize(1)
            assertThat(response.orderedItems[0].productCode).isEqualTo("19310235")
            assertThat(response.orderedItems[0].productName).isEqualTo("오뚜기밥")
        }

        @Test
        @DisplayName("성공 — 정합 승격된 라인(#858)은 응답 orderedItems.isCancelled=true 로 반영")
        fun reconciledLineReflectedAsCancelled() {
            // SAP DefaultReason 으로 취소가 반영됐고 정합 컴포넌트가 해당 productCode 를 승격했다고 가정.
            val orderRequest = createOrderRequestWithEmployeeId(employeeId = 1L, deliveryDate = LocalDate.of(2026, 5, 10))
            every { orderRequestRepository.findById(eq(100L)) } returns Optional.of(orderRequest)
            every { orderRequestProductRepository.findByOrderRequest_IdOrderByLineNumberAsc(100L) } returns
                listOf(buildCrmProduct("1000023", "진라면", 30, orderRequest))
            // SAP 응답에 DefaultReason(취소 반영) 채워진 라인.
            every { orderRequestDetailSapSender.fetchDetail(any()) } returns
                listOf(buildSapLineWithDefaultReason("1000023", "S1"))
            // 정합 컴포넌트가 해당 productCode 를 승격했다고 stub.
            every { orderCancelReconciler.reconcileTimedOutCancels(100L, setOf("1000023")) } returns setOf("1000023")

            val response = service.getOrderRequestDetail(100L, userId = 1L)

            val item = response.orderedItems.first { it.productCode == "1000023" }
            assertThat(item.isCancelled).isTrue() // 정합 승격 반영 (조회 엔티티는 미반영이나 forceCancelled)
            assertThat(item.isOutOfStock).isTrue() // DefaultReason 존재로 결품 플래그도 여전히 true
        }
    }

    private fun createOrderRequest(
        id: Long = 1L,
        accountId: Long = 1,
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

    private fun createOrderRequestWithEmployeeId(
        employeeId: Long,
        deliveryDate: LocalDate = LocalDate.of(2026, 5, 6),
    ): OrderRequest {
        val account = Account(id = 5, name = "ABC")
        val employee = Employee(id = employeeId, employeeCode = "20030117", name = "Test")
        return OrderRequest(
            id = 100L,
            orderRequestNumber = "OR-0000100",
            orderDate = LocalDateTime.of(2026, 5, 4, 10, 0),
            deliveryDate = deliveryDate,
            totalAmount = BigDecimal("1234567.00"),
            orderRequestStatus = OrderRequestStatus.APPROVED,
            employee = employee,
            account = account,
        )
    }

    private fun buildCrmProduct(
        productCode: String,
        productName: String,
        piecesPerBox: Int,
        orderRequest: OrderRequest,
    ): OrderRequestProduct =
        OrderRequestProduct(
            id = 1L,
            lineNumber = BigDecimal.valueOf(1L),
            productCode = productCode,
            quantityBoxes = BigDecimal("10"),
            quantityPieces = BigDecimal.valueOf(0L),
            unit = "BOX",
            unitPrice = BigDecimal.ZERO,
            amount = BigDecimal.ZERO,
            piecesPerBox = piecesPerBox,
            orderRequest = orderRequest,
            product = Product(
                id = 1L,
                productCode = productCode,
                name = productName,
            ),
        )

    /** product FK 미설정 (product_id 누락) 라인. */
    private fun buildCrmProductWithoutFk(
        productCode: String,
        orderRequest: OrderRequest,
    ): OrderRequestProduct =
        OrderRequestProduct(
            id = 1L,
            lineNumber = BigDecimal.valueOf(1L),
            productCode = productCode,
            quantityBoxes = BigDecimal("10"),
            quantityPieces = BigDecimal.valueOf(0L),
            unit = "BOX",
            unitPrice = BigDecimal.ZERO,
            amount = BigDecimal.ZERO,
            piecesPerBox = 1,
            orderRequest = orderRequest,
            product = null,
        )

    private fun buildSapLine(
        productCode: String,
        sapOrderNumber: String,
        completeTime: String,
    ): SapOrderRequestDetailLine =
        SapOrderRequestDetailLine(
            lineNumber = "00001",
            productCode = productCode,
            productName = "name",
            lineItemStatus = "OK",
            totalQuantity = "10",
            unit = "BOX",
            sapOrderNumber = sapOrderNumber,
            orderSalesAmount = "120000",
            deliveryRequestDate = "20260506",
            orderDate = "20260504",
            shippingDriverName = "홍길동",
            shippingVehicle = "12가3456",
            shippingDriverPhone = "010-1234-5678",
            shippingScheduleTime = "120000",
            shippingCompleteTime = completeTime,
            totalQuantityBox = "10",
            shippingQuantityBox = "10",
            defaultReason = "",
        )

    /** DefaultReason(취소 반영) 채워진 SAP 라인 — #858 정합 대상. */
    private fun buildSapLineWithDefaultReason(
        productCode: String,
        defaultReason: String,
    ): SapOrderRequestDetailLine =
        SapOrderRequestDetailLine(
            lineNumber = "00001",
            productCode = productCode,
            productName = "name",
            lineItemStatus = "",
            totalQuantity = "10",
            unit = "BOX",
            sapOrderNumber = "",
            orderSalesAmount = "120000",
            deliveryRequestDate = "20260506",
            orderDate = "20260504",
            shippingDriverName = "",
            shippingVehicle = "",
            shippingDriverPhone = "",
            shippingScheduleTime = "000000",
            shippingCompleteTime = "000000",
            totalQuantityBox = "10",
            shippingQuantityBox = "10",
            defaultReason = defaultReason,
        )
}
