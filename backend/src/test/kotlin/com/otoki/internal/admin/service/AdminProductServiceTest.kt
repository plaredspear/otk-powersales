package com.otoki.internal.admin.service

import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.repository.CategoryRow
import com.otoki.internal.sap.repository.ProductRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminProductService 테스트")
class AdminProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var adminProductService: AdminProductService

    @Nested
    @DisplayName("getProducts - 제품 목록 조회")
    inner class GetProductsTests {

        @Test
        @DisplayName("기본 조회 - 필터 없이 호출 -> 전체 제품 목록 반환")
        fun getProducts_noFilters_success() {
            // Given
            val products = listOf(
                createProduct(id = 1L, name = "진라면 매운맛", productCode = "P001"),
                createProduct(id = 2L, name = "카레 약간매운맛", productCode = "P002")
            )
            val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
            val page = PageImpl(products, pageable, 2L)

            whenever(productRepository.searchForAdmin(
                keyword = isNull(),
                category1 = isNull(),
                category2 = isNull(),
                category3 = isNull(),
                productStatus = isNull(),
                pageable = any()
            )).thenReturn(page)

            // When
            val result = adminProductService.getProducts(
                keyword = null, category1 = null, category2 = null,
                category3 = null, productStatus = null, page = 0, size = 20
            )

            // Then
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
            // Given
            val products = listOf(
                createProduct(id = 1L, name = "진라면 매운맛", productCode = "P001")
            )
            val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
            val page = PageImpl(products, pageable, 1L)

            whenever(productRepository.searchForAdmin(
                keyword = eq("진라면"),
                category1 = isNull(),
                category2 = isNull(),
                category3 = isNull(),
                productStatus = isNull(),
                pageable = any()
            )).thenReturn(page)

            // When
            val result = adminProductService.getProducts(
                keyword = "진라면", category1 = null, category2 = null,
                category3 = null, productStatus = null, page = 0, size = 20
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("진라면 매운맛")
        }

        @Test
        @DisplayName("복합 필터 - 카테고리 + 상태 전달 -> 필터링된 결과 반환")
        fun getProducts_withMultipleFilters_success() {
            // Given
            val products = listOf(
                createProduct(id = 1L, name = "진라면", category1 = "면류", category2 = "라면", productStatus = "판매중")
            )
            val pageable = PageRequest.of(0, 10, Sort.by("name").ascending())
            val page = PageImpl(products, pageable, 1L)

            whenever(productRepository.searchForAdmin(
                keyword = isNull(),
                category1 = eq("면류"),
                category2 = eq("라면"),
                category3 = isNull(),
                productStatus = eq("판매중"),
                pageable = any()
            )).thenReturn(page)

            // When
            val result = adminProductService.getProducts(
                keyword = null, category1 = "면류", category2 = "라면",
                category3 = null, productStatus = "판매중", page = 0, size = 10
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("빈 결과 - 존재하지 않는 카테고리 -> 빈 목록 반환")
        fun getProducts_noResults_emptyContent() {
            // Given
            val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
            val emptyPage = PageImpl<Product>(emptyList(), pageable, 0L)

            whenever(productRepository.searchForAdmin(
                keyword = isNull(),
                category1 = eq("없는카테고리"),
                category2 = isNull(),
                category3 = isNull(),
                productStatus = isNull(),
                pageable = any()
            )).thenReturn(emptyPage)

            // When
            val result = adminProductService.getProducts(
                keyword = null, category1 = "없는카테고리", category2 = null,
                category3 = null, productStatus = null, page = 0, size = 20
            )

            // Then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.totalPages).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getCategories - 카테고리 목록 조회")
    inner class GetCategoriesTests {

        @Test
        @DisplayName("계층 구조 - 카테고리 3단계 트리 반환")
        fun getCategories_success() {
            // Given
            val rows = listOf(
                CategoryRow("면류", "라면", "봉지면"),
                CategoryRow("면류", "라면", "컵라면"),
                CategoryRow("면류", "국수", "소면"),
                CategoryRow("소스류", "카레", "레토르트")
            )
            whenever(productRepository.findDistinctCategories()).thenReturn(rows)

            // When
            val result = adminProductService.getCategories()

            // Then
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
            // Given
            whenever(productRepository.findDistinctCategories()).thenReturn(emptyList())

            // When
            val result = adminProductService.getCategories()

            // Then
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
        standardPrice: Double? = 850.0,
        unit: String? = "EA",
        storageCondition: String? = "실온",
        productStatus: String? = "판매중",
        launchDate: java.time.LocalDate? = java.time.LocalDate.of(2020, 1, 15)
    ): Product = Product(
        id = id,
        name = name,
        productCode = productCode,
        category1 = category1,
        category2 = category2,
        category3 = category3,
        standardPrice = standardPrice,
        unit = unit,
        storageCondition = storageCondition,
        productStatus = productStatus,
        launchDate = launchDate
    )
}
