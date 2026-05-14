package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #727 — ProfessionalPromotionTeamHistory ↔ Salesforce `ProfessionalPromotionTeamHistory__c` 어노테이션 검증.
 *
 * 단일 권위: (외부 문서) 이력(ProfessionalPromotionTeamHistory__c).md
 * Group A + Reference R-2 + BaseEntity 상속 (Q1 옵션 1)
 */
@DisplayName("ProfessionalPromotionTeamHistory SF 어노테이션 검증 (Spec #727)")
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
        @DisplayName("매핑 키 수 = 11 (Name + 4 Custom + R-2 3 + BaseEntity 2 + IsDeleted) — empCode__c Formula 제거")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamHistory::class.java)
            assertThat(mapping).hasSize(11)
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
    @DisplayName("AC1 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamHistory::class.java)

        @Test
        @DisplayName("Custom 필드 → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["oldValue__c"]).isEqualTo("old_value")
            assertThat(mapping["newValue__c"]).isEqualTo("new_value")
            assertThat(mapping["updateTime__c"]).isEqualTo("changed_at")
        }

        @Test
        @DisplayName("매핑 키셋 정확히 일치 (Group A R-2 + BaseEntity 포함, empCode__c Formula 제외)")
        fun mappingKeysExact() {
            assertThat(mapping.keys)
                .containsExactlyInAnyOrder(
                    "Name",
                    "EmployeeId__c", "oldValue__c", "newValue__c", "updateTime__c",
                    "OwnerId", "CreatedById", "LastModifiedById",
                    "CreatedDate", "LastModifiedDate",
                    "IsDeleted"
                )
        }
    }
}
