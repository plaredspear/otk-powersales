package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import jakarta.persistence.Id
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #625 — Promotion ↔ Salesforce `DKRetail__Promotion__c` 어노테이션 검증.
 *
 * 단일 권위: Salesforce Object (`DKRetail__Promotion__c`)
 */
@DisplayName("Promotion SF 어노테이션 검증 (Spec #625)")
class PromotionSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__Promotion__c'")
        fun sfObjectValue() {
            val annotation = Promotion::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__Promotion__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 20 (sf-meta-diff Q2/Q4/Q5 제거. 도메인 12 + Q3 Formula 잔존 2 + SF 표준 시스템 3 + IsDeleted 1 + BaseEntity 2)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(Promotion::class.java)
            assertThat(mapping).hasSize(20)
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Promotion::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 promotion_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(Promotion::class.java)
            assertThat(mapping.values).doesNotContain("promotion_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (도메인 12개, sf-meta-diff Q5 deprecated_acc_sfid 제거)")
    inner class DomainSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Promotion::class.java)

        @Test
        @DisplayName("도메인 12개 SF API Name → 컬럼명 1:1")
        fun domainMappingValues() {
            assertThat(mapping["Name"]).isEqualTo("promotion_number")
            assertThat(mapping["DKRetail__PromotionType__c"]).isEqualTo("promotion_type_id")
            assertThat(mapping["AccId__c"]).isEqualTo("account_sfid")
            assertThat(mapping["DKRetail__StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["DKRetail__EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["DKRetail__PrimaryProductId__c"]).isEqualTo("primary_product_sfid")
            assertThat(mapping["DKRetail__OtherProduct__c"]).isEqualTo("other_product")
            assertThat(mapping["DKRetail__Message__c"]).isEqualTo("message")
            assertThat(mapping["DKRetail__StandLocation__c"]).isEqualTo("stand_location")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["DKRetail__Remark__c"]).isEqualTo("remark")
            assertThat(mapping["DKRetail__ProductType__c"]).isEqualTo("product_type")

            // Q5 (sf-meta-diff): DKRetail__AccId__c (Label="사용안함", E 분류) 매핑 제거
            assertThat(mapping).doesNotContainKey("DKRetail__AccId__c")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (SF 표준 시스템 3개)")
    inner class StandardSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Promotion::class.java)

        @Test
        @DisplayName("SF 표준 시스템 3개 SF API Name → 컬럼명 1:1")
        fun standardMappingValues() {
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }
    }
}
