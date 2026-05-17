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
 * 단일 권위: Salesforce Object (`DKRetail__PromotionEmployee__c`)
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
        @DisplayName("매핑 키 수 = 24 (OwnerId 제거 — SF SObject 부재, V154)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(PromotionEmployee::class.java)
            assertThat(mapping).hasSize(24)
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
    @DisplayName("AC1 — @SFField 매핑 키셋 (신규 — sf-meta-diff Q7 WorkType2__c 제거 후)")
    inner class NewSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(PromotionEmployee::class.java)

        @Test
        @DisplayName("Description__c → description 1:1")
        fun newMappingValues() {
            assertThat(mapping["Description__c"]).isEqualTo("description")
        }

        @Test
        @DisplayName("WorkType2__c (SF Formula) 매핑 제거 확인")
        fun workType2FormulaMappingRemoved() {
            assertThat(mapping).doesNotContainKey("WorkType2__c")
        }
    }

    @Nested
    @DisplayName("sf-meta-diff Q8 — DKRetail__DailyActualSalesAmount__c Formula 재현 (의미 오류 포함 그대로)")
    inner class DkDailyActualSalesAmountFormula {

        @Test
        @DisplayName("SF 원본 공식: (price × primaryQty) + (otherQty × otherQty)")
        fun originalFormulaIncludingSemanticError() {
            val pe = newPe(price = 1000, primaryQty = 5, otherQty = 3)
            // (1000 × 5) + (3 × 3) = 5000 + 9 = 5009 — otherQty 를 두 번 곱하는 의미 오류 그대로 보존
            assertThat(pe.dkDailyActualSalesAmount).isEqualTo(5009L)
        }

        @Test
        @DisplayName("모든 입력이 null 이면 결과도 null")
        fun allNullReturnsNull() {
            val pe = newPe(price = null, primaryQty = null, otherQty = null)
            assertThat(pe.dkDailyActualSalesAmount).isNull()
        }

        @Test
        @DisplayName("일부 입력이 null 이면 null 항은 0 으로 계산")
        fun partialNullTreatedAsZero() {
            val pe = newPe(price = 1000, primaryQty = 5, otherQty = null)
            // (1000 × 5) + (0 × 0) = 5000
            assertThat(pe.dkDailyActualSalesAmount).isEqualTo(5000L)
        }

        @Test
        @DisplayName("otherQty 만 있으면 otherQty² 결과")
        fun otherQuantityOnly() {
            val pe = newPe(price = null, primaryQty = null, otherQty = 7)
            // (0 × 0) + (7 × 7) = 49 — 의미 오류 (수량 제곱)
            assertThat(pe.dkDailyActualSalesAmount).isEqualTo(49L)
        }

        private fun newPe(price: Long?, primaryQty: Long?, otherQty: Long?) = PromotionEmployee(
            promotionId = 1L,
            primarySalesPrice = price,
            primarySalesQuantity = primaryQty,
            otherSalesQuantity = otherQty
        )
    }
}
