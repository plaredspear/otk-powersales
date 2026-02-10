package com.otoki.internal.service

import com.otoki.internal.entity.*
import com.otoki.internal.dto.response.FavoriteProductResponse
import com.otoki.internal.exception.AlreadyFavoritedException
import com.otoki.internal.exception.FavoriteNotFoundException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.repository.FavoriteProductRepository
import com.otoki.internal.repository.ProductRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("FavoriteProductService 테스트")
class FavoriteProductServiceTest {

    @Mock
    private lateinit var favoriteProductRepository: FavoriteProductRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var favoriteProductService: FavoriteProductService

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

    private fun createTestProduct(productCode: String = "01101123"): Product {
        return Product(
            id = 1L,
            productId = productCode,
            productName = "갈릭 아이올리소스 240g",
            productCode = productCode,
            barcode = "8801045570716",
            storageType = "냉장",
            categoryMid = "소스",
            categorySub = "양념소스",
            piecesPerBox = 20,
            unitPrice = 3000L
        )
    }

    private fun createTestFavorite(
        user: User = createTestUser(),
        product: Product = createTestProduct()
    ): FavoriteProduct {
        return FavoriteProduct(
            id = 1L,
            user = user,
            product = product,
            productCode = product.productCode,
            createdAt = LocalDateTime.of(2026, 2, 1, 10, 0)
        )
    }

    // --- getMyFavoriteProducts tests ---

    @Test
    @DisplayName("getMyFavoriteProducts - 즐겨찾기 제품 목록을 페이지네이션으로 반환한다")
    fun getMyFavoriteProducts_returnsPageOfFavorites() {
        // given
        val userId = 1L
        val pageable = PageRequest.of(0, 20)
        val favorite = createTestFavorite()
        val pageResult = PageImpl(listOf(favorite), pageable, 1)

        whenever(favoriteProductRepository.findByUserIdWithProduct(userId, pageable))
            .thenReturn(pageResult)

        // when
        val result = favoriteProductService.getMyFavoriteProducts(userId, 0, 20)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].productCode).isEqualTo("01101123")
        assertThat(result.content[0].productName).isEqualTo("갈릭 아이올리소스 240g")
        assertThat(result.content[0].barcode).isEqualTo("8801045570716")
        assertThat(result.content[0].storageType).isEqualTo("냉장")
    }

    @Test
    @DisplayName("getMyFavoriteProducts - 즐겨찾기가 없으면 빈 페이지를 반환한다")
    fun getMyFavoriteProducts_returnsEmptyPage() {
        // given
        val userId = 1L
        val pageable = PageRequest.of(0, 20)
        val pageResult = PageImpl<FavoriteProduct>(emptyList(), pageable, 0)

        whenever(favoriteProductRepository.findByUserIdWithProduct(userId, pageable))
            .thenReturn(pageResult)

        // when
        val result = favoriteProductService.getMyFavoriteProducts(userId, 0, 20)

        // then
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @Test
    @DisplayName("getMyFavoriteProducts - 잘못된 페이지 번호면 InvalidOrderParameterException")
    fun getMyFavoriteProducts_invalidPage_throwsException() {
        // when & then
        assertThatThrownBy {
            favoriteProductService.getMyFavoriteProducts(1L, -1, 20)
        }.isInstanceOf(InvalidOrderParameterException::class.java)
            .hasMessageContaining("페이지 번호")
    }

    // --- addFavoriteProduct tests ---

    @Test
    @DisplayName("addFavoriteProduct - 즐겨찾기 추가 성공")
    fun addFavoriteProduct_success() {
        // given
        val userId = 1L
        val productCode = "01101123"
        val product = createTestProduct(productCode)
        val user = createTestUser(userId)

        whenever(productRepository.findByProductCode(productCode)).thenReturn(product)
        whenever(favoriteProductRepository.existsByUserIdAndProductCode(userId, productCode)).thenReturn(false)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(favoriteProductRepository.save(any<FavoriteProduct>())).thenReturn(createTestFavorite(user, product))

        // when
        favoriteProductService.addFavoriteProduct(userId, productCode)

        // then
        verify(productRepository).findByProductCode(productCode)
        verify(favoriteProductRepository).existsByUserIdAndProductCode(userId, productCode)
        verify(userRepository).findById(userId)
        verify(favoriteProductRepository).save(any<FavoriteProduct>())
    }

    @Test
    @DisplayName("addFavoriteProduct - 제품이 존재하지 않으면 ProductNotFoundException")
    fun addFavoriteProduct_productNotFound_throwsException() {
        // given
        val productCode = "INVALID99"
        whenever(productRepository.findByProductCode(productCode)).thenReturn(null)

        // when & then
        assertThatThrownBy {
            favoriteProductService.addFavoriteProduct(1L, productCode)
        }.isInstanceOf(ProductNotFoundException::class.java)
            .hasMessageContaining("INVALID99")
    }

    @Test
    @DisplayName("addFavoriteProduct - 이미 즐겨찾기에 추가되어 있으면 AlreadyFavoritedException")
    fun addFavoriteProduct_alreadyFavorited_throwsException() {
        // given
        val userId = 1L
        val productCode = "01101123"
        val product = createTestProduct(productCode)

        whenever(productRepository.findByProductCode(productCode)).thenReturn(product)
        whenever(favoriteProductRepository.existsByUserIdAndProductCode(userId, productCode)).thenReturn(true)

        // when & then
        assertThatThrownBy {
            favoriteProductService.addFavoriteProduct(userId, productCode)
        }.isInstanceOf(AlreadyFavoritedException::class.java)
    }

    // --- removeFavoriteProduct tests ---

    @Test
    @DisplayName("removeFavoriteProduct - 즐겨찾기 해제 성공")
    fun removeFavoriteProduct_success() {
        // given
        val userId = 1L
        val productCode = "01101123"
        val favorite = createTestFavorite()

        whenever(favoriteProductRepository.findByUserIdAndProductCode(userId, productCode))
            .thenReturn(favorite)

        // when
        favoriteProductService.removeFavoriteProduct(userId, productCode)

        // then
        verify(favoriteProductRepository).findByUserIdAndProductCode(userId, productCode)
        verify(favoriteProductRepository).delete(favorite)
    }

    @Test
    @DisplayName("removeFavoriteProduct - 즐겨찾기에 없으면 FavoriteNotFoundException")
    fun removeFavoriteProduct_notFound_throwsException() {
        // given
        val userId = 1L
        val productCode = "01101123"

        whenever(favoriteProductRepository.findByUserIdAndProductCode(userId, productCode))
            .thenReturn(null)

        // when & then
        assertThatThrownBy {
            favoriteProductService.removeFavoriteProduct(userId, productCode)
        }.isInstanceOf(FavoriteNotFoundException::class.java)
    }
}
