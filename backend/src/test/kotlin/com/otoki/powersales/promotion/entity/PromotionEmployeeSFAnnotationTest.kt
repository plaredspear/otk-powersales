package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #626 — PromotionEmployee ↔ Salesforce `DKRetail__PromotionEmployee__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/행사사원(DKRetail__PromotionEmployee__c).md
 */
@DisplayName("PromotionEmployee SF 어노테이션 검증 (Spec #626)")
class PromotionEmployeeSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__PromotionEmployee__c'")
        fun sfObjectValue() {
            val annotation = PromotionEmployee::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__PromotionEmployee__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 22 (Spec #740: WorkType4 + ProfessionalPromotionTeam Formula 2건 제거. 기존 15 + 신규 2 + BaseEntity 2 + R-2 3)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(PromotionEmployee::class.java)
            assertThat(mapping).hasSize(22)
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = PromotionEmployee::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 promotion_employee_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(PromotionEmployee::class.java)
            assertThat(mapping.values).doesNotContain("promotion_employee_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (기존 15개, Spec #740: WorkType4 + ProfessionalPromotionTeam 제거)")
    inner class ExistingSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(PromotionEmployee::class.java)

        @Test
        @DisplayName("기존 15개 SF API Name → 컬럼명 1:1")
        fun existingMappingValues() {
            assertThat(mapping["DKRetail__PromotionId__c"]).isEqualTo("promotion_sfid")
            assertThat(mapping["DKRetail__ScheduleDate__c"]).isEqualTo("schedule_date")
            assertThat(mapping["DKRetail__WorkStatus__c"]).isEqualTo("work_status")
            assertThat(mapping["DKRetail__WorkType1__c"]).isEqualTo("work_type1")
            assertThat(mapping["DKRetail__WorkType3__c"]).isEqualTo("work_type3")
            assertThat(mapping["DKRetail__ScheduleId__c"]).isEqualTo("team_member_schedule_sfid")
            assertThat(mapping["PromoCloseByTm__c"]).isEqualTo("promo_close_by_tm")
            assertThat(mapping["DKRetail__BasePrice__c"]).isEqualTo("base_price")
            assertThat(mapping["DKRetail__DailyTargetCount__c"]).isEqualTo("daily_target_count")
            assertThat(mapping["PrimaryProductAmount__c"]).isEqualTo("primary_product_amount")
            assertThat(mapping["DKRetail__PrimarySalesQuantity__c"]).isEqualTo("primary_sales_quantity")
            assertThat(mapping["DKRetail__PrimarySalesPrice__c"]).isEqualTo("primary_sales_price")
            assertThat(mapping["DKRetail__OtherSalesAmount__c"]).isEqualTo("other_sales_amount")
            assertThat(mapping["DKRetail__OtherSalesQuantity__c"]).isEqualTo("other_sales_quantity")
            assertThat(mapping["S3ImageUniqueKey__c"]).isEqualTo("s3_image_unique_key")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (신규 2개)")
    inner class NewSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(PromotionEmployee::class.java)

        @Test
        @DisplayName("신규 2개 SF API Name → 컬럼명 1:1")
        fun newMappingValues() {
            assertThat(mapping["Description__c"]).isEqualTo("description")
            assertThat(mapping["WorkType2__c"]).isEqualTo("work_type2")
        }
    }
}
