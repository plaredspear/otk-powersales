package com.otoki.powersales.product.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #613 — Product ↔ Salesforce `DKRetail__Product__c` SF 누락 비수식 컬럼 도입 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/제품(DKRetail__Product__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (40개 — 33 기존 + 1 매핑 추가 + 6 신규. spec #650 으로 `StandardPrice__c` 매핑 제거)
 *   - AC3: PK 미부착
 */
@DisplayName("Product SF 어노테이션 검증 (Spec #613)")
class ProductSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__Product__c'")
        fun sfObjectValue() {
            val annotation = Product::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__Product__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (40개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Product::class.java)

        @Test
        @DisplayName("매핑 키 수 = 45 (42 + Spec #723 R-2 Owner/CreatedBy/LastModifiedBy 3)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(45)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 1개 신규 부착: Pallet__c → pallet")
        fun section62MissingMapping() {
            assertThat(mapping["Pallet__c"]).isEqualTo("pallet")
        }

        @Test
        @DisplayName("§6.3 — 신규 6개 필드 매핑")
        fun section63NewFields() {
            assertThat(mapping["DKRetail__Barcode__c"]).isEqualTo("barcode")
            assertThat(mapping["manufacture__c"]).isEqualTo("manufacture")
            assertThat(mapping["manufacture_detail__c"]).isEqualTo("manufacture_detail")
            assertThat(mapping["Claim_Management__c"]).isEqualTo("claim_management")
            assertThat(mapping["New_Product__c"]).isEqualTo("new_product_sfid")
            assertThat(mapping["StoreCondition__c"]).isEqualTo("store_condition_text")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 매핑 무변경 샘플")
        fun section61ExistingSample() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__ProductCode__c"]).isEqualTo("product_code")
            assertThat(mapping["DKRetail__StoreCondition__c"]).isEqualTo("storage_condition")
            assertThat(mapping["DKRetail__LogisticsBarCode__c"]).isEqualTo("logistics_barcode")
            assertThat(mapping["DKRetail__BoxReceivingQuantity__c"]).isEqualTo("box_receiving_quantity")
        }

        @Test
        @DisplayName("StoreCondition__c 와 DKRetail__StoreCondition__c 별개 매핑 존재")
        fun storeConditionFieldsAreDistinct() {
            assertThat(mapping["StoreCondition__c"]).isEqualTo("store_condition_text")
            assertThat(mapping["DKRetail__StoreCondition__c"]).isEqualTo("storage_condition")
        }
    }

    @Nested
    @DisplayName("AC3 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Product::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 product_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(Product::class.java)
            assertThat(mapping.values).doesNotContain("product_id")
        }
    }
}
