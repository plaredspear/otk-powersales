package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.dto.response.ProductAppVisibilityIssue
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.ProductType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 앱 노출/주문 진단 판정 — 모바일 검색 술어(`orderableProductFilter`) + 주문 차단 규칙 정합 검증.
 *
 * 회귀 기준점: 제품 18010406(한강라면 110GX4) 은 제품상태 판매중 / 소분류 가정 / 전용·시식 아님인데
 * 바코드 마스터가 0건이라 앱 검색에서 빠졌고, 관리자 화면이 그 사실을 보여주지 못해 개발자 문의로 넘어왔다.
 */
@DisplayName("ProductAppVisibilityEvaluator 테스트")
class ProductAppVisibilityEvaluatorTest {

    private fun product(
        productCode: String = "18010406",
        unit: String? = "BOX",
        category3: String? = "가정",
        productStatus: ProductStatus? = null,
        productType: ProductType? = null,
        tasteGift: String? = null,
    ) = Product(
        productCode = productCode,
        name = "한강라면 (110GX4)",
        unit = unit,
        productCategory3 = category3,
        productStatus = productStatus,
        productType = productType,
        tasteGift = tasteGift,
    )

    private fun barcode(unit: String?, value: String? = "8801234567890", isDeleted: Boolean? = null) =
        ProductBarcode(barcode = value, unit = unit, isDeleted = isDeleted)

    @Nested
    @DisplayName("검색 노출 판정")
    inner class Searchable {

        @Test
        @DisplayName("발주단위와 같은 단위의 바코드가 있으면 노출 — 사유 없음")
        fun visibleWhenUnitMatchedBarcodeExists() {
            val result = ProductAppVisibilityEvaluator.evaluate(product(), listOf(barcode(unit = "BOX")))

            assertThat(result.searchable).isTrue()
            assertThat(result.orderable).isTrue()
            assertThat(result.issues).isEmpty()
        }

        @Test
        @DisplayName("바코드 마스터 0건 → 미노출 + 전송 누락을 가리키는 사유")
        fun hiddenWhenNoBarcodeRow() {
            val result = ProductAppVisibilityEvaluator.evaluate(product(), emptyList())

            assertThat(result.searchable).isFalse()
            assertThat(result.issues).singleElement()
                .satisfies({
                    assertThat(it.code).isEqualTo("BARCODE_NOT_REGISTERED")
                    assertThat(it.scope).isEqualTo(ProductAppVisibilityIssue.SCOPE_SEARCH)
                    assertThat(it.action).contains("바코드 마스터")
                })
        }

        @Test
        @DisplayName("바코드는 있으나 발주단위(PAC)와 단위가 다르면 미노출 — 양쪽 단위를 사유에 노출")
        fun hiddenWhenBarcodeUnitMismatch() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(unit = "PAC"),
                listOf(barcode(unit = "BOX"), barcode(unit = "EA")),
            )

