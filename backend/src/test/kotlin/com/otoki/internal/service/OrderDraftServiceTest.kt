package com.otoki.internal.service

import com.otoki.internal.dto.request.DraftItemRequest
import com.otoki.internal.dto.request.OrderDraftRequest
import com.otoki.internal.entity.*
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.DraftNotFoundException
import com.otoki.internal.exception.InvalidDeliveryDateException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.repository.OrderDraftRepository
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
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
@DisplayName("OrderDraftService 테스트")
class OrderDraftServiceTest {

    @Mock
    private lateinit var orderDraftRepository: OrderDraftRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var orderDraftService: OrderDraftService

    // --- Helper methods ---

    private fun createTestUser(id: Long = 1L): User {
        return User(
            id = id,
            employeeId = "20010585",
            password = "encodedPassword",
            name = "홍길동",
            orgName = "서울지점"
        )
    }

    private fun createTestStore(id: Long = 1L): Store {
        return Store(
            id = id,
            storeCode = "ST001",
            storeName = "롯데마트 응암점",
            address = "서울시 은평구",
            representativeName = "김대표",
            phoneNumber = "02-1234-5678",
            creditLimit = 100000000L,
            usedCredit = 45000000L
        )
    }

    private fun createTestProduct(
        productCode: String = "01101123",
        piecesPerBox: Int = 50,
        minOrderUnit: Int = 1,
        supplyQuantity: Int = 100,
        dcQuantity: Int = 50,
        unitPrice: Long = 5000
    ): Product {
        return Product(
            id = 1L,
            productId = productCode,
            productName = "갈릭 아이올리소스 240g",
            productCode = productCode,
            barcode = "8801045570716",
            storageType = "냉장",
            piecesPerBox = piecesPerBox,
            minOrderUnit = minOrderUnit,
            supplyQuantity = supplyQuantity,
            dcQuantity = dcQuantity,
            unitPrice = unitPrice
        )
    }

    private fun createTestOrderDraft(
        user: User,
        store: Store,
        deliveryDate: LocalDate = LocalDate.now().plusDays(1),
        totalAmount: Long = 0
    ): OrderDraft {
        return OrderDraft(
            id = 1L,
            user = user,
            store = store,
            deliveryDate = deliveryDate,
            totalAmount = totalAmount
        )
    }

    private fun createTestOrderDraftItem(
        orderDraft: OrderDraft,
        product: Product,
        boxQuantity: Int = 5,
        pieceQuantity: Int = 10
    ): OrderDraftItem {
        val amount = (boxQuantity.toLong() * product.piecesPerBox + pieceQuantity) * product.unitPrice
        return OrderDraftItem(
            id = 1L,
            orderDraft = orderDraft,
            productCode = product.productCode,
            productName = product.productName,
            boxQuantity = boxQuantity,
            pieceQuantity = pieceQuantity,
            unitPrice = product.unitPrice,
            amount = amount,
            piecesPerBox = product.piecesPerBox,
            minOrderUnit = product.minOrderUnit,
            supplyQuantity = product.supplyQuantity,
            dcQuantity = product.dcQuantity
        )
    }

    // --- getMyDraft tests ---

