package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.FavoriteProduct
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.repository.FavoriteProductRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductSearchRow
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.exception.AlreadyFavoritedException
import com.otoki.powersales.platform.common.exception.FavoriteNotFoundException
import com.otoki.powersales.platform.common.exception.ProductNotFoundException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("FavoriteProductService 테스트")
class FavoriteProductServiceTest {

    private val favoriteProductRepository: FavoriteProductRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val service = FavoriteProductService(
        favoriteProductRepository,
        productRepository,
        employeeRepository,
    )

    private val userId = 1L
    private val employeeCode = "20030117"

    private fun mockEmployee() {
        val employee = mockk<Employee>()
        every { employee.employeeCode } returns employeeCode
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
    }

    private fun product(code: String, name: String) = Product(
        name = name,
        productCode = code,
        logisticsBarcode = "880$code",
    )

    @Test
    @DisplayName("getMyFavoriteProducts — 최근순 OrderProductDto(isFavorite=true) 매핑, 마스터에 없는 코드 제외")
    fun getMyFavoriteProducts_mapsAndFiltersMissing() {
        mockEmployee()
        every { favoriteProductRepository.findByEmployeeCodeOrderByCreatedAtDesc(employeeCode) } returns listOf(
            FavoriteProduct(employeeCode = employeeCode, productCode = "P1"),
            FavoriteProduct(employeeCode = employeeCode, productCode = "P_GONE"),
            FavoriteProduct(employeeCode = employeeCode, productCode = "P2"),
        )
        // P_GONE 은 제품 마스터에 없음(단종/삭제) → 결과에서 제외.
        // 검색과 동일하게 발주 단위 매칭 대표 바코드(ProductBarcode.barcode)를 함께 내려준다.
        every { productRepository.findOrderRowsByProductCodes(listOf("P1", "P_GONE", "P2")) } returns listOf(
            ProductSearchRow(product("P1", "열라면"), "8801111"),
            ProductSearchRow(product("P2", "참깨라면"), "8802222"),
        )

        val result = service.getMyFavoriteProducts(userId)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.productCode }).containsExactly("P1", "P2")
        assertThat(result).allMatch { it.isFavorite }
        // 대표 바코드가 채워져야 한다(logisticsBarcode 폴백이 아님) — POS/전산 매출조회 필터 정합.
        assertThat(result.map { it.barcode }).containsExactly("8801111", "8802222")
    }

    @Test
    @DisplayName("getMyFavoriteProducts — 즐겨찾기 없음 → 빈 목록(제품 조회 호출 안 함)")
    fun getMyFavoriteProducts_empty() {
        mockEmployee()
        every { favoriteProductRepository.findByEmployeeCodeOrderByCreatedAtDesc(employeeCode) } returns emptyList()

        val result = service.getMyFavoriteProducts(userId)

        assertThat(result).isEmpty()
        verify(exactly = 0) { productRepository.findOrderRowsByProductCodes(any()) }
    }

    @Test
    @DisplayName("getFavoriteProductCodes — 사번 즐겨찾기 코드 집합 반환")
    fun getFavoriteProductCodes_returnsSet() {
        mockEmployee()
        every { favoriteProductRepository.findByEmployeeCodeOrderByCreatedAtDesc(employeeCode) } returns listOf(
            FavoriteProduct(employeeCode = employeeCode, productCode = "P1"),
            FavoriteProduct(employeeCode = employeeCode, productCode = "P2"),
        )

        assertThat(service.getFavoriteProductCodes(userId)).containsExactlyInAnyOrder("P1", "P2")
    }

    @Test
    @DisplayName("addFavoriteProduct — 성공 시 저장")
    fun addFavoriteProduct_success() {
        mockEmployee()
        every { productRepository.findByProductCode("P1") } returns product("P1", "열라면")
        every { favoriteProductRepository.existsByEmployeeCodeAndProductCode(employeeCode, "P1") } returns false
        val saved = slot<FavoriteProduct>()
        every { favoriteProductRepository.save(capture(saved)) } answers { saved.captured }

        service.addFavoriteProduct(userId, "P1")

        assertThat(saved.captured.employeeCode).isEqualTo(employeeCode)
        assertThat(saved.captured.productCode).isEqualTo("P1")
    }

    @Test
    @DisplayName("addFavoriteProduct — 제품 없음 → ProductNotFoundException")
    fun addFavoriteProduct_productNotFound() {
        mockEmployee()
        every { productRepository.findByProductCode("X") } returns null

        assertThatThrownBy { service.addFavoriteProduct(userId, "X") }
            .isInstanceOf(ProductNotFoundException::class.java)
        verify(exactly = 0) { favoriteProductRepository.save(any()) }
    }

    @Test
    @DisplayName("addFavoriteProduct — 이미 즐겨찾기 → AlreadyFavoritedException")
    fun addFavoriteProduct_alreadyFavorited() {
        mockEmployee()
        every { productRepository.findByProductCode("P1") } returns product("P1", "열라면")
        every { favoriteProductRepository.existsByEmployeeCodeAndProductCode(employeeCode, "P1") } returns true

        assertThatThrownBy { service.addFavoriteProduct(userId, "P1") }
            .isInstanceOf(AlreadyFavoritedException::class.java)
        verify(exactly = 0) { favoriteProductRepository.save(any()) }
    }

    @Test
    @DisplayName("removeFavoriteProduct — 성공 시 삭제")
    fun removeFavoriteProduct_success() {
        mockEmployee()
        val favorite = FavoriteProduct(employeeCode = employeeCode, productCode = "P1")
        every { favoriteProductRepository.findByEmployeeCodeAndProductCode(employeeCode, "P1") } returns favorite
        every { favoriteProductRepository.delete(favorite) } just Runs

        service.removeFavoriteProduct(userId, "P1")

        verify { favoriteProductRepository.delete(favorite) }
    }

    @Test
    @DisplayName("removeFavoriteProduct — 즐겨찾기 없음 → FavoriteNotFoundException")
    fun removeFavoriteProduct_notFound() {
        mockEmployee()
        every { favoriteProductRepository.findByEmployeeCodeAndProductCode(employeeCode, "P1") } returns null

        assertThatThrownBy { service.removeFavoriteProduct(userId, "P1") }
            .isInstanceOf(FavoriteNotFoundException::class.java)
        verify(exactly = 0) { favoriteProductRepository.delete(any()) }
    }

    @Test
    @DisplayName("사번 없는 사원 → IllegalStateException")
    fun resolveEmployeeCode_nullCode_throws() {
        val employee = mockk<Employee>()
        every { employee.employeeCode } returns null
        every { employeeRepository.findById(userId) } returns Optional.of(employee)

        assertThatThrownBy { service.getMyFavoriteProducts(userId) }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
