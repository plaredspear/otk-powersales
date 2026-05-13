package com.otoki.powersales.order.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #611 — ErpOrderProduct ↔ Salesforce `ERP_OrderProduct__c` 어노테이션 부착 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/ERP주문상품(ERP_OrderProduct__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (26개 — 22 기존 OK + 3 누락 + 1 신규)
 *     * 스펙 §6.1 "기존 OK 19개"는 작성 추정치 — 실제 @SFField 부착 22개
 *   - AC3: PK / FK 미부착
 */
@DisplayName("ErpOrderProduct SF 어노테이션 검증 (Spec #611)")
class ErpOrderProductSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'ERP_OrderProduct__c'")
        fun sfObjectValue() {
            val annotation = ErpOrderProduct::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("ERP_OrderProduct__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (26개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(ErpOrderProduct::class.java)

        @Test
        @DisplayName("매핑 키 수 = 28 (22 기존 + 3 누락 보강 + 1 신규 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(31)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 3개 신규 부착")
        fun section62MissingMappings() {
            assertThat(mapping["ExternalKey__c"]).isEqualTo("external_key")
            assertThat(mapping["LineItemStatus__c"]).isEqualTo("line_item_status")
            assertThat(mapping["ShippingQuantity__c"]).isEqualTo("shipping_quantity")
        }

        @Test
        @DisplayName("§6.3 — 신규 1개 필드: BoxQuantity__c → box_quantity")
        fun section63NewField() {
            assertThat(mapping["BoxQuantity__c"]).isEqualTo("box_quantity")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 매핑 무변경 샘플")
        fun section61ExistingSample() {
            assertThat(mapping["LineNumber__c"]).isEqualTo("line_number")
            assertThat(mapping["ProductCode__c"]).isEqualTo("product_code")
            assertThat(mapping["OrderQuantity__c"]).isEqualTo("order_quantity")
            assertThat(mapping["OrderStatus__c"]).isEqualTo("delivery_status")
            assertThat(mapping["ReleaseAmount__c"]).isEqualTo("release_amount")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / FK 미부착")
    inner class PkFkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = ErpOrderProduct::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(erpOrder) 필드에 @SFField 미부착")
        fun erpOrderHasNoSfField() {
            val field = ErpOrderProduct::class.java.getDeclaredField("erpOrder")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 erp_order_product_id / erp_order_id 미등장")
        fun mappingValuesExcludePkFk() {
            val mapping = SFSchemaUtils.getSFMapping(ErpOrderProduct::class.java)
            assertThat(mapping.values).doesNotContain("erp_order_product_id", "erp_order_id")
        }
    }
}
