package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.domain.foundation.product.exception.ProductNotFoundException
import com.otoki.powersales.domain.foundation.product.repository.CategoryRow
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.repository.ProductBarcodeRepository
import com.otoki.powersales.domain.foundation.product.service.AdminProductService
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("AdminProductService 테스트")
class AdminProductServiceTest {

    private val productRepository: ProductRepository = mockk()
    private val productBarcodeRepository: ProductBarcodeRepository = mockk()

    private val adminProductService = AdminProductService(
        productRepository,
        productBarcodeRepository,
    )

    @Nested
    @DisplayName("getProducts - 제품 목록 조회")
    inner class GetProductsTests {

        @Test
        @DisplayName("기본 조회 - 필터 없이 호출 -> 전체 제품 목록 반환")
        fun getProducts_noFilters_success() {
            val products = listOf(
                createProduct(id = 1L, name = "진라면 매운맛", productCode = "P001"),
                createProduct(id = 2L, name = "카레 약간매운맛", productCode = "P002")
            )
            val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
            val page = PageImpl(products, pageable, 2L)

            every {
                productRepository.searchForAdmin(
                    keyword = null,
                    category1 = null,
                    category2 = null,
                    category3 = null,
                    productStatus = null,
                    pageable = any()
                )
            } returns page

            val result = adminProductService.getProducts(
                keyword = null, category1 = null, category2 = null,
                category3 = null, productStatus = null, page = 0, size = 20
            )

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].productCode).isEqualTo("P001")
            assertThat(result.content[1].name).isEqualTo("카레 약간매운맛")
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("키워드 검색 - keyword 전달 -> 필터링된 결과 반환")
        fun getProducts_withKeyword_success() {
            val products = listOf(
                createProduct(id = 1L, name = "진라면 매운맛", productCode = "P001")
            )
            val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
            val page = PageImpl(products, pageable, 1L)

            every {
                productRepository.searchForAdmin(
                    keyword = "진라면",
                    category1 = null,
                    category2 = null,
                    category3 = null,
                    productStatus = null,
                    pageable = any()
                )
            } returns page

            val result = adminProductService.getProducts(
                keyword = "진라면", category1 = null, category2 = null,
                category3 = null, productStatus = null, page = 0, size = 20
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("진라면 매운맛")
        }

        @Test
        @DisplayName("복합 필터 - 카테고리 + 상태 전달 -> 필터링된 결과 반환")
        fun getProducts_withMultipleFilters_success() {
            val products = listOf(
                createProduct(id = 1L, name = "진라면", category1 = "면류", category2 = "라면", productStatus = "판매중")
            )
            val pageable = PageRequest.of(0, 10, Sort.by("name").ascending())
            val page = PageImpl(products, pageable, 1L)

            every {
                productRepository.searchForAdmin(
                    keyword = null,
                    category1 = "면류",
                    category2 = "라면",
                    category3 = null,
                    productStatus = "판매중",
                    pageable = any()
                )
            } returns page

            val result = adminProductService.getProducts(
                keyword = null, category1 = "면류", category2 = "라면",
                category3 = null, productStatus = "판매중", page = 0, size = 10
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("빈 결과 - 존재하지 않는 카테고리 -> 빈 목록 반환")
        fun getProducts_noResults_emptyContent() {
            val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
            val emptyPage = PageImpl<Product>(emptyList(), pageable, 0L)

            every {
                productRepository.searchForAdmin(
                    keyword = null,
                    category1 = "없는카테고리",
                    category2 = null,
                    category3 = null,
                    productStatus = null,
                    pageable = any()
                )
            } returns emptyPage

            val result = adminProductService.getProducts(
                keyword = null, category1 = "없는카테고리", category2 = null,
                category3 = null, productStatus = null, page = 0, size = 20
            )

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.totalPages).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getProductDetail - 제품 상세 조회")
    inner class GetProductDetailTests {

        @Test
        @DisplayName("존재하는 제품코드 -> ProductDetail 반환")
        fun getProductDetail_success() {
            val product = createProduct(id = 10L, productCode = "P100", name = "꿀배청 680G")
            every { productRepository.findByProductCode("P100") } returns product
            every { productBarcodeRepository.findByProductId(10L) } returns listOf(
                ProductBarcode(id = 1, productId = 10L, barcode = "8801234567890", unit = "EA", sortOrder = "1")
            )

            val result = adminProductService.getProductDetail("P100")

            assertThat(result.productCode).isEqualTo("P100")
            assertThat(result.name).isEqualTo("꿀배청 680G")
            assertThat(result.barcodes).hasSize(1)
            assertThat(result.barcodes[0].barcode).isEqualTo("8801234567890")
        }

        @Test
        @DisplayName("존재하지 않는 제품코드 -> ProductNotFoundException")
        fun getProductDetail_notFound() {
            every { productRepository.findByProductCode("X-NONE") } returns null

            assertThatThrownBy { adminProductService.getProductDetail("X-NONE") }
                .isInstanceOf(ProductNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getCategories - 카테고리 목록 조회")
    inner class GetCategoriesTests {

        @Test
        @DisplayName("계층 구조 - 카테고리 3단계 트리 반환")
        fun getCategories_success() {
            val rows = listOf(
                CategoryRow("면류", "라면", "봉지면"),
                CategoryRow("면류", "라면", "컵라면"),
                CategoryRow("면류", "국수", "소면"),
                CategoryRow("소스류", "카레", "레토르트")
            )
            every { productRepository.findDistinctCategories() } returns rows

            val result = adminProductService.getCategories()

            assertThat(result).hasSize(2)

            val noodles = result.find { it.category1 == "면류" }!!
            assertThat(noodles.children).hasSize(2)
            val ramen = noodles.children.find { it.category2 == "라면" }!!
            assertThat(ramen.children).containsExactly("봉지면", "컵라면")
            val noodle = noodles.children.find { it.category2 == "국수" }!!
            assertThat(noodle.children).containsExactly("소면")

            val sauce = result.find { it.category1 == "소스류" }!!
            assertThat(sauce.children).hasSize(1)
            assertThat(sauce.children[0].category2).isEqualTo("카레")
            assertThat(sauce.children[0].children).containsExactly("레토르트")
        }

        @Test
        @DisplayName("빈 결과 - 카테고리 없음 -> 빈 리스트 반환")
        fun getCategories_empty() {
            every { productRepository.findDistinctCategories() } returns emptyList()

            val result = adminProductService.getCategories()

            assertThat(result).isEmpty()
        }
    }

    private fun createProduct(
        id: Long = 1L,
        name: String? = "테스트제품",
        productCode: String? = "P001",
        category1: String? = "면류",
        category2: String? = "라면",
        category3: String? = "봉지면",
        standardUnitPrice: Double? = 850.0,
        unit: String? = "EA",
        storageCondition: String? = "실온",
        productStatus: String? = "판매중",
        launchDate: LocalDate? = LocalDate.of(2020, 1, 15)
    ): Product = Product(
        id = id,
        name = name,
        productCode = productCode,
        productCategory1 = category1,
        productCategory2 = category2,
        productCategory3 = category3,
        standardUnitPrice = standardUnitPrice?.let { BigDecimal.valueOf(it) },
        unit = unit,
        storageCondition = StorageCondition.fromDisplayNameOrNull(storageCondition),
        productStatus = ProductStatus.fromDisplayNameOrNull(productStatus),
        launchDate = launchDate
    )
}
