package com.otoki.internal.repository

import com.otoki.internal.entity.Product
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("ProductRepository 테스트")
class ProductRepositoryTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 데이터 삽입
        val products = listOf(
            createProduct("18110014", "열라면_용기105G", "18110014", "8801045570716", "라면", "용기면"),
            createProduct("18110007", "열라면_용기115G", "18110007", "8801045570723", "라면", "용기면"),
            createProduct("18110001", "열라면_봉지120G", "18110001", "8801045570730", "라면", "봉지면"),
            createProduct("18120001", "진라면_순한맛_봉지120G", "18120001", "8801045571001", "라면", "봉지면"),
            createProduct("18120002", "진라면_매운맛_봉지120G", "18120002", "8801045571018", "라면", "봉지면"),
            createProduct("19110001", "오뚜기카레_약간매운맛100G", "19110001", "8801045573001", "즉석식품", "카레"),
            createProduct("20110001", "오뚜기마요네스500G", "20110001", "8801045575001", "소스", "마요네스")
        )
        products.forEach { testEntityManager.persistAndFlush(it) }
        testEntityManager.clear()
    }

    // ========== searchByText Tests ==========

    @Nested
    @DisplayName("searchByText - 제품명/제품코드 LIKE 검색")
    inner class SearchByTextTests {

        @Test
        @DisplayName("제품명으로 검색 - '열라면' 포함 제품 반환")
        fun searchByText_byProductName_returnsMatchingProducts() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.searchByText("열라면", pageable)

            // Then
            assertThat(result.content).hasSize(3)
            assertThat(result.content).allSatisfy { product ->
                assertThat(product.productName).containsIgnoringCase("열라면")
            }
        }

        @Test
        @DisplayName("제품명으로 검색 - '진라면' 포함 제품 반환")
        fun searchByText_jinRamen_returnsMatchingProducts() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.searchByText("진라면", pageable)

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.content).allSatisfy { product ->
                assertThat(product.productName).containsIgnoringCase("진라면")
            }
        }

        @Test
        @DisplayName("제품코드로 검색 - 코드 일부 매칭")
        fun searchByText_byProductCode_returnsMatchingProducts() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.searchByText("18110", pageable)

            // Then
            assertThat(result.content).hasSize(3)
            assertThat(result.content).allSatisfy { product ->
                assertThat(product.productCode).contains("18110")
            }
        }

        @Test
        @DisplayName("검색 결과 없음 - 빈 페이지 반환")
        fun searchByText_noMatch_returnsEmptyPage() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.searchByText("존재하지않는제품", pageable)

            // Then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("결과가 제품명 기준 가나다순으로 정렬된다")
        fun searchByText_resultsSortedByProductName() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.searchByText("라면", pageable)

            // Then
            assertThat(result.content).hasSizeGreaterThan(1)
            val names = result.content.map { it.productName }
            assertThat(names).isSorted()
        }
    }

    // ========== searchByTextIncludingBarcode Tests ==========

    @Nested
    @DisplayName("searchByTextIncludingBarcode - 바코드 포함 텍스트 검색")
    inner class SearchByTextIncludingBarcodeTests {

        @Test
        @DisplayName("숫자 검색어로 제품코드/바코드 함께 검색")
        fun searchByTextIncludingBarcode_matchesProductCodeAndBarcode() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.searchByTextIncludingBarcode("8801045570716", pageable)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].barcode).isEqualTo("8801045570716")
        }

        @Test
        @DisplayName("제품코드 일부로 검색 - 코드/바코드 매칭 결과")
        fun searchByTextIncludingBarcode_partialCode_returnsMatches() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.searchByTextIncludingBarcode("18110014", pageable)

            // Then
            assertThat(result.content).isNotEmpty()
            assertThat(result.content).anySatisfy { product ->
                assertThat(product.productCode).isEqualTo("18110014")
            }
        }
    }

    // ========== findByBarcode Tests ==========

    @Nested
    @DisplayName("findByBarcode - 바코드 정확 일치 검색")
    inner class FindByBarcodeTests {

        @Test
        @DisplayName("존재하는 바코드 검색 - 해당 제품 반환")
        fun findByBarcode_existingBarcode_returnsProduct() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.findByBarcode("8801045570716", pageable)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].barcode).isEqualTo("8801045570716")
            assertThat(result.content[0].productName).isEqualTo("열라면_용기105G")
        }

        @Test
        @DisplayName("존재하지 않는 바코드 검색 - 빈 결과 반환")
        fun findByBarcode_nonExistingBarcode_returnsEmpty() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.findByBarcode("0000000000000", pageable)

            // Then
            assertThat(result.content).isEmpty()
        }
    }

    // ========== 페이지네이션 Tests ==========

    @Nested
    @DisplayName("페이지네이션 검증")
    inner class PaginationTests {

        @Test
        @DisplayName("페이지 크기 2로 검색 - 정확한 페이지 정보 반환")
        fun searchByText_withPagination_returnsCorrectPage() {
            // Given
            val pageable = PageRequest.of(0, 2)

            // When
            val result = productRepository.searchByText("라면", pageable)

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(5) // 열라면 3개 + 진라면 2개
            assertThat(result.totalPages).isEqualTo(3) // 5/2 = 3페이지
            assertThat(result.isFirst).isTrue()
            assertThat(result.isLast).isFalse()
        }

        @Test
        @DisplayName("마지막 페이지 조회 - last=true")
        fun searchByText_lastPage_isLastTrue() {
            // Given
            val pageable = PageRequest.of(2, 2)

            // When
            val result = productRepository.searchByText("라면", pageable)

            // Then
            assertThat(result.content).hasSize(1) // 5개 중 마지막 1개
            assertThat(result.isLast).isTrue()
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun createProduct(
        productId: String,
        productName: String,
        productCode: String,
        barcode: String,
        categoryMid: String? = null,
        categorySub: String? = null
    ): Product {
        return Product(
            productId = productId,
            productName = productName,
            productCode = productCode,
            barcode = barcode,
            storageType = "상온",
            shelfLife = "7개월",
            categoryMid = categoryMid,
            categorySub = categorySub
        )
    }
}
