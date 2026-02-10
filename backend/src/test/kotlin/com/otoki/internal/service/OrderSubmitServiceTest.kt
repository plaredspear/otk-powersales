package com.otoki.internal.service

import com.otoki.internal.dto.request.DraftItemRequest
import com.otoki.internal.dto.request.OrderDraftRequest
import com.otoki.internal.entity.*
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.InvalidDeliveryDateException
import com.otoki.internal.exception.OrderValidationFailedException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.integration.SapOrderClient
import com.otoki.internal.integration.SapOrderResult
import com.otoki.internal.repository.*
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderSubmitService 테스트")
class OrderSubmitServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var orderItemRepository: OrderItemRepository

    @Mock
    private lateinit var orderDraftRepository: OrderDraftRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var sapOrderClient: SapOrderClient

    @InjectMocks
    private lateinit var orderSubmitService: OrderSubmitService

    private val testUserId = 1L

    private fun createTestUser(id: Long = 1L): User {
        return User(
            id = id,
            employeeId = "20010585",
            password = "encodedPassword",
            name = "홍길동",
            department = "영업1팀",
            branchName = "서울지점"
        )
    }

    private fun createTestStore(id: Long = 1L): Store {
        return Store(
            id = id,
            storeCode = "S001",
            storeName = "롯데마트 응암점",
            creditLimit = 100_000_000,
            usedCredit = 45_000_000
        )
    }

    private fun createTestProduct(
        productCode: String = "01101123",
        piecesPerBox: Int = 50,
        minOrderUnit: Int = 10,
        supplyQuantity: Int = 1000,
        dcQuantity: Int = 500,
        unitPrice: Long = 5000
    ): Product {
        return Product(
            id = 1L,
            productId = productCode,
            productName = "갈릭 아이올리소스 240g",
            productCode = productCode,
            barcode = "8801045570716",
            storageType = "냉장",
            categoryMid = "소스",
            categorySub = "양념소스",
            piecesPerBox = piecesPerBox,
            minOrderUnit = minOrderUnit,
            supplyQuantity = supplyQuantity,
            dcQuantity = dcQuantity,
            unitPrice = unitPrice
        )
    }

    private fun createValidRequest(
        clientId: Long = 1L,
        deliveryDate: String = LocalDate.now().plusDays(3).toString(),
        boxQuantity: Int = 5,
        pieceQuantity: Int = 10
    ): OrderDraftRequest {
        return OrderDraftRequest(
            clientId = clientId,
            deliveryDate = deliveryDate,
            items = listOf(
                DraftItemRequest(
                    productCode = "01101123",
                    boxQuantity = boxQuantity,
                    pieceQuantity = pieceQuantity
                )
            )
        )
    }

    // --- validateOrder tests ---

    @Nested
    @DisplayName("validateOrder")
    inner class ValidateOrderTests {

        @Test
        @DisplayName("모든 제품이 유효하면 isValid=true를 반환한다")
        fun validateOrder_allValid_returnsTrue() {
            // given
            val request = createValidRequest(boxQuantity = 5, pieceQuantity = 10)
            val product = createTestProduct()
            val store = createTestStore()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))

            // when
            val result = orderSubmitService.validateOrder(testUserId, request)

            // then
            assertThat(result.isValid).isTrue()
            assertThat(result.invalidItems).isEmpty()
        }

        @Test
        @DisplayName("수량이 모두 0이면 isValid=false를 반환한다")
        fun validateOrder_zeroQuantity_returnsFalse() {
            // given
            val request = createValidRequest(boxQuantity = 0, pieceQuantity = 0)
            val product = createTestProduct()
            val store = createTestStore()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))

            // when
            val result = orderSubmitService.validateOrder(testUserId, request)

            // then
            assertThat(result.isValid).isFalse()
            assertThat(result.invalidItems).hasSize(1)
            assertThat(result.invalidItems[0].validationErrors)
                .contains("수량을 입력해주세요")
        }

        @Test
        @DisplayName("최소주문단위 미달이면 isValid=false를 반환한다")
        fun validateOrder_belowMinOrder_returnsFalse() {
            // given - minOrderUnit=10, but totalQty = 0*50 + 5 = 5
            val request = createValidRequest(boxQuantity = 0, pieceQuantity = 5)
            val product = createTestProduct(minOrderUnit = 10)
            val store = createTestStore()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))

            // when
            val result = orderSubmitService.validateOrder(testUserId, request)

            // then
            assertThat(result.isValid).isFalse()
            assertThat(result.invalidItems[0].validationErrors)
                .anyMatch { it.contains("최소주문단위") }
        }

        @Test
        @DisplayName("공급수량 초과이면 isValid=false를 반환한다")
        fun validateOrder_exceedsSupply_returnsFalse() {
            // given - supplyQuantity=100, totalQty = 5*50 + 10 = 260
            val request = createValidRequest(boxQuantity = 5, pieceQuantity = 10)
            val product = createTestProduct(supplyQuantity = 100)
            val store = createTestStore()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))

            // when
            val result = orderSubmitService.validateOrder(testUserId, request)

            // then
            assertThat(result.isValid).isFalse()
            assertThat(result.invalidItems[0].validationErrors)
                .anyMatch { it.contains("공급수량") }
        }

        @Test
        @DisplayName("DC수량 초과이면 isValid=false를 반환한다")
        fun validateOrder_exceedsDc_returnsFalse() {
            // given - dcQuantity=100, totalQty = 5*50 + 10 = 260
            val request = createValidRequest(boxQuantity = 5, pieceQuantity = 10)
            val product = createTestProduct(dcQuantity = 100)
            val store = createTestStore()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))

            // when
            val result = orderSubmitService.validateOrder(testUserId, request)

            // then
            assertThat(result.isValid).isFalse()
            assertThat(result.invalidItems[0].validationErrors)
                .anyMatch { it.contains("DC수량") }
        }

        @Test
        @DisplayName("거래처가 없으면 ClientNotFoundException을 던진다")
        fun validateOrder_clientNotFound_throwsException() {
            // given
            val request = createValidRequest()
            whenever(storeRepository.findById(1L)).thenReturn(Optional.empty())

            // when & then
            assertThatThrownBy {
                orderSubmitService.validateOrder(testUserId, request)
            }.isInstanceOf(ClientNotFoundException::class.java)
        }

        @Test
        @DisplayName("제품이 없으면 ProductNotFoundException을 던진다")
        fun validateOrder_productNotFound_throwsException() {
            // given
            val request = createValidRequest()
            val store = createTestStore()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(emptyList())

            // when & then
            assertThatThrownBy {
                orderSubmitService.validateOrder(testUserId, request)
            }.isInstanceOf(ProductNotFoundException::class.java)
        }
    }

    // --- submitOrder tests ---

    @Nested
    @DisplayName("submitOrder")
    inner class SubmitOrderTests {

        @Test
        @DisplayName("유효한 주문이면 Order를 생성하고 APPROVED를 반환한다")
        fun submitOrder_valid_returnsApproved() {
            // given
            val request = createValidRequest()
            val product = createTestProduct()
            val store = createTestStore()
            val user = createTestUser()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))
            whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] as Order }
            whenever(orderItemRepository.saveAll(any<List<OrderItem>>())).thenAnswer { it.arguments[0] as List<*> }
            whenever(sapOrderClient.sendOrder(any())).thenReturn(SapOrderResult(success = true))

            // when
            val result = orderSubmitService.submitOrder(testUserId, request)

            // then
            assertThat(result.approvalStatus).isEqualTo("APPROVED")
            assertThat(result.orderRequestNumber).startsWith("OP")
            assertThat(result.totalAmount).isGreaterThan(0)
            verify(orderDraftRepository).deleteByUserId(testUserId)
        }

        @Test
        @DisplayName("SAP 전송 실패 시 SEND_FAILED를 반환한다")
        fun submitOrder_sapFailed_returnsSendFailed() {
            // given
            val request = createValidRequest()
            val product = createTestProduct()
            val store = createTestStore()
            val user = createTestUser()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))
            whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] as Order }
            whenever(orderItemRepository.saveAll(any<List<OrderItem>>())).thenAnswer { it.arguments[0] as List<*> }
            whenever(sapOrderClient.sendOrder(any()))
                .thenReturn(SapOrderResult(success = false, failureReason = "SAP 연결 오류"))

            // when
            val result = orderSubmitService.submitOrder(testUserId, request)

            // then
            assertThat(result.approvalStatus).isEqualTo("SEND_FAILED")
            assertThat(result.failureReason).isEqualTo("SAP 연결 오류")
        }

        @Test
        @DisplayName("SAP 전송 중 예외가 발생하면 SEND_FAILED를 반환한다")
        fun submitOrder_sapException_returnsSendFailed() {
            // given
            val request = createValidRequest()
            val product = createTestProduct()
            val store = createTestStore()
            val user = createTestUser()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))
            whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] as Order }
            whenever(orderItemRepository.saveAll(any<List<OrderItem>>())).thenAnswer { it.arguments[0] as List<*> }
            whenever(sapOrderClient.sendOrder(any())).thenThrow(RuntimeException("Network error"))

            // when
            val result = orderSubmitService.submitOrder(testUserId, request)

            // then
            assertThat(result.approvalStatus).isEqualTo("SEND_FAILED")
            assertThat(result.failureReason).isEqualTo("Network error")
        }

        @Test
        @DisplayName("유효성 검증 실패 시 OrderValidationFailedException을 던진다")
        fun submitOrder_validationFailed_throwsException() {
            // given - boxQuantity=0, pieceQuantity=0 → validation fails
            val request = createValidRequest(boxQuantity = 0, pieceQuantity = 0)
            val product = createTestProduct()
            val store = createTestStore()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))

            // when & then
            assertThatThrownBy {
                orderSubmitService.submitOrder(testUserId, request)
            }.isInstanceOf(OrderValidationFailedException::class.java)
        }

        @Test
        @DisplayName("납기일이 과거이면 InvalidDeliveryDateException을 던진다")
        fun submitOrder_pastDeliveryDate_throwsException() {
            // given
            val request = createValidRequest(
                deliveryDate = LocalDate.now().minusDays(1).toString()
            )

            // when & then
            assertThatThrownBy {
                orderSubmitService.submitOrder(testUserId, request)
            }.isInstanceOf(InvalidDeliveryDateException::class.java)
        }

        @Test
        @DisplayName("금액이 정확히 계산된다 (boxQty*piecesPerBox + pieceQty) * unitPrice")
        fun submitOrder_amountCalculation_isCorrect() {
            // given - boxQty=5, piecesPerBox=50, pieceQty=10, unitPrice=5000
            // totalQty = 5*50 + 10 = 260, amount = 260 * 5000 = 1,300,000
            val request = createValidRequest(boxQuantity = 5, pieceQuantity = 10)
            val product = createTestProduct(piecesPerBox = 50, unitPrice = 5000)
            val store = createTestStore()
            val user = createTestUser()

            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCodeIn(listOf("01101123")))
                .thenReturn(listOf(product))
            whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] as Order }
            whenever(orderItemRepository.saveAll(any<List<OrderItem>>())).thenAnswer { it.arguments[0] as List<*> }
            whenever(sapOrderClient.sendOrder(any())).thenReturn(SapOrderResult(success = true))

            // when
            val result = orderSubmitService.submitOrder(testUserId, request)

            // then
            assertThat(result.totalAmount).isEqualTo(1_300_000)
        }
    }
}
