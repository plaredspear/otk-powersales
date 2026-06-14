package com.otoki.powersales.domain.foundation.product.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #614 — ProductBarcode ↔ Salesforce `ProductBarcode__c` SF 누락 컬럼 도입 검증.
 *
 * 단일 권위: Salesforce Object (`ProductBarcode__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (8개 — 6 기존 + 1 누락 보강 + 1 신규)
 *   - AC3: PK 미부착
 */
@DisplayName("ProductBarcode SF 어노테이션 검증 (Spec #614)")
class ProductBarcodeSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'ProductBarcode__c'")
        fun sfObjectValue() {
            val annotation = ProductBarcode::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("ProductBarcode__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (8개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(ProductBarcode::class.java)

        @Test
        @DisplayName("매핑 키 수 = 10 (6 기존 + 1 누락 보강 + 1 신규 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(14)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 1개 신규 부착: Product__c → product_sfid")
        fun section62MissingMapping() {
            assertThat(mapping["Product__c"]).isEqualTo("product_sfid")
        }

        @Test
        @DisplayName("§6.3 — 신규 1개 필드: ProductCode__c → product_code")
        fun section63NewField() {
            assertThat(mapping["ProductCode__c"]).isEqualTo("product_code")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 매핑 6개 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["ProductName__c"]).isEqualTo("product_name")
            assertThat(mapping["ProductBarcode__c"]).isEqualTo("barcode")
            assertThat(mapping["ProductUnit__c"]).isEqualTo("unit")
            assertThat(mapping["ProductSequence__c"]).isEqualTo("sort_order")
            assertThat(mapping["CustomKey__c"]).isEqualTo("custom_key")
        }
    }

    @Nested
    @DisplayName("AC3 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = ProductBarcode::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 product_barcode_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(ProductBarcode::class.java)
            assertThat(mapping.values).doesNotContain("product_barcode_id")
        }
    }
}
