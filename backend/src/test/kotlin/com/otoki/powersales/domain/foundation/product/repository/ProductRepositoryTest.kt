package com.otoki.powersales.domain.foundation.product.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.org.employee.entity.Employee
import java.time.LocalDateTime
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
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
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition

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
        productStatus: ProductStatus? = null,
        categoryCode3: String? = null
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
            productStatus = productStatus,
            categoryCode3 = categoryCode3
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

    // ========== 즐겨찾기용 제품+대표바코드 조회 (findOrderRowsByProductCodes) ==========

    @Nested
    @DisplayName("findOrderRowsByProductCodes - 제품코드로 제품+발주단위 대표바코드 조회")
    inner class FindOrderRowsByProductCodes {

        @Test
        @DisplayName("발주 단위 매칭 대표 바코드를 함께 반환한다(logisticsBarcode 폴백 아님)")
        fun returnsRepresentativeBarcode() {
            val saved = testEntityManager.persistAndFlush(
                createProduct("즐겨찾기라면", "88880001", "9990000000001", unit = "EA")
            )
            testEntityManager.persistAndFlush(createBarcode(saved.id, "EA", "8801234567890"))
            testEntityManager.clear()

            val rows = productRepository.findOrderRowsByProductCodes(listOf("88880001"))

            assertThat(rows).hasSize(1)
            assertThat(rows[0].product.productCode).isEqualTo("88880001")
            assertThat(rows[0].barcode).isEqualTo("8801234567890")
        }

        @Test
        @DisplayName("orderable 필터 없이 조회 — 바코드 없는 제품도 barcode=null 로 반환한다")
        fun returnsProductWithoutBarcodeAsNull() {
            testEntityManager.persistAndFlush(
                createProduct("바코드없는즐겨찾기", "88880002", "9990000000002", category3 = "기타")
            )
            testEntityManager.clear()

            val rows = productRepository.findOrderRowsByProductCodes(listOf("88880002"))

            assertThat(rows).hasSize(1)
            assertThat(rows[0].barcode).isNull()
        }

        @Test
        @DisplayName("빈 코드 목록 → 빈 결과")
        fun emptyInput() {
            assertThat(productRepository.findOrderRowsByProductCodes(emptyList())).isEmpty()
        }
    }

    // ========== 주문서 제품검색 정렬 (레거시 selectProduct ORDER BY 이식) ==========

    @Nested
    @DisplayName("주문서 제품검색 정렬 - 레거시 ORDER BY categorycode3, productcode")
    inner class OrderSearchSortTests {

        /**
         * 제품명 정렬과 소분류코드 정렬의 결과 순서가 어긋나도록 배치한다.
         * 제품명 가나다순: 정렬가_A → 정렬나_B → 정렬다_C
         * 소분류코드,제품코드순: 정렬다_C(A10) → 정렬나_B(B20/70000002) → 정렬가_A(B20/70000003)
         */
        @BeforeEach
        fun setUpSortFixtures() {
            val fixtures = listOf(
                Triple("정렬가_A", "70000003", "B20"),
                Triple("정렬나_B", "70000002", "B20"),
                Triple("정렬다_C", "70000001", "A10"),
            )
            fixtures.forEach { (name, code, categoryCode3) ->
                val saved = testEntityManager.persistAndFlush(
                    createProduct(
                        productName = name,
                        productCode = code,
                        logisticsBarcode = "999$code",
                        category2 = "정렬중분류",
                        unit = "EA",
                        categoryCode3 = categoryCode3
                    )
                )
                testEntityManager.persistAndFlush(createBarcode(saved.id, "EA", "880$code"))
            }
            testEntityManager.clear()
        }

        @Test
        @DisplayName("searchForOrder - 소분류코드 → 제품코드 오름차순으로 정렬된다 (제품명순 아님)")
        fun searchForOrder_sortedByCategoryCode3ThenProductCode() {
            val result = productRepository.searchForOrder("정렬", null, null, PageRequest.of(0, 20))

            // 제품명 가나다순이면 정렬가_A 가 먼저 나오므로, 이 기대값은 제품명 정렬로 회귀하면 깨진다.
            val names = result.content.map { it.product.name }
            assertThat(names).containsExactly("정렬다_C", "정렬나_B", "정렬가_A")
        }

        @Test
        @DisplayName("searchByFilter - 소분류코드 → 제품코드 오름차순으로 정렬된다")
        fun searchByFilter_sortedByCategoryCode3ThenProductCode() {
            val result = productRepository.searchByFilter(
                null, null, "정렬중분류", null, PageRequest.of(0, 20)
            )

            val names = result.content.map { it.product.name }
            assertThat(names).containsExactly("정렬다_C", "정렬나_B", "정렬가_A")
        }
    }

    // ========== 최근 주문 제품 상단 정렬 ==========

    @Nested
    @DisplayName("최근 주문 제품 상단 정렬 (searchForOrder recentOrder*)")
    inner class RecentOrderSortTests {

        private var employeeId = 0L

        /**
         * 소분류코드 정렬상 맨 뒤인 제품(Z90)을 최근 주문분으로 만들어,
         * 최근주문 정렬키가 기존 정렬을 실제로 앞지르는지 확인한다.
         */
        @BeforeEach
        fun setUpRecentOrderFixtures() {
            val employee = testEntityManager.persistAndFlush(
                Employee(employeeCode = "E-RECENT", name = "테스터")
            )
            employeeId = employee.id

            val fixtures = listOf(
                Triple("최근가_A", "80000001", "A10"),
                Triple("최근나_B", "80000002", "B20"),
                Triple("최근다_C", "80000003", "Z90"),
            )
            val saved = fixtures.map { (name, code, categoryCode3) ->
                val p = testEntityManager.persistAndFlush(
                    createProduct(
                        productName = name,
                        productCode = code,
                        logisticsBarcode = "777$code",
                        unit = "EA",
                        categoryCode3 = categoryCode3
                    )
                )
                testEntityManager.persistAndFlush(createBarcode(p.id, "EA", "770$code"))
                p
            }

            // 정렬상 마지막인 최근다_C(Z90) 만 3일 전에 주문한 이력을 만든다.
            val order = testEntityManager.persistAndFlush(
                OrderRequest(
                    orderRequestNumber = "OP-RECENT-001",
                    employee = employee,
                    orderDate = LocalDateTime.now().minusDays(3),
                )
            )
            testEntityManager.persistAndFlush(
                OrderRequestProduct(orderRequest = order, product = saved[2])
            )
            testEntityManager.clear()
        }

        @Test
        @DisplayName("최근 주문 제품이 기존 정렬을 앞질러 맨 위로 올라온다")
        fun recentlyOrderedProductComesFirst() {
            val result = productRepository.searchForOrder(
                "최근", null, null, PageRequest.of(0, 20),
                recentOrderEmployeeId = employeeId,
                recentOrderFrom = LocalDateTime.now().minusDays(10),
            )

            val names = result.content.map { it.product.name }
            // 최근다_C 는 소분류코드(Z90)상 원래 맨 뒤지만 최근 주문이라 맨 앞으로 온다.
            assertThat(names).containsExactly("최근다_C", "최근가_A", "최근나_B")
            assertThat(result.content[0].recentlyOrdered).isTrue()
            assertThat(result.content[1].recentlyOrdered).isFalse()
        }

        @Test
        @DisplayName("최근주문 파라미터가 없으면 기존 정렬만 적용된다")
        fun withoutRecentOrderParams_keepsLegacySort() {
            val result = productRepository.searchForOrder(
                "최근", null, null, PageRequest.of(0, 20)
            )

            val names = result.content.map { it.product.name }
            assertThat(names).containsExactly("최근가_A", "최근나_B", "최근다_C")
            assertThat(result.content).allMatch { !it.recentlyOrdered }
        }

        @Test
        @DisplayName("기간 밖(10일 초과) 주문은 상단 정렬 대상이 아니다")
        fun orderOutsideWindow_isNotRecent() {
            val result = productRepository.searchForOrder(
                "최근", null, null, PageRequest.of(0, 20),
                recentOrderEmployeeId = employeeId,
                // 주문일(3일 전)보다 뒤인 기준일 → 윈도우 밖
                recentOrderFrom = LocalDateTime.now().minusDays(1),
            )

            val names = result.content.map { it.product.name }
            assertThat(names).containsExactly("최근가_A", "최근나_B", "최근다_C")
            assertThat(result.content).allMatch { !it.recentlyOrdered }
        }

        @Test
        @DisplayName("다른 사원의 주문은 상단 정렬 대상이 아니다")
        fun otherEmployeesOrder_isNotRecent() {
            val result = productRepository.searchForOrder(
                "최근", null, null, PageRequest.of(0, 20),
                recentOrderEmployeeId = employeeId + 999,
                recentOrderFrom = LocalDateTime.now().minusDays(10),
            )

            val names = result.content.map { it.product.name }
            assertThat(names).containsExactly("최근가_A", "최근나_B", "최근다_C")
            assertThat(result.content).allMatch { !it.recentlyOrdered }
        }
    }

    // ========== searchForAdmin 제품상태 필터 Tests ==========

    @Nested
    @DisplayName("searchForAdmin - 제품상태 필터 (화면 표시명 기준)")
    inner class SearchForAdminProductStatusTests {

        @BeforeEach
        fun seedStatusProducts() {
            // setUp() 의 7건은 productStatus = null (= "판매중") 이다. 단종 제품 1건을 추가한다.
            testEntityManager.persistAndFlush(
                createProduct(
                    "단종라면_봉지120G", "18990001", "8801045579001",
                    category1 = "라면", category2 = "봉지면",
                    productStatus = ProductStatus.OUT_OF_STOCK
                )
            )
            testEntityManager.clear()
        }

        @Test
        @DisplayName("'단종' 필터 - 출고중지 제품만 반환")
        fun filterByDiscontinued() {
            val result = productRepository.searchForAdmin(
                keyword = null, category1 = null, category2 = null, category3 = null,
                productStatus = "단종", pageable = PageRequest.of(0, 20)
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productCode).isEqualTo("18990001")
            assertThat(result.content[0].productStatus).isEqualTo(ProductStatus.OUT_OF_STOCK)
        }

        @Test
        @DisplayName("'판매중' 필터 - 상태값이 없는(null) 제품을 반환한다 (eq 아닌 isNull 매칭)")
        fun filterByOnSaleMatchesNullStatus() {
            val result = productRepository.searchForAdmin(
                keyword = null, category1 = null, category2 = null, category3 = null,
                productStatus = "판매중", pageable = PageRequest.of(0, 20)
            )

            // setUp() 의 7건 (전부 productStatus = null) 이 잡히고, 단종 1건은 제외된다.
            assertThat(result.content).hasSize(7)
            assertThat(result.content).allSatisfy { assertThat(it.productStatus).isNull() }
        }

        @Test
        @DisplayName("상태 미지정 - 판매중/단종 전건 반환")
        fun noStatusFilterReturnsAll() {
            val result = productRepository.searchForAdmin(
                keyword = null, category1 = null, category2 = null, category3 = null,
                productStatus = null, pageable = PageRequest.of(0, 20)
            )

            assertThat(result.content).hasSize(8)
        }

        @Test
        @DisplayName("저장값('출고중지')을 그대로 넘기면 매칭되지 않는다 — 파라미터는 표시명 기준")
        fun rawStoredValueIsNotAccepted() {
            val result = productRepository.searchForAdmin(
                keyword = null, category1 = null, category2 = null, category3 = null,
                productStatus = "출고중지", pageable = PageRequest.of(0, 20)
            )

            // 해소 실패 시 조건 자체가 붙지 않아 전건 반환 (기존 동작 유지).
            assertThat(result.content).hasSize(8)
        }
    }
}
