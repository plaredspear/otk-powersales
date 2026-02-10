package com.otoki.internal.service

import com.otoki.internal.entity.Product
import com.otoki.internal.exception.InvalidSearchParameterException
import com.otoki.internal.exception.InvalidSearchTypeException
import com.otoki.internal.repository.ProductRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductService 테스트")
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var productService: ProductService

    // ========== 텍스트 검색 Tests ==========

    @Nested
    @DisplayName("텍스트 검색 (type=text)")
    inner class TextSearch {

        @Test
        @DisplayName("제품명 검색 성공 - 검색어에 해당하는 제품 목록 반환")
        fun searchProducts_textSearch_success() {
            // Given
            val products = listOf(
                createTestProduct("18110014", "열라면_용기105G", "18110014", "8801045570716"),
                createTestProduct("18110007", "열라면_용기115G", "18110007", "8801045570723")
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 2)
            whenever(productRepository.searchByText(eq("열라면"), any())).thenReturn(page)

            // When
            val result = productService.searchProducts("열라면", "text", 0, 20)

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].productName).isEqualTo("열라면_용기105G")
            assertThat(result.content[1].productName).isEqualTo("열라면_용기115G")
            assertThat(result.totalElements).isEqualTo(2)
            verify(productRepository).searchByText(eq("열라면"), any())
        }

        @Test
        @DisplayName("숫자 검색어 - 제품코드/바코드도 함께 검색")
        fun searchProducts_numericQuery_searchesBarcodeToo() {
            // Given
            val products = listOf(
                createTestProduct("18110014", "열라면_용기105G", "18110014", "8801045570716")
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 1)
            whenever(productRepository.searchByTextIncludingBarcode(eq("18110014"), any())).thenReturn(page)

            // When
            val result = productService.searchProducts("18110014", "text", 0, 20)

            // Then
            assertThat(result.content).hasSize(1)
            verify(productRepository).searchByTextIncludingBarcode(eq("18110014"), any())
        }

        @Test
        @DisplayName("빈 결과 - 빈 Page 반환")
        fun searchProducts_noResults_returnsEmptyPage() {
            // Given
            val page = PageImpl<Product>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(productRepository.searchByText(eq("존재하지않는제품"), any())).thenReturn(page)

            // When
            val result = productService.searchProducts("존재하지않는제품", "text", 0, 20)

            // Then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("검색어 1자 - INVALID_PARAMETER 예외 발생")
        fun searchProducts_shortQuery_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("열", "text", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("2자 이상")
        }

        @Test
        @DisplayName("빈 검색어 - INVALID_PARAMETER 예외 발생")
        fun searchProducts_emptyQuery_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("", "text", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
        }

        @Test
        @DisplayName("공백만 있는 검색어 - INVALID_PARAMETER 예외 발생")
        fun searchProducts_blankQuery_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("   ", "text", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
        }
    }

    // ========== 바코드 검색 Tests ==========

    @Nested
    @DisplayName("바코드 검색 (type=barcode)")
    inner class BarcodeSearch {

        @Test
        @DisplayName("바코드 정확 일치 검색 성공")
        fun searchProducts_barcodeSearch_success() {
            // Given
            val products = listOf(
                createTestProduct("18110014", "열라면_용기105G", "18110014", "8801045570716")
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 1)
            whenever(productRepository.findByBarcode(eq("8801045570716"), any())).thenReturn(page)

            // When
            val result = productService.searchProducts("8801045570716", "barcode", 0, 20)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].barcode).isEqualTo("8801045570716")
            verify(productRepository).findByBarcode(eq("8801045570716"), any())
        }

        @Test
        @DisplayName("바코드 형식 오류 (숫자가 아닌 값) - INVALID_PARAMETER 예외")
        fun searchProducts_invalidBarcodeFormat_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("abc12345", "barcode", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("바코드 형식")
        }

        @Test
        @DisplayName("바코드 길이 부족 (8자 미만) - INVALID_PARAMETER 예외")
        fun searchProducts_shortBarcode_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("1234567", "barcode", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("바코드 형식")
        }

        @Test
        @DisplayName("빈 바코드 - INVALID_PARAMETER 예외")
        fun searchProducts_emptyBarcode_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("", "barcode", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
        }
    }

    // ========== 검색 유형 검증 Tests ==========

    @Nested
    @DisplayName("검색 유형 검증")
    inner class SearchTypeValidation {

        @Test
        @DisplayName("잘못된 검색 유형 - INVALID_SEARCH_TYPE 예외")
        fun searchProducts_invalidType_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("열라면", "invalid", 0, 20)
            }.isInstanceOf(InvalidSearchTypeException::class.java)
        }
    }

    // ========== 페이지네이션 검증 Tests ==========

    @Nested
    @DisplayName("페이지네이션 검증")
    inner class PaginationValidation {

        @Test
        @DisplayName("페이지네이션 - 두 번째 페이지 조회")
        fun searchProducts_pagination_secondPage() {
            // Given
            val products = listOf(
                createTestProduct("18130001", "참깨라면_봉지115G", "18130001", "8801045572001")
            )
            val page = PageImpl(products, PageRequest.of(1, 5), 6)
            whenever(productRepository.searchByText(eq("라면"), any())).thenReturn(page)

            // When
            val result = productService.searchProducts("라면", "text", 1, 5)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.number).isEqualTo(1)
            assertThat(result.size).isEqualTo(5)
            assertThat(result.totalElements).isEqualTo(6)
            assertThat(result.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("음수 페이지 번호 - INVALID_PARAMETER 예외")
        fun searchProducts_negativePage_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("열라면", "text", -1, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("페이지 번호")
        }

        @Test
        @DisplayName("페이지 크기 0 - INVALID_PARAMETER 예외")
        fun searchProducts_zeroSize_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("열라면", "text", 0, 0)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("페이지 크기")
        }

        @Test
        @DisplayName("페이지 크기 초과 (101) - INVALID_PARAMETER 예외")
        fun searchProducts_oversizeSize_throwsException() {
            // When & Then
            assertThatThrownBy {
                productService.searchProducts("열라면", "text", 0, 101)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("페이지 크기")
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun createTestProduct(
        productId: String,
        productName: String,
        productCode: String,
        barcode: String,
        storageType: String = "상온",
        shelfLife: String = "7개월",
        categoryMid: String = "라면",
        categorySub: String = "봉지면"
    ): Product {
        return Product(
            productId = productId,
            productName = productName,
            productCode = productCode,
            barcode = barcode,
            storageType = storageType,
            shelfLife = shelfLife,
            categoryMid = categoryMid,
            categorySub = categorySub
        )
    }
}
