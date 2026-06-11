package com.otoki.powersales.product.repository

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.entity.ProductBarcode
import com.otoki.powersales.product.enums.ProductStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import com.otoki.powersales.common.config.QueryDslConfig
import com.otoki.powersales.product.enums.StorageCondition

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
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
            createProduct("열라면_용기105G", "18110014", "8801045570716", "라면", "용기면"),
            createProduct("열라면_용기115G", "18110007", "8801045570723", "라면", "용기면"),
            createProduct("열라면_봉지120G", "18110001", "8801045570730", "라면", "봉지면"),
            createProduct("진라면_순한맛_봉지120G", "18120001", "8801045571001", "라면", "봉지면"),
            createProduct("진라면_매운맛_봉지120G", "18120002", "8801045571018", "라면", "봉지면"),
            createProduct("오뚜기카레_약간매운맛100G", "19110001", "8801045573001", "즉석식품", "카레"),
            createProduct("오뚜기마요네스500G", "20110001", "8801045575001", "소스", "마요네스")
        )
        products.forEach { product ->
            val saved = testEntityManager.persistAndFlush(product)
            // 모바일 제품검색 고정 필터(단위 일치 바코드 존재)를 만족시키기 위한 바코드 시드
            testEntityManager.persistAndFlush(
                createBarcode(productId = saved.id, unit = saved.unit, barcode = saved.logisticsBarcode)
            )
        }
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
            assertThat(result.content).allSatisfy { row ->
                assertThat(row.product.name).containsIgnoringCase("열라면")
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
            assertThat(result.content).allSatisfy { row ->
                assertThat(row.product.name).containsIgnoringCase("진라면")
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
            assertThat(result.content).allSatisfy { row ->
                assertThat(row.product.productCode).contains("18110")
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
            val names = result.content.map { it.product.name }
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
            assertThat(result.content[0].product.logisticsBarcode).isEqualTo("8801045570716")
            // 단위 매칭 대표 바코드(레거시 productbarcode__c)가 함께 내려온다 (시드: barcode == logisticsBarcode)
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
            assertThat(result.content).anySatisfy { row ->
                assertThat(row.product.productCode).isEqualTo("18110014")
            }
        }
    }

    // ========== findByBarcode Tests ==========

    @Nested
    @DisplayName("findByBarcode - 소비자 바코드(ProductBarcode) 부분일치 검색")
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
            assertThat(result.content[0].product.name).isEqualTo("열라면_용기105G")
            assertThat(result.content[0].barcode).isEqualTo("8801045570716")
        }

        @Test
        @DisplayName("바코드 일부로 검색 - 부분일치 결과 반환")
        fun findByBarcode_partialBarcode_returnsMatches() {
            // Given
            val pageable = PageRequest.of(0, 20)

            // When
            val result = productRepository.findByBarcode("88010455707", pageable)

            // Then
            assertThat(result.content).anySatisfy { row ->
                assertThat(row.barcode).isEqualTo("8801045570716")
            }
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
        productName: String,
        productCode: String,
        logisticsBarcode: String,
        category1: String? = null,
        category2: String? = null,
        category3: String? = "가정",
        unit: String = "EA",
        productStatus: ProductStatus? = null
    ): Product {
        return Product(
            name = productName,
            productCode = productCode,
            logisticsBarcode = logisticsBarcode,
            storageCondition = StorageCondition.ROOM_TEMP,
            shelfLife = "7개월",
            productCategory1 = category1,
            productCategory2 = category2,
            productCategory3 = category3,
            unit = unit,
            productStatus = productStatus
        )
    }

    private fun createBarcode(
        productId: Long,
        unit: String?,
        barcode: String?
    ): ProductBarcode {
        return ProductBarcode(
            productId = productId,
            unit = unit,
            barcode = barcode
        )
    }

    // ========== 모바일 제품검색 고정 필터 (레거시 selectProduct 이식) ==========

    @Nested
    @DisplayName("제품검색 고정 필터 - 단위 일치 바코드 / category3(가정·업소) / productStatus null")
    inner class OrderableFilter {

        @Test
        @DisplayName("단위 일치 바코드가 없으면 검색 결과에서 제외된다")
        fun excludesProductWithoutBarcode() {
            testEntityManager.persistAndFlush(
                createProduct("바코드없는라면", "99990001", "8801045579990")
            )
            // 바코드 미시드
            testEntityManager.clear()

            val result = productRepository.searchByText("바코드없는라면", PageRequest.of(0, 20))

            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("바코드 단위가 제품 단위와 다르면 제외된다")
        fun excludesProductWithUnitMismatchedBarcode() {
            val saved = testEntityManager.persistAndFlush(
                createProduct("단위불일치라면", "99990002", "8801045579991", unit = "EA")
            )
            testEntityManager.persistAndFlush(createBarcode(saved.id, "BOX", "8801045579991"))
            testEntityManager.clear()

            val result = productRepository.searchByText("단위불일치라면", PageRequest.of(0, 20))

            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("소분류(category3)가 가정/업소가 아니면 제외된다")
        fun excludesProductNotInOrderableCategory3() {
            val saved = testEntityManager.persistAndFlush(
                createProduct("기타카테고리라면", "99990003", "8801045579992", category3 = "기타")
            )
            testEntityManager.persistAndFlush(createBarcode(saved.id, "EA", "8801045579992"))
            testEntityManager.clear()

            val result = productRepository.searchByText("기타카테고리라면", PageRequest.of(0, 20))

            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("업소 소분류 제품도 검색된다")
        fun includesEopsoCategory3() {
            val saved = testEntityManager.persistAndFlush(
                createProduct("업소용라면", "99990004", "8801045579993", category3 = "업소")
            )
            testEntityManager.persistAndFlush(createBarcode(saved.id, "EA", "8801045579993"))
            testEntityManager.clear()

            val result = productRepository.searchByText("업소용라면", PageRequest.of(0, 20))

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("productStatus 가 설정된 제품(비활성)은 제외된다")
        fun excludesProductWithNonNullStatus() {
            val saved = testEntityManager.persistAndFlush(
                createProduct(
                    "단종라면", "99990005", "8801045579994",
                    productStatus = ProductStatus.PLACEHOLDER
                )
            )
            testEntityManager.persistAndFlush(createBarcode(saved.id, "EA", "8801045579994"))
            testEntityManager.clear()

            val result = productRepository.searchByText("단종라면", PageRequest.of(0, 20))

            assertThat(result.content).isEmpty()
        }
    }
}
