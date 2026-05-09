package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #627 — ProfessionalPromotionTeamHistory ↔ Salesforce `ProfessionalPromotionTeamHistory__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/전문행사조 이력(ProfessionalPromotionTeamHistory__c).md
 */
@DisplayName("ProfessionalPromotionTeamHistory SF 어노테이션 검증 (Spec #627)")
class ProfessionalPromotionTeamHistorySFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'ProfessionalPromotionTeamHistory__c'")
        fun sfObjectValue() {
            val annotation = ProfessionalPromotionTeamHistory::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("ProfessionalPromotionTeamHistory__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 4")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamHistory::class.java)
            assertThat(mapping).hasSize(4)
        }
    }

    @Nested
    @DisplayName("AC1 — PK·FK 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = ProfessionalPromotionTeamHistory::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = ProfessionalPromotionTeamHistory::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 professional_promotion_team_history_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamHistory::class.java)
            assertThat(mapping.values).doesNotContain("professional_promotion_team_history_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (4개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamHistory::class.java)

        @Test
        @DisplayName("4개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["oldValue__c"]).isEqualTo("old_value")
            assertThat(mapping["newValue__c"]).isEqualTo("new_value")
            assertThat(mapping["updateTime__c"]).isEqualTo("changed_at")
        }
    }
}
