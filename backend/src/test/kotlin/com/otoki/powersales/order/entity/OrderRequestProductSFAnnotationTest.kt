package com.otoki.powersales.order.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #623 — OrderRequestProduct ↔ Salesforce `DKRetail__OrderRequestProduct__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/주문요청제품(DKRetail__OrderRequestProduct__c).md
 */
@DisplayName("OrderRequestProduct SF 어노테이션 검증 (Spec #623)")
class OrderRequestProductSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__OrderRequestProduct__c'")
        fun sfObjectValue() {
            val annotation = OrderRequestProduct::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__OrderRequestProduct__c")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (12개 = 기존 7 + 신규 5)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(OrderRequestProduct::class.java)

        @Test
        @DisplayName("매핑 키 수 = 14 (기존 7 + 신규 5 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(17)
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 7개 매핑 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["LineNumber__c"]).isEqualTo("line_number")
            assertThat(mapping["ProductCode__c"]).isEqualTo("product_code")
            assertThat(mapping["TotalQuantity_Box__c"]).isEqualTo("quantity_boxes")
            assertThat(mapping["TotalQuantity_Each__c"]).isEqualTo("quantity_pieces")
            assertThat(mapping["DKRetail__OrderingUnit__c"]).isEqualTo("unit")
            assertThat(mapping["DKRetail__TotalAmount__c"]).isEqualTo("amount")
            assertThat(mapping["DKRetail__LineChangeType__c"]).isEqualTo("is_cancelled")
        }

        @Test
        @DisplayName("§6.2 — 신규 도입 5개 매핑")
        fun section62NewMappings() {
            assertThat(mapping["Status__c"]).isEqualTo("status")
            assertThat(mapping["DKRetail__ProductId__c"]).isEqualTo("product_sfid")
            assertThat(mapping["DKRetail__Box__c"]).isEqualTo("box")
            assertThat(mapping["DKRetail__Piece__c"]).isEqualTo("piece")
            assertThat(mapping["DKRetail__BoxQuantity__c"]).isEqualTo("box_quantity")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / sfid 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = OrderRequestProduct::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례)")
        fun sfidHasNoSfField() {
            val field = OrderRequestProduct::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 order_request_product_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(OrderRequestProduct::class.java)
            assertThat(mapping.values).doesNotContain("order_request_product_id")
        }
    }
}