            assertThat(result.searchable).isFalse()
            val issue = result.issues.single { it.code == "BARCODE_UNIT_MISMATCH" }
            assertThat(issue.message).contains("PAC").contains("BOX").contains("EA")
        }

        @Test
        @DisplayName("바코드 값이 비어 있는 행은 없는 것으로 본다 (검색 쿼리 barcode IS NOT NULL 정합)")
        fun blankBarcodeTreatedAsAbsent() {
            val result = ProductAppVisibilityEvaluator.evaluate(product(), listOf(barcode(unit = "BOX", value = null)))

            assertThat(result.searchable).isFalse()
            assertThat(result.issues.single().code).isEqualTo("BARCODE_NOT_REGISTERED")
        }

        @Test
        @DisplayName("소프트 삭제된 바코드도 매칭에 포함 — 검색 쿼리가 is_deleted 를 보지 않는다")
        fun softDeletedBarcodeStillCounts() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(),
                listOf(barcode(unit = "BOX", isDeleted = true)),
            )

            assertThat(result.searchable).isTrue()
        }

        @Test
        @DisplayName("제품 발주단위가 없으면 매칭 불가 — 컬럼 간 등치라 SQL 에서도 NULL 은 매칭되지 않는다")
        fun nullProductUnitNeverMatches() {
            val result = ProductAppVisibilityEvaluator.evaluate(product(unit = null), listOf(barcode(unit = null)))

            assertThat(result.searchable).isFalse()
            assertThat(result.issues.single { it.code == "BARCODE_UNIT_MISMATCH" }).isNotNull()
        }

        @Test
        @DisplayName("소분류가 가정/업소가 아니면 미노출")
        fun hiddenWhenCategory3NotOrderable() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(category3 = "특판"),
                listOf(barcode(unit = "BOX")),
            )

            assertThat(result.searchable).isFalse()
            assertThat(result.issues.single().code).isEqualTo("CATEGORY3_NOT_ORDERABLE")
        }

        @Test
        @DisplayName("제품상태 값이 있으면(단종) 미노출 — 표시명과 원본값을 함께 알려준다")
        fun hiddenWhenProductStatusSet() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(productStatus = ProductStatus.OUT_OF_STOCK),
                listOf(barcode(unit = "BOX")),
            )

            assertThat(result.searchable).isFalse()
            val issue = result.issues.single { it.code == "PRODUCT_STATUS_SET" }
            assertThat(issue.message).contains("단종").contains("출고중지")
        }

        @Test
        @DisplayName("사유가 여러 개면 전부 보고한다")
        fun reportsAllReasons() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(category3 = "특판", productStatus = ProductStatus.OUT_OF_STOCK),
                emptyList(),
            )

            assertThat(result.issues.map { it.code })
                .containsExactlyInAnyOrder("BARCODE_NOT_REGISTERED", "CATEGORY3_NOT_ORDERABLE", "PRODUCT_STATUS_SET")
        }
    }

    @Nested
    @DisplayName("주문 담기 판정")
    inner class Orderable {

        @Test
        @DisplayName("전용상품은 검색은 되지만 주문 담기 불가")
        fun exclusiveProductBlocksOrderOnly() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(productType = ProductType.PRODUCT_TYPE_2),
                listOf(barcode(unit = "BOX")),
            )

            assertThat(result.searchable).isTrue()
            assertThat(result.orderable).isFalse()
            assertThat(result.issues.single().scope).isEqualTo(ProductAppVisibilityIssue.SCOPE_ORDER)
        }

        @Test
        @DisplayName("전용상품 예외 제품코드(20010042)는 주문 가능 — 레거시 하드코딩 정합")
        fun exclusiveExemptCodeIsOrderable() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(productCode = "20010042", productType = ProductType.PRODUCT_TYPE_2),
                listOf(barcode(unit = "BOX")),
            )

            assertThat(result.orderable).isTrue()
            assertThat(result.issues).isEmpty()
        }

        @Test
        @DisplayName("시식·증정용은 대소문자 무관하게 주문 담기 불가 (예외 코드 없음)")
        fun tastingGiftBlocksOrder() {
            listOf("x", "X").forEach { flag ->
                val result = ProductAppVisibilityEvaluator.evaluate(
                    product(productCode = "20010042", tasteGift = flag),
                    listOf(barcode(unit = "BOX")),
                )

                assertThat(result.orderable).isFalse()
                assertThat(result.issues.single().code).isEqualTo("TASTING_GIFT")
            }
        }

        @Test
        @DisplayName("tasteGift 가 무관한 값이면 주문 가능 (오차단 방지)")
        fun unrelatedTasteGiftValuePasses() {
            val result = ProductAppVisibilityEvaluator.evaluate(
                product(tasteGift = "1"),
                listOf(barcode(unit = "BOX")),
            )

            assertThat(result.orderable).isTrue()
        }
    }
}
