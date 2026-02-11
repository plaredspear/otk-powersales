package com.otoki.internal.service

import com.otoki.internal.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.entity.*
import com.otoki.internal.exception.*
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.ShelfLifeRepository
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.UserRepository
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
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ShelfLifeService 테스트")
class ShelfLifeServiceTest {

    @Mock
    private lateinit var shelfLifeRepository: ShelfLifeRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var shelfLifeService: ShelfLifeService

    // ========== getShelfLifeList Tests ==========

    @Nested
    @DisplayName("getShelfLifeList - 유통기한 목록 조회")
    inner class GetShelfLifeListTests {

        @Test
        @DisplayName("그룹 분리 - 유통기한 지남 1건 + 전 2건 -> expiredItems=1, upcomingItems=2")
        fun getShelfLifeList_groupSplit() {
            // Given
            val userId = 1L
            val today = LocalDate.now()
            val fromDate = today.minusDays(7).toString()
            val toDate = today.plusDays(7).toString()

            val items = listOf(
                createShelfLife(id = 1, expiryDate = today.minusDays(1)),  // 지남
                createShelfLife(id = 2, expiryDate = today.plusDays(2)),   // 전
                createShelfLife(id = 3, expiryDate = today.plusDays(3))    // 전
            )

            whenever(shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                userId, today.minusDays(7), today.plusDays(7)
            )).thenReturn(items)

            // When
            val result = shelfLifeService.getShelfLifeList(userId, null, fromDate, toDate)

            // Then
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.expiredItems).hasSize(1)
            assertThat(result.upcomingItems).hasSize(2)
        }

        @Test
        @DisplayName("D-DAY 계산 - expiryDate가 오늘+2일 -> dDay=2")
        fun getShelfLifeList_dDayCalculation() {
            // Given
            val userId = 1L
            val today = LocalDate.now()
            val fromDate = today.toString()
            val toDate = today.plusDays(7).toString()

            val items = listOf(
                createShelfLife(id = 1, expiryDate = today.plusDays(2))
            )

            whenever(shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                userId, today, today.plusDays(7)
            )).thenReturn(items)

            // When
            val result = shelfLifeService.getShelfLifeList(userId, null, fromDate, toDate)

            // Then
            assertThat(result.upcomingItems).hasSize(1)
            assertThat(result.upcomingItems[0].dDay).isEqualTo(2)
            assertThat(result.upcomingItems[0].isExpired).isFalse()
        }

        @Test
        @DisplayName("정렬 - 각 그룹 내에서 dDay 오름차순 정렬")
        fun getShelfLifeList_sortedByDDay() {
            // Given
            val userId = 1L
            val today = LocalDate.now()
            val fromDate = today.minusDays(7).toString()
            val toDate = today.plusDays(7).toString()

            val items = listOf(
                createShelfLife(id = 1, expiryDate = today.plusDays(5)),
                createShelfLife(id = 2, expiryDate = today.plusDays(1)),
                createShelfLife(id = 3, expiryDate = today.plusDays(3))
            )

            whenever(shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                userId, today.minusDays(7), today.plusDays(7)
            )).thenReturn(items)

            // When
            val result = shelfLifeService.getShelfLifeList(userId, null, fromDate, toDate)

            // Then
            assertThat(result.upcomingItems).hasSize(3)
            assertThat(result.upcomingItems[0].dDay).isEqualTo(1)
            assertThat(result.upcomingItems[1].dDay).isEqualTo(3)
            assertThat(result.upcomingItems[2].dDay).isEqualTo(5)
        }

        @Test
        @DisplayName("빈 결과 - 해당 기간 데이터 없음 -> totalCount=0")
        fun getShelfLifeList_emptyResult() {
            // Given
            val userId = 1L
            val today = LocalDate.now()
            val fromDate = today.toString()
            val toDate = today.plusDays(7).toString()

            whenever(shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                userId, today, today.plusDays(7)
            )).thenReturn(emptyList())

            // When
            val result = shelfLifeService.getShelfLifeList(userId, null, fromDate, toDate)

            // Then
            assertThat(result.totalCount).isEqualTo(0)
            assertThat(result.expiredItems).isEmpty()
            assertThat(result.upcomingItems).isEmpty()
        }

        @Test
        @DisplayName("거래처 필터 - storeId 지정 시 해당 거래처만 조회")
        fun getShelfLifeList_withStoreFilter() {
            // Given
            val userId = 1L
            val storeId = 1025L
            val today = LocalDate.now()
            val fromDate = today.toString()
            val toDate = today.plusDays(7).toString()

            val items = listOf(
                createShelfLife(id = 1, expiryDate = today.plusDays(2))
            )

            whenever(shelfLifeRepository.findByUserIdAndStoreIdAndExpiryDateBetween(
                userId, storeId, today, today.plusDays(7)
            )).thenReturn(items)

            // When
            val result = shelfLifeService.getShelfLifeList(userId, storeId, fromDate, toDate)

            // Then
            assertThat(result.totalCount).isEqualTo(1)
        }

        @Test
        @DisplayName("잘못된 날짜 순서 - fromDate > toDate -> InvalidDateRangeException")
        fun getShelfLifeList_invalidDateOrder() {
            // Given
            val userId = 1L
            val fromDate = "2026-02-15"
            val toDate = "2026-02-10"

            // When & Then
            assertThatThrownBy {
                shelfLifeService.getShelfLifeList(userId, null, fromDate, toDate)
            }
                .isInstanceOf(InvalidShelfLifeDateRangeException::class.java)
                .hasMessageContaining("종료일은 시작일 이후여야 합니다")
        }

        @Test
        @DisplayName("기간 6개월 초과 - 7개월 기간 요청 -> InvalidDateRangeException")
        fun getShelfLifeList_exceedMaxPeriod() {
            // Given
            val userId = 1L
            val fromDate = "2026-01-01"
            val toDate = "2026-08-01"

            // When & Then
            assertThatThrownBy {
                shelfLifeService.getShelfLifeList(userId, null, fromDate, toDate)
            }
                .isInstanceOf(InvalidShelfLifeDateRangeException::class.java)
                .hasMessageContaining("최대 6개월")
        }
    }

    // ========== getShelfLife Tests ==========

    @Nested
    @DisplayName("getShelfLife - 유통기한 단건 조회")
    inner class GetShelfLifeTests {

        @Test
        @DisplayName("정상 조회 - 본인 소유 데이터 -> ShelfLifeItemResponse 반환")
        fun getShelfLife_success() {
            // Given
            val userId = 1L
            val shelfLifeId = 10L
            val shelfLife = createShelfLife(id = shelfLifeId, userId = userId)

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))

            // When
            val result = shelfLifeService.getShelfLife(userId, shelfLifeId)

            // Then
            assertThat(result.id).isEqualTo(shelfLifeId)
            assertThat(result.productCode).isEqualTo("30310009")
            assertThat(result.productName).isEqualTo("고등어김치&무조림(캔)280G")
        }

        @Test
        @DisplayName("데이터 없음 - 존재하지 않는 ID -> ShelfLifeNotFoundException")
        fun getShelfLife_notFound() {
            // Given
            val userId = 1L
            val shelfLifeId = 999L

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { shelfLifeService.getShelfLife(userId, shelfLifeId) }
                .isInstanceOf(ShelfLifeNotFoundException::class.java)
        }

        @Test
        @DisplayName("타인 데이터 - 다른 사용자의 데이터 접근 -> ShelfLifeForbiddenException")
        fun getShelfLife_forbidden() {
            // Given
            val userId = 1L
            val otherUserId = 2L
            val shelfLifeId = 10L
            val shelfLife = createShelfLife(id = shelfLifeId, userId = otherUserId)

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))

            // When & Then
            assertThatThrownBy { shelfLifeService.getShelfLife(userId, shelfLifeId) }
                .isInstanceOf(ShelfLifeForbiddenException::class.java)
        }
    }

    // ========== createShelfLife Tests ==========

    @Nested
    @DisplayName("createShelfLife - 유통기한 등록")
    inner class CreateShelfLifeTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> ShelfLife 생성 반환")
        fun createShelfLife_success() {
            // Given
            val userId = 1L
            val request = ShelfLifeCreateRequest(
                storeId = 1025L,
                productCode = "30310009",
                expiryDate = LocalDate.now().plusDays(10).toString(),
                alertDate = LocalDate.now().plusDays(9).toString(),
                description = "테스트 설명"
            )

            val user = createUser(id = userId)
            val store = createStore(id = 1025L)
            val product = createProduct()

            whenever(storeRepository.findById(1025L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCode("30310009")).thenReturn(product)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(shelfLifeRepository.existsByUserIdAndStoreIdAndProductId(userId, 1025L, product.id))
                .thenReturn(false)
            whenever(shelfLifeRepository.save(any<ShelfLife>())).thenAnswer { invocation ->
                val saved = invocation.getArgument<ShelfLife>(0)
                ShelfLife(
                    id = 100L,
                    user = saved.user,
                    store = saved.store,
                    product = saved.product,
                    productCode = saved.productCode,
                    productName = saved.productName,
                    storeName = saved.storeName,
                    expiryDate = saved.expiryDate,
                    alertDate = saved.alertDate,
                    description = saved.description
                )
            }

            // When
            val result = shelfLifeService.createShelfLife(userId, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
            assertThat(result.productCode).isEqualTo("30310009")
            assertThat(result.description).isEqualTo("테스트 설명")
        }

        @Test
        @DisplayName("거래처 없음 - 존재하지 않는 storeId -> ShelfLifeStoreNotFoundException")
        fun createShelfLife_storeNotFound() {
            // Given
            val userId = 1L
            val request = ShelfLifeCreateRequest(
                storeId = 9999L,
                productCode = "30310009",
                expiryDate = LocalDate.now().plusDays(10).toString(),
                alertDate = LocalDate.now().plusDays(9).toString()
            )

            whenever(storeRepository.findById(9999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { shelfLifeService.createShelfLife(userId, request) }
                .isInstanceOf(ShelfLifeStoreNotFoundException::class.java)
        }

        @Test
        @DisplayName("제품 없음 - 존재하지 않는 productCode -> ShelfLifeProductNotFoundException")
        fun createShelfLife_productNotFound() {
            // Given
            val userId = 1L
            val request = ShelfLifeCreateRequest(
                storeId = 1025L,
                productCode = "INVALID",
                expiryDate = LocalDate.now().plusDays(10).toString(),
                alertDate = LocalDate.now().plusDays(9).toString()
            )
            val store = createStore(id = 1025L)

            whenever(storeRepository.findById(1025L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCode("INVALID")).thenReturn(null)

            // When & Then
            assertThatThrownBy { shelfLifeService.createShelfLife(userId, request) }
                .isInstanceOf(ShelfLifeProductNotFoundException::class.java)
        }

        @Test
        @DisplayName("중복 등록 - 동일 사용자+거래처+제품 -> DuplicateShelfLifeException")
        fun createShelfLife_duplicate() {
            // Given
            val userId = 1L
            val request = ShelfLifeCreateRequest(
                storeId = 1025L,
                productCode = "30310009",
                expiryDate = LocalDate.now().plusDays(10).toString(),
                alertDate = LocalDate.now().plusDays(9).toString()
            )
            val store = createStore(id = 1025L)
            val product = createProduct()
            val user = createUser(id = userId)

            whenever(storeRepository.findById(1025L)).thenReturn(Optional.of(store))
            whenever(productRepository.findByProductCode("30310009")).thenReturn(product)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(shelfLifeRepository.existsByUserIdAndStoreIdAndProductId(userId, 1025L, product.id))
                .thenReturn(true)

            // When & Then
            assertThatThrownBy { shelfLifeService.createShelfLife(userId, request) }
                .isInstanceOf(DuplicateShelfLifeException::class.java)
        }

        @Test
        @DisplayName("잘못된 알림 날짜 - alertDate >= expiryDate -> InvalidAlertDateException")
        fun createShelfLife_invalidAlertDate() {
            // Given
            val userId = 1L
            val expiryDate = LocalDate.now().plusDays(10)
            val request = ShelfLifeCreateRequest(
                storeId = 1025L,
                productCode = "30310009",
                expiryDate = expiryDate.toString(),
                alertDate = expiryDate.toString()  // 동일 날짜
            )

            // When & Then
            assertThatThrownBy { shelfLifeService.createShelfLife(userId, request) }
                .isInstanceOf(InvalidAlertDateException::class.java)
        }
    }

    // ========== updateShelfLife Tests ==========

    @Nested
    @DisplayName("updateShelfLife - 유통기한 수정")
    inner class UpdateShelfLifeTests {

        @Test
        @DisplayName("정상 수정 - 유효한 요청 -> 필드 업데이트 반환")
        fun updateShelfLife_success() {
            // Given
            val userId = 1L
            val shelfLifeId = 10L
            val newExpiryDate = LocalDate.now().plusDays(20)
            val newAlertDate = LocalDate.now().plusDays(19)

            val shelfLife = createShelfLife(id = shelfLifeId, userId = userId)

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))
            whenever(shelfLifeRepository.save(any<ShelfLife>())).thenAnswer { it.getArgument<ShelfLife>(0) }

            val request = ShelfLifeUpdateRequest(
                expiryDate = newExpiryDate.toString(),
                alertDate = newAlertDate.toString(),
                description = "수정된 설명"
            )

            // When
            val result = shelfLifeService.updateShelfLife(userId, shelfLifeId, request)

            // Then
            assertThat(result.expiryDate).isEqualTo(newExpiryDate.toString())
            assertThat(result.alertDate).isEqualTo(newAlertDate.toString())
            assertThat(result.description).isEqualTo("수정된 설명")
        }

        @Test
        @DisplayName("alertSent 리셋 - alertDate 변경 시 alertSent=false 확인")
        fun updateShelfLife_alertSentReset() {
            // Given
            val userId = 1L
            val shelfLifeId = 10L
            val originalAlertDate = LocalDate.now().plusDays(5)
            val newAlertDate = LocalDate.now().plusDays(8)
            val newExpiryDate = LocalDate.now().plusDays(10)

            val shelfLife = createShelfLife(
                id = shelfLifeId,
                userId = userId,
                alertDate = originalAlertDate,
                alertSent = true
            )

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))
            whenever(shelfLifeRepository.save(any<ShelfLife>())).thenAnswer { it.getArgument<ShelfLife>(0) }

            val request = ShelfLifeUpdateRequest(
                expiryDate = newExpiryDate.toString(),
                alertDate = newAlertDate.toString()
            )

            // When
            shelfLifeService.updateShelfLife(userId, shelfLifeId, request)

            // Then
            assertThat(shelfLife.alertSent).isFalse()
        }

        @Test
        @DisplayName("데이터 없음 - 존재하지 않는 ID -> ShelfLifeNotFoundException")
        fun updateShelfLife_notFound() {
            // Given
            val userId = 1L
            val shelfLifeId = 999L
            val request = ShelfLifeUpdateRequest(
                expiryDate = LocalDate.now().plusDays(10).toString(),
                alertDate = LocalDate.now().plusDays(9).toString()
            )

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { shelfLifeService.updateShelfLife(userId, shelfLifeId, request) }
                .isInstanceOf(ShelfLifeNotFoundException::class.java)
        }

        @Test
        @DisplayName("타인 데이터 - 다른 사용자의 데이터 수정 시도 -> ShelfLifeForbiddenException")
        fun updateShelfLife_forbidden() {
            // Given
            val userId = 1L
            val otherUserId = 2L
            val shelfLifeId = 10L
            val shelfLife = createShelfLife(id = shelfLifeId, userId = otherUserId)

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))

            val request = ShelfLifeUpdateRequest(
                expiryDate = LocalDate.now().plusDays(10).toString(),
                alertDate = LocalDate.now().plusDays(9).toString()
            )

            // When & Then
            assertThatThrownBy { shelfLifeService.updateShelfLife(userId, shelfLifeId, request) }
                .isInstanceOf(ShelfLifeForbiddenException::class.java)
        }

        @Test
        @DisplayName("잘못된 알림 날짜 - alertDate >= expiryDate -> InvalidAlertDateException")
        fun updateShelfLife_invalidAlertDate() {
            // Given
            val userId = 1L
            val shelfLifeId = 10L
            val expiryDate = LocalDate.now().plusDays(10)
            val shelfLife = createShelfLife(id = shelfLifeId, userId = userId)

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))

            val request = ShelfLifeUpdateRequest(
                expiryDate = expiryDate.toString(),
                alertDate = expiryDate.plusDays(1).toString()  // alertDate > expiryDate
            )

            // When & Then
            assertThatThrownBy { shelfLifeService.updateShelfLife(userId, shelfLifeId, request) }
                .isInstanceOf(InvalidAlertDateException::class.java)
        }
    }

    // ========== deleteShelfLife Tests ==========

    @Nested
    @DisplayName("deleteShelfLife - 유통기한 단건 삭제")
    inner class DeleteShelfLifeTests {

        @Test
        @DisplayName("정상 삭제 - 본인 소유 데이터 삭제 성공")
        fun deleteShelfLife_success() {
            // Given
            val userId = 1L
            val shelfLifeId = 10L
            val shelfLife = createShelfLife(id = shelfLifeId, userId = userId)

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))

            // When
            shelfLifeService.deleteShelfLife(userId, shelfLifeId)

            // Then
            verify(shelfLifeRepository).delete(shelfLife)
        }

        @Test
        @DisplayName("데이터 없음 - 존재하지 않는 ID -> ShelfLifeNotFoundException")
        fun deleteShelfLife_notFound() {
            // Given
            val userId = 1L
            val shelfLifeId = 999L

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { shelfLifeService.deleteShelfLife(userId, shelfLifeId) }
                .isInstanceOf(ShelfLifeNotFoundException::class.java)
        }

        @Test
        @DisplayName("타인 데이터 - 다른 사용자의 데이터 삭제 시도 -> ShelfLifeForbiddenException")
        fun deleteShelfLife_forbidden() {
            // Given
            val userId = 1L
            val otherUserId = 2L
            val shelfLifeId = 10L
            val shelfLife = createShelfLife(id = shelfLifeId, userId = otherUserId)

            whenever(shelfLifeRepository.findById(shelfLifeId))
                .thenReturn(Optional.of(shelfLife))

            // When & Then
            assertThatThrownBy { shelfLifeService.deleteShelfLife(userId, shelfLifeId) }
                .isInstanceOf(ShelfLifeForbiddenException::class.java)
        }
    }

    // ========== deleteShelfLifeBatch Tests ==========

    @Nested
    @DisplayName("deleteShelfLifeBatch - 유통기한 일괄 삭제")
    inner class DeleteShelfLifeBatchTests {

        @Test
        @DisplayName("정상 일괄 삭제 - 3건 본인 소유 -> deletedCount=3")
        fun deleteShelfLifeBatch_success() {
            // Given
            val userId = 1L
            val ids = listOf(1L, 2L, 3L)
            val request = ShelfLifeBatchDeleteRequest(ids = ids)

            val ownedItems = listOf(
                createShelfLife(id = 1, userId = userId),
                createShelfLife(id = 2, userId = userId),
                createShelfLife(id = 3, userId = userId)
            )

            whenever(shelfLifeRepository.findByIdInAndUserId(ids, userId)).thenReturn(ownedItems)
            whenever(shelfLifeRepository.findAllById(ids)).thenReturn(ownedItems)

            // When
            val result = shelfLifeService.deleteShelfLifeBatch(userId, request)

            // Then
            assertThat(result.deletedCount).isEqualTo(3)
            verify(shelfLifeRepository).deleteAll(ownedItems)
        }

        @Test
        @DisplayName("부분 존재 - 2건 존재 + 1건 이미 삭제 -> deletedCount=2")
        fun deleteShelfLifeBatch_partialExist() {
            // Given
            val userId = 1L
            val ids = listOf(1L, 2L, 3L)
            val request = ShelfLifeBatchDeleteRequest(ids = ids)

            val existingItems = listOf(
                createShelfLife(id = 1, userId = userId),
                createShelfLife(id = 2, userId = userId)
            )

            whenever(shelfLifeRepository.findByIdInAndUserId(ids, userId)).thenReturn(existingItems)
            whenever(shelfLifeRepository.findAllById(ids)).thenReturn(existingItems)

            // When
            val result = shelfLifeService.deleteShelfLifeBatch(userId, request)

            // Then
            assertThat(result.deletedCount).isEqualTo(2)
        }

        @Test
        @DisplayName("타인 데이터 포함 - 타인 소유 항목 포함 -> ShelfLifeForbiddenException")
        fun deleteShelfLifeBatch_forbidden() {
            // Given
            val userId = 1L
            val otherUserId = 2L
            val ids = listOf(1L, 2L)
            val request = ShelfLifeBatchDeleteRequest(ids = ids)

            val ownedItems = listOf(
                createShelfLife(id = 1, userId = userId)
            )
            val allItems = listOf(
                createShelfLife(id = 1, userId = userId),
                createShelfLife(id = 2, userId = otherUserId)
            )

            whenever(shelfLifeRepository.findByIdInAndUserId(ids, userId)).thenReturn(ownedItems)
            whenever(shelfLifeRepository.findAllById(ids)).thenReturn(allItems)

            // When & Then
            assertThatThrownBy { shelfLifeService.deleteShelfLifeBatch(userId, request) }
                .isInstanceOf(ShelfLifeForbiddenException::class.java)
        }
    }

    // ========== Helpers ==========

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "12345678"
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encodedPassword",
            name = "테스트 사용자",
            department = "영업부",
            branchName = "부산지점",
            role = UserRole.USER,
            workerType = WorkerType.PATROL,
            passwordChangeRequired = false
        )
    }

    private fun createStore(
        id: Long = 1025L,
        storeCode: String = "1025",
        storeName: String = "그린유통D"
    ): Store {
        return Store(
            id = id,
            storeCode = storeCode,
            storeName = storeName
        )
    }

    private fun createProduct(
        id: Long = 100L,
        productCode: String = "30310009",
        productName: String = "고등어김치&무조림(캔)280G"
    ): Product {
        return Product(
            id = id,
            productId = productCode,
            productName = productName,
            productCode = productCode,
            barcode = "8801045570001",
            storageType = "상온"
        )
    }

    private fun createShelfLife(
        id: Long = 1L,
        userId: Long = 1L,
        storeId: Long = 1025L,
        expiryDate: LocalDate = LocalDate.now().plusDays(5),
        alertDate: LocalDate = LocalDate.now().plusDays(4),
        alertSent: Boolean = false,
        description: String? = null
    ): ShelfLife {
        val user = createUser(id = userId)
        val store = createStore(id = storeId)
        val product = createProduct()

        return ShelfLife(
            id = id,
            user = user,
            store = store,
            product = product,
            productCode = product.productCode,
            productName = product.productName,
            storeName = store.storeName,
            expiryDate = expiryDate,
            alertDate = alertDate,
            description = description,
            alertSent = alertSent
        )
    }
}
