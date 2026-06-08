package com.otoki.powersales.product.service

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.enums.StorageCondition
import com.otoki.powersales.product.exception.InvalidSearchParameterException
import com.otoki.powersales.product.exception.InvalidSearchTypeException
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.product.repository.ProductSearchRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@DisplayName("ProductService 테스트")
class ProductServiceTest {

    private val productRepository: ProductRepository = mockk()

    private val productService = ProductService(
        productRepository,
    )

    @Nested
    @DisplayName("텍스트 검색 (type=text)")
    inner class TextSearch {

        @Test
        @DisplayName("제품명 검색 성공 - 검색어에 해당하는 제품 목록 반환")
        fun searchProducts_textSearch_success() {
            val products = listOf(
                createTestProduct("18110014", "열라면_용기105G", "18110014", "8801045570716"),
                createTestProduct("18110007", "열라면_용기115G", "18110007", "8801045570723")
            )
            val page = rowPage(products, PageRequest.of(0, 20), 2)
            every { productRepository.searchByText("열라면", any()) } returns page

            val result = productService.searchProducts("열라면", "text", 0, 20)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].productName).isEqualTo("열라면_용기105G")
            assertThat(result.content[1].productName).isEqualTo("열라면_용기115G")
            assertThat(result.totalElements).isEqualTo(2)
            verify { productRepository.searchByText("열라면", any()) }
        }

        @Test
        @DisplayName("숫자 검색어 - 제품코드/바코드도 함께 검색")
        fun searchProducts_numericQuery_searchesBarcodeToo() {
            val products = listOf(
                createTestProduct("18110014", "열라면_용기105G", "18110014", "8801045570716")
            )
            val page = rowPage(products, PageRequest.of(0, 20), 1)
            every { productRepository.searchByTextIncludingBarcode("18110014", any()) } returns page

            val result = productService.searchProducts("18110014", "text", 0, 20)

            assertThat(result.content).hasSize(1)
            verify { productRepository.searchByTextIncludingBarcode("18110014", any()) }
        }

        @Test
        @DisplayName("빈 결과 - 빈 Page 반환")
        fun searchProducts_noResults_returnsEmptyPage() {
            val page = PageImpl<ProductSearchRow>(emptyList(), PageRequest.of(0, 20), 0)
            every { productRepository.searchByText("존재하지않는제품", any()) } returns page

            val result = productService.searchProducts("존재하지않는제품", "text", 0, 20)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("검색어 1자 - INVALID_PARAMETER 예외 발생")
        fun searchProducts_shortQuery_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("열", "text", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("2자 이상")
        }

        @Test
        @DisplayName("빈 검색어 - INVALID_PARAMETER 예외 발생")
        fun searchProducts_emptyQuery_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("", "text", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
        }

        @Test
        @DisplayName("공백만 있는 검색어 - INVALID_PARAMETER 예외 발생")
        fun searchProducts_blankQuery_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("   ", "text", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("바코드 검색 (type=barcode)")
    inner class BarcodeSearch {

        @Test
        @DisplayName("바코드 정확 일치 검색 성공")
        fun searchProducts_barcodeSearch_success() {
            val products = listOf(
                createTestProduct("18110014", "열라면_용기105G", "18110014", "8801045570716")
            )
            val page = rowPage(products, PageRequest.of(0, 20), 1)
            every { productRepository.findByLogisticsBarcode("8801045570716", any()) } returns page

            val result = productService.searchProducts("8801045570716", "barcode", 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].barcode).isEqualTo("8801045570716")
            assertThat(result.content[0].logisticsBarcode).isEqualTo("8801045570716")
            verify { productRepository.findByLogisticsBarcode("8801045570716", any()) }
        }

        @Test
        @DisplayName("바코드 형식 오류 (숫자가 아닌 값) - INVALID_PARAMETER 예외")
        fun searchProducts_invalidBarcodeFormat_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("abc12345", "barcode", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("바코드 형식")
        }

        @Test
        @DisplayName("바코드 길이 부족 (8자 미만) - INVALID_PARAMETER 예외")
        fun searchProducts_shortBarcode_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("1234567", "barcode", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("바코드 형식")
        }

        @Test
        @DisplayName("빈 바코드 - INVALID_PARAMETER 예외")
        fun searchProducts_emptyBarcode_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("", "barcode", 0, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("검색 유형 검증")
    inner class SearchTypeValidation {

        @Test
        @DisplayName("잘못된 검색 유형 - INVALID_SEARCH_TYPE 예외")
        fun searchProducts_invalidType_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("열라면", "invalid", 0, 20)
            }.isInstanceOf(InvalidSearchTypeException::class.java)
        }
    }

    @Nested
    @DisplayName("페이지네이션 검증")
    inner class PaginationValidation {

        @Test
        @DisplayName("페이지네이션 - 두 번째 페이지 조회")
        fun searchProducts_pagination_secondPage() {
            val products = listOf(
                createTestProduct("18130001", "참깨라면_봉지115G", "18130001", "8801045572001")
            )
            val page = rowPage(products, PageRequest.of(1, 5), 6)
            every { productRepository.searchByText("라면", any()) } returns page

            val result = productService.searchProducts("라면", "text", 1, 5)

            assertThat(result.content).hasSize(1)
            assertThat(result.number).isEqualTo(1)
            assertThat(result.size).isEqualTo(5)
            assertThat(result.totalElements).isEqualTo(6)
            assertThat(result.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("음수 페이지 번호 - INVALID_PARAMETER 예외")
        fun searchProducts_negativePage_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("열라면", "text", -1, 20)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("페이지 번호")
        }

        @Test
        @DisplayName("페이지 크기 0 - INVALID_PARAMETER 예외")
        fun searchProducts_zeroSize_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("열라면", "text", 0, 0)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("페이지 크기")
        }

        @Test
        @DisplayName("페이지 크기 초과 (101) - INVALID_PARAMETER 예외")
        fun searchProducts_oversizeSize_throwsException() {
            assertThatThrownBy {
                productService.searchProducts("열라면", "text", 0, 101)
            }.isInstanceOf(InvalidSearchParameterException::class.java)
                .hasMessageContaining("페이지 크기")
        }
    }

    /** 제품 목록을 검색 결과 행(ProductSearchRow) 페이지로 감싼다. 바코드는 logisticsBarcode 로 시드. */
    private fun rowPage(products: List<Product>, pageable: PageRequest, total: Long) =
        PageImpl(products.map { ProductSearchRow(it, it.logisticsBarcode) }, pageable, total)

    private fun createTestProduct(
        productId: String,
        productName: String,
        productCode: String,
        barcode: String,
        storageCondition: String = "실온",
        shelfLife: String = "7개월",
        category1: String = "라면",
        category2: String = "봉지면"
    ): Product {
        return Product(
            name = productName,
            productCode = productCode,
            logisticsBarcode = barcode,
            storageCondition = StorageCondition.fromDisplayNameOrNull(storageCondition),
            shelfLife = shelfLife,
            productCategory1 = category1,
            productCategory2 = category2
        )
    }
}
