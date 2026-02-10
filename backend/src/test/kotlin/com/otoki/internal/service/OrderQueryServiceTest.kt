package com.otoki.internal.service

import com.otoki.internal.entity.Product
import com.otoki.internal.entity.Store
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.InvalidOrderDateRangeException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.repository.OrderItemRepository
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.StoreRepository
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
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderQueryService 테스트")
class OrderQueryServiceTest {

    @Mock
    private lateinit var orderItemRepository: OrderItemRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @InjectMocks
    private lateinit var orderQueryService: OrderQueryService

    // --- getOrderHistoryProducts tests ---

    @Nested
    @DisplayName("getOrderHistoryProducts")
    inner class GetOrderHistoryProductsTests {

        @Test
        @DisplayName("주문이력 제품을 정상 조회한다")
        fun getOrderHistoryProducts_success() {
            // given
            val userId = 1L
            val today = LocalDate.now()
            val row = arrayOf<Any>(
                "P001",
                "갈릭 아이올리소스 240g",
                "8801045570716",
                "냉장",
                "소스",
                "양념소스",
                today.minusDays(1),
                3L
            )

            whenever(orderItemRepository.findOrderHistoryProducts(eq(userId), any(), any(), any()))
                .thenReturn(listOf(row))
            whenever(orderItemRepository.countOrderHistoryProducts(eq(userId), any(), any()))
                .thenReturn(1L)

            // when
            val result = orderQueryService.getOrderHistoryProducts(userId, null, null, null, null)

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productCode).isEqualTo("P001")
            assertThat(result.content[0].productName).isEqualTo("갈릭 아이올리소스 240g")
            assertThat(result.content[0].totalOrderCount).isEqualTo(3)
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("빈 결과를 반환한다")
        fun getOrderHistoryProducts_empty() {
            // given
            whenever(orderItemRepository.findOrderHistoryProducts(eq(1L), any(), any(), any()))
                .thenReturn(emptyList())
            whenever(orderItemRepository.countOrderHistoryProducts(eq(1L), any(), any()))
                .thenReturn(0L)

            // when
            val result = orderQueryService.getOrderHistoryProducts(1L, null, null, null, null)

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("날짜 범위가 역전되면 InvalidOrderDateRangeException을 던진다")
        fun getOrderHistoryProducts_invalidDateRange_throwsException() {
            // when & then
            assertThatThrownBy {
                orderQueryService.getOrderHistoryProducts(
                    1L,
                    LocalDate.of(2026, 2, 10),
                    LocalDate.of(2026, 2, 5),
                    null,
                    null
                )
            }.isInstanceOf(InvalidOrderDateRangeException::class.java)
        }

        @Test
        @DisplayName("잘못된 페이지 번호면 InvalidOrderParameterException을 던진다")
        fun getOrderHistoryProducts_invalidPage_throwsException() {
            // when & then
            assertThatThrownBy {
                orderQueryService.getOrderHistoryProducts(1L, null, null, -1, null)
            }.isInstanceOf(InvalidOrderParameterException::class.java)
        }
    }

    // --- getClientCreditBalance tests ---

    @Nested
    @DisplayName("getClientCreditBalance")
    inner class GetClientCreditBalanceTests {

        @Test
        @DisplayName("여신잔액을 정상 조회한다")
        fun getClientCreditBalance_success() {
            // given
            val store = Store(
                id = 1L,
                storeCode = "S001",
                storeName = "롯데마트 응암점",
                creditLimit = 100_000_000,
                usedCredit = 45_000_000,
                creditUpdatedAt = LocalDateTime.of(2026, 2, 10, 10, 0)
            )
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))

            // when
            val result = orderQueryService.getClientCreditBalance(1L)

            // then
            assertThat(result.clientId).isEqualTo(1L)
            assertThat(result.clientName).isEqualTo("롯데마트 응암점")
            assertThat(result.creditLimit).isEqualTo(100_000_000)
            assertThat(result.usedCredit).isEqualTo(45_000_000)
            assertThat(result.availableCredit).isEqualTo(55_000_000)
            assertThat(result.lastUpdatedAt).isEqualTo("2026-02-10T10:00:00")
        }

        @Test
        @DisplayName("거래처가 없으면 ClientNotFoundException을 던진다")
        fun getClientCreditBalance_notFound_throwsException() {
            // given
            whenever(storeRepository.findById(999L)).thenReturn(Optional.empty())

            // when & then
            assertThatThrownBy {
                orderQueryService.getClientCreditBalance(999L)
            }.isInstanceOf(ClientNotFoundException::class.java)
        }

        @Test
        @DisplayName("creditUpdatedAt이 null이면 lastUpdatedAt도 null을 반환한다")
        fun getClientCreditBalance_nullUpdatedAt() {
            // given
            val store = Store(
                id = 1L,
                storeCode = "S001",
                storeName = "롯데마트 응암점",
                creditLimit = 100_000_000,
                usedCredit = 0,
                creditUpdatedAt = null
            )
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))

            // when
            val result = orderQueryService.getClientCreditBalance(1L)

            // then
            assertThat(result.lastUpdatedAt).isNull()
            assertThat(result.availableCredit).isEqualTo(100_000_000)
        }
    }

    // --- getProductOrderInfo tests ---

    @Nested
    @DisplayName("getProductOrderInfo")
    inner class GetProductOrderInfoTests {

        @Test
        @DisplayName("제품 주문정보를 정상 조회한다")
        fun getProductOrderInfo_success() {
            // given
            val product = Product(
                id = 1L,
                productId = "01101123",
                productName = "갈릭 아이올리소스 240g",
                productCode = "01101123",
                barcode = "8801045570716",
                storageType = "냉장",
                categoryMid = "소스",
                categorySub = "양념소스",
                piecesPerBox = 50,
                minOrderUnit = 10,
                supplyQuantity = 1000,
                dcQuantity = 500,
                unitPrice = 5000
            )
            whenever(productRepository.findByProductCode("01101123")).thenReturn(product)

            // when
            val result = orderQueryService.getProductOrderInfo("01101123")

            // then
            assertThat(result.productCode).isEqualTo("01101123")
            assertThat(result.productName).isEqualTo("갈릭 아이올리소스 240g")
            assertThat(result.piecesPerBox).isEqualTo(50)
            assertThat(result.minOrderUnit).isEqualTo(10)
            assertThat(result.supplyQuantity).isEqualTo(1000)
            assertThat(result.dcQuantity).isEqualTo(500)
            assertThat(result.unitPrice).isEqualTo(5000)
        }

        @Test
        @DisplayName("제품이 없으면 ProductNotFoundException을 던진다")
        fun getProductOrderInfo_notFound_throwsException() {
            // given
            whenever(productRepository.findByProductCode("INVALID")).thenReturn(null)

            // when & then
            assertThatThrownBy {
                orderQueryService.getProductOrderInfo("INVALID")
            }.isInstanceOf(ProductNotFoundException::class.java)
        }
    }
}