    @Test
    @DisplayName("getMyDraft - 임시저장된 주문이 존재하면 OrderDraftResponse를 반환한다")
    fun getMyDraft_whenDraftExists_returnsOrderDraftResponse() {
        // given
        val userId = 1L
        val user = createTestUser(userId)
        val store = createTestStore()
        val product = createTestProduct()
        val draft = createTestOrderDraft(user, store, totalAmount = 1300000L)
        val draftItem = createTestOrderDraftItem(draft, product, boxQuantity = 5, pieceQuantity = 10)
        draft.items.add(draftItem)

        whenever(orderDraftRepository.findByUserIdWithItems(userId)).thenReturn(draft)

        // when
        val result = orderDraftService.getMyDraft(userId)

        // then
        assertThat(result).isNotNull
        assertThat(result!!.clientId).isEqualTo(store.id)
        assertThat(result.clientName).isEqualTo("롯데마트 응암점")
        assertThat(result.deliveryDate).isEqualTo(draft.deliveryDate.toString())
        assertThat(result.totalAmount).isEqualTo(1300000L)
        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].productCode).isEqualTo("01101123")
        assertThat(result.items[0].boxQuantity).isEqualTo(5)
        assertThat(result.items[0].pieceQuantity).isEqualTo(10)
        // amount = (5 * 50 + 10) * 5000 = 260 * 5000 = 1,300,000
        assertThat(result.items[0].amount).isEqualTo(1300000L)
        assertThat(result.items[0].piecesPerBox).isEqualTo(50)

        verify(orderDraftRepository).findByUserIdWithItems(userId)
    }

    @Test
    @DisplayName("getMyDraft - 임시저장된 주문이 없으면 null을 반환한다")
    fun getMyDraft_whenNoDraft_returnsNull() {
        // given
        val userId = 1L
        whenever(orderDraftRepository.findByUserIdWithItems(userId)).thenReturn(null)

        // when
        val result = orderDraftService.getMyDraft(userId)

        // then
        assertThat(result).isNull()
        verify(orderDraftRepository).findByUserIdWithItems(userId)
    }

    // --- saveDraft tests ---

    @Test
    @DisplayName("saveDraft - 새로운 임시저장을 성공적으로 저장한다")
    fun saveDraft_success_savesNewDraft() {
        // given
        val userId = 1L
        val clientId = 1L
        val tomorrow = LocalDate.now().plusDays(1)
        val user = createTestUser(userId)
        val store = createTestStore(clientId)
        val product = createTestProduct()

        val request = OrderDraftRequest(
            clientId = clientId,
            deliveryDate = tomorrow.toString(),
            items = listOf(
                DraftItemRequest(productCode = "01101123", boxQuantity = 5, pieceQuantity = 10)
            )
        )

        whenever(storeRepository.findById(clientId)).thenReturn(Optional.of(store))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(productRepository.findByProductCodeIn(listOf("01101123"))).thenReturn(listOf(product))
        whenever(orderDraftRepository.save(any<OrderDraft>())).thenAnswer { it.arguments[0] as OrderDraft }

        // when
        val result = orderDraftService.saveDraft(userId, request)

        // then
        assertThat(result).isNotNull
        assertThat(result.savedAt).isNotBlank()

        verify(orderDraftRepository).deleteByUserId(userId)
        verify(storeRepository).findById(clientId)
        verify(userRepository).findById(userId)
        verify(productRepository).findByProductCodeIn(listOf("01101123"))
        verify(orderDraftRepository).save(any<OrderDraft>())
    }

    @Test
    @DisplayName("saveDraft - 기존 임시저장을 덮어쓴다 (deleteByUserId 호출 확인)")
    fun saveDraft_overwritesExistingDraft() {
        // given
        val userId = 1L
        val clientId = 1L
        val tomorrow = LocalDate.now().plusDays(1)
        val user = createTestUser(userId)
        val store = createTestStore(clientId)
        val product = createTestProduct()

        val request = OrderDraftRequest(
            clientId = clientId,
            deliveryDate = tomorrow.toString(),
            items = listOf(
                DraftItemRequest(productCode = "01101123", boxQuantity = 3, pieceQuantity = 20)
            )
        )

        whenever(storeRepository.findById(clientId)).thenReturn(Optional.of(store))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(productRepository.findByProductCodeIn(listOf("01101123"))).thenReturn(listOf(product))
        whenever(orderDraftRepository.save(any<OrderDraft>())).thenAnswer { it.arguments[0] as OrderDraft }

        // when
        val result = orderDraftService.saveDraft(userId, request)

        // then
        assertThat(result).isNotNull
        verify(orderDraftRepository).deleteByUserId(userId)
        verify(orderDraftRepository).save(any<OrderDraft>())
    }

    @Test
    @DisplayName("saveDraft - 거래처를 찾을 수 없으면 ClientNotFoundException을 발생시킨다")
    fun saveDraft_whenClientNotFound_throwsClientNotFoundException() {
        // given
        val userId = 1L
        val clientId = 999L
        val tomorrow = LocalDate.now().plusDays(1)

        val request = OrderDraftRequest(
            clientId = clientId,
            deliveryDate = tomorrow.toString(),
            items = listOf(
                DraftItemRequest(productCode = "01101123", boxQuantity = 5, pieceQuantity = 10)
            )
        )

        whenever(storeRepository.findById(clientId)).thenReturn(Optional.empty())

        // when & then
        assertThatThrownBy { orderDraftService.saveDraft(userId, request) }
            .isInstanceOf(ClientNotFoundException::class.java)
    }

    @Test
    @DisplayName("saveDraft - 제품을 찾을 수 없으면 ProductNotFoundException을 발생시킨다")
    fun saveDraft_whenProductNotFound_throwsProductNotFoundException() {
        // given
        val userId = 1L
        val clientId = 1L
        val tomorrow = LocalDate.now().plusDays(1)
        val user = createTestUser(userId)
        val store = createTestStore(clientId)
        val product = createTestProduct("01101123")

        val request = OrderDraftRequest(
            clientId = clientId,
            deliveryDate = tomorrow.toString(),
            items = listOf(
                DraftItemRequest(productCode = "01101123", boxQuantity = 5, pieceQuantity = 10),
                DraftItemRequest(productCode = "INVALID99", boxQuantity = 2, pieceQuantity = 5)
            )
        )

        whenever(storeRepository.findById(clientId)).thenReturn(Optional.of(store))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(productRepository.findByProductCodeIn(listOf("01101123", "INVALID99")))
            .thenReturn(listOf(product))

        // when & then
        assertThatThrownBy { orderDraftService.saveDraft(userId, request) }
            .isInstanceOf(ProductNotFoundException::class.java)
            .hasMessageContaining("INVALID99")
    }

    @Test
    @DisplayName("saveDraft - 잘못된 납기일 형식이면 InvalidDeliveryDateException을 발생시킨다")
    fun saveDraft_whenInvalidDateFormat_throwsInvalidDeliveryDateException() {
        // given
        val userId = 1L
        val clientId = 1L

        val request = OrderDraftRequest(
            clientId = clientId,
            deliveryDate = "invalid-date",
            items = listOf(
                DraftItemRequest(productCode = "01101123", boxQuantity = 5, pieceQuantity = 10)
            )
        )

        // 날짜 파싱이 거래처 확인보다 먼저 실행되므로 storeRepository mock 불필요

        // when & then
        assertThatThrownBy { orderDraftService.saveDraft(userId, request) }
            .isInstanceOf(InvalidDeliveryDateException::class.java)
    }

    @Test
    @DisplayName("saveDraft - 납기일이 과거이면 InvalidDeliveryDateException을 발생시킨다")
    fun saveDraft_whenPastDate_throwsInvalidDeliveryDateException() {
        // given
        val userId = 1L
        val clientId = 1L
        val yesterday = LocalDate.now().minusDays(1)

        val request = OrderDraftRequest(
            clientId = clientId,
            deliveryDate = yesterday.toString(),
            items = listOf(
                DraftItemRequest(productCode = "01101123", boxQuantity = 5, pieceQuantity = 10)
            )
        )

        // 날짜 검증이 거래처 확인보다 먼저 실행되므로 storeRepository mock 불필요

        // when & then
        assertThatThrownBy { orderDraftService.saveDraft(userId, request) }
            .isInstanceOf(InvalidDeliveryDateException::class.java)
    }

    @Test
    @DisplayName("saveDraft - 금액 계산이 정확하다: (5*50+10)*5000 = 1,300,000")
    fun saveDraft_calculatesAmountCorrectly() {
        // given
        val userId = 1L
        val clientId = 1L
        val tomorrow = LocalDate.now().plusDays(1)
        val user = createTestUser(userId)
        val store = createTestStore(clientId)
        val product = createTestProduct(piecesPerBox = 50, unitPrice = 5000)

        val request = OrderDraftRequest(
            clientId = clientId,
            deliveryDate = tomorrow.toString(),
            items = listOf(
                DraftItemRequest(productCode = "01101123", boxQuantity = 5, pieceQuantity = 10)
            )
        )

        whenever(storeRepository.findById(clientId)).thenReturn(Optional.of(store))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(productRepository.findByProductCodeIn(listOf("01101123"))).thenReturn(listOf(product))

        // ArgumentCaptor 대신 answer로 저장된 OrderDraft를 캡처
        var capturedDraft: OrderDraft? = null
        whenever(orderDraftRepository.save(any<OrderDraft>())).thenAnswer {
            capturedDraft = it.arguments[0] as OrderDraft
            capturedDraft!!
        }

        // when
        orderDraftService.saveDraft(userId, request)

        // then
        assertThat(capturedDraft).isNotNull
        assertThat(capturedDraft!!.totalAmount).isEqualTo(1300000L)
        assertThat(capturedDraft!!.items).hasSize(1)
        assertThat(capturedDraft!!.items[0].amount).isEqualTo(1300000L)
    }

    // --- deleteDraft tests ---

    @Test
    @DisplayName("deleteDraft - 임시저장 주문을 성공적으로 삭제한다")
    fun deleteDraft_success() {
        // given
        val userId = 1L
        val user = createTestUser(userId)
        val store = createTestStore()
        val draft = createTestOrderDraft(user, store)

        whenever(orderDraftRepository.findByUserIdWithItems(userId)).thenReturn(draft)

        // when
        orderDraftService.deleteDraft(userId)

        // then
        verify(orderDraftRepository).findByUserIdWithItems(userId)
        verify(orderDraftRepository).delete(draft)
    }

    @Test
    @DisplayName("deleteDraft - 임시저장 주문이 없으면 DraftNotFoundException을 발생시킨다")
    fun deleteDraft_whenNoDraft_throwsDraftNotFoundException() {
        // given
        val userId = 1L
        whenever(orderDraftRepository.findByUserIdWithItems(userId)).thenReturn(null)

        // when & then
        assertThatThrownBy { orderDraftService.deleteDraft(userId) }
            .isInstanceOf(DraftNotFoundException::class.java)

        verify(orderDraftRepository).findByUserIdWithItems(userId)
    }
}
