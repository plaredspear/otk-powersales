package com.otoki.powersales.domain.foundation.product.repository

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

/**
 * `searchForElectronicSalesAdvanced` (POS 매출 / 전산실적 제품 고급 검색) 쿼리 테스트.
 *
 * 검증 축:
 *  - 소비자 바코드 보유 제품 한정 (드롭다운 빠른 검색과 동일 집합)
 *  - 바코드가 여러 건인 제품이 페이징에서 중복 행으로 부풀지 않음 (EXISTS 사용 근거)
 *  - 키워드가 제품명/제품코드/소비자 바코드 3축 OR 매칭 (물류 바코드가 아님)
 *  - 대/중/소분류 + 제품상태 필터
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("ProductRepository.searchForElectronicSalesAdvanced 테스트")
class ProductRepositoryElectronicSalesAdvancedTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        testEntityManager.clear()

        // 바코드 2건 보유 — 중복 행 부풀림 검증용
        persist(
            product("진라면_매운맛", "18120002", category1 = "면류", category2 = "라면", category3 = "봉지"),
            barcodes = listOf("8801045571018", "8801045571025"),
        )
        // 바코드 1건 보유
        persist(
            product("진라면_순한맛", "18120001", category1 = "면류", category2 = "라면", category3 = "봉지"),
            barcodes = listOf("8801045571001"),
        )
        // 단종 상태 + 바코드 보유 — 상태 필터 검증용
        persist(
            product(
                "진라면_단종품",
                "18120003",
                category1 = "면류",
                category2 = "라면",
                category3 = "봉지",
                productStatus = ProductStatus.OUT_OF_STOCK,
            ),
            barcodes = listOf("8801045571032"),
        )
        // 다른 분류 — 분류 필터 검증용
        persist(
            product("오뚜기카레", "19110001", category1 = "간편식", category2 = "카레", category3 = "분말"),
            barcodes = listOf("8801045573001"),
        )
        // 바코드 미보유 — 결과에서 배제되어야 함 (POS UPC_CD IN 필터에 사용 불가)
        persist(
            product("진라면_바코드없음", "18120099", category1 = "면류", category2 = "라면", category3 = "봉지"),
            barcodes = emptyList(),
        )

        testEntityManager.clear()
    }

    @Nested
    @DisplayName("소비자 바코드 보유 제품 한정")
    inner class BarcodeRequiredTests {

        @Test
        @DisplayName("바코드 없는 제품은 키워드가 일치해도 반환하지 않는다")
        fun excludesProductWithoutBarcode() {
            val result = search(keyword = "진라면")

            assertThat(result.content.map { it.product.name })
                .contains("진라면_매운맛", "진라면_순한맛", "진라면_단종품")
                .doesNotContain("진라면_바코드없음")
        }

        @Test
        @DisplayName("바코드가 여러 건인 제품도 1행으로만 집계된다 (페이징 total 부풀림 방지)")
        fun doesNotDuplicateProductWithMultipleBarcodes() {
            val result = search(keyword = "진라면_매운맛")

            // 바코드 2건이지만 제품은 1건 — JOIN 이었다면 2행/total=2 가 된다.
            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("대표 바코드를 함께 반환한다 (다건이면 min)")
        fun returnsRepresentativeBarcode() {
            val result = search(keyword = "진라면_매운맛")

            // 화면이 드롭다운과 동일한 `제품명 (제품코드 / 바코드)` 라벨을 만들 수 있어야 한다.
            assertThat(result.content.first().barcode).isEqualTo("8801045571018")
        }
    }

    @Nested
    @DisplayName("키워드 검색 축")
    inner class KeywordTests {

        @Test
        @DisplayName("제품코드로 검색된다")
        fun searchesByProductCode() {
            val result = search(keyword = "18120001")

            assertThat(result.content.map { it.product.name }).containsExactly("진라면_순한맛")
        }

        @Test
        @DisplayName("소비자 바코드로 검색된다 (물류 바코드가 아니라 product_barcode)")
        fun searchesByConsumerBarcode() {
            // 매운맛의 두 번째 소비자 바코드 — logisticsBarcode 에는 없는 값이다.
            val result = search(keyword = "8801045571025")

            assertThat(result.content.map { it.product.name }).containsExactly("진라면_매운맛")
        }

        @Test
        @DisplayName("키워드 없이 분류 필터만으로도 검색된다")
        fun searchesByCategoryOnly() {
            val result = search(keyword = null, category2 = "카레")

            assertThat(result.content.map { it.product.name }).containsExactly("오뚜기카레")
        }
    }

    @Nested
    @DisplayName("분류 / 상태 필터")
    inner class FilterTests {

        @Test
        @DisplayName("대분류로 필터링된다")
        fun filtersByCategory1() {
            val result = search(keyword = null, category1 = "간편식")

            assertThat(result.content.map { it.product.name }).containsExactly("오뚜기카레")
        }

        @Test
        @DisplayName("소분류로 필터링된다")
        fun filtersByCategory3() {
            val result = search(keyword = null, category1 = "면류", category2 = "라면", category3 = "봉지")

            assertThat(result.content.map { it.product.name })
                .containsExactlyInAnyOrder("진라면_매운맛", "진라면_순한맛", "진라면_단종품")
        }

        @Test
        @DisplayName("제품상태 '단종' 으로 필터링된다")
        fun filtersByDiscontinuedStatus() {
            val result = search(keyword = "진라면", productStatus = ProductStatus.OUT_OF_STOCK.label)

            assertThat(result.content.map { it.product.name }).containsExactly("진라면_단종품")
        }

        @Test
        @DisplayName("제품상태 '판매중' 은 상태값이 없는(null) 제품을 가리킨다")
        fun defaultStatusMatchesNullStatus() {
            val result = search(keyword = "진라면", productStatus = ProductStatus.DEFAULT_LABEL)

            assertThat(result.content.map { it.product.name })
                .containsExactlyInAnyOrder("진라면_매운맛", "진라면_순한맛")
        }
    }

    @Nested
    @DisplayName("페이징")
    inner class PagingTests {

        @Test
        @DisplayName("size 만큼 잘라 반환하고 total 은 전체 건수를 유지한다")
        fun paginates() {
            val first = search(keyword = "진라면", page = 0, size = 2)

            assertThat(first.content).hasSize(2)
            assertThat(first.totalElements).isEqualTo(3)
            assertThat(first.totalPages).isEqualTo(2)

            val second = search(keyword = "진라면", page = 1, size = 2)
            assertThat(second.content).hasSize(1)

            // 두 페이지의 합집합이 전체와 일치 — 중복/누락 없음
            assertThat((first.content + second.content).map { it.product.id }.distinct()).hasSize(3)
        }
    }

    // ------------------- helpers -------------------

    private fun search(
        keyword: String?,
        category1: String? = null,
        category2: String? = null,
        category3: String? = null,
        productStatus: String? = null,
        page: Int = 0,
        size: Int = 20,
    ) = productRepository.searchForElectronicSalesAdvanced(
        keyword = keyword,
        category1 = category1,
        category2 = category2,
        category3 = category3,
        productStatus = productStatus,
        pageable = PageRequest.of(page, size),
    )

    private fun persist(product: Product, barcodes: List<String>) {
        val saved = testEntityManager.persistAndFlush(product)
        barcodes.forEach { barcode ->
            testEntityManager.persistAndFlush(
                ProductBarcode(productId = saved.id, unit = saved.unit, barcode = barcode),
            )
        }
    }

    private fun product(
        name: String,
        productCode: String,
        category1: String? = null,
        category2: String? = null,
        category3: String? = null,
        productStatus: ProductStatus? = null,
    ): Product = Product(
        name = name,
        productCode = productCode,
        // 소비자 바코드와 구분되는 값 — 키워드 검색이 물류 바코드를 타지 않음을 드러낸다.
        logisticsBarcode = "LOGI-$productCode",
        storageCondition = StorageCondition.ROOM_TEMP,
        shelfLife = "7개월",
        productCategory1 = category1,
        productCategory2 = category2,
        productCategory3 = category3,
        unit = "EA",
        productStatus = productStatus,
    )
}
