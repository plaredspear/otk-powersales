package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #628 — ProfessionalPromotionTeamMaster ↔ Salesforce `ProfessionalPromotionTeamMaster__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/전문행사조 마스터(ProfessionalPromotionTeamMaster__c).md
 */
@DisplayName("ProfessionalPromotionTeamMaster SF 어노테이션 검증 (Spec #628)")
class ProfessionalPromotionTeamMasterSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'ProfessionalPromotionTeamMaster__c'")
        fun sfObjectValue() {
            val annotation = ProfessionalPromotionTeamMaster::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("ProfessionalPromotionTeamMaster__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 12 (Spec #740: EmployeeNumber + BranchName Formula 2건 제거. 6 + BaseEntity 2 + FullName + R-2 3)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamMaster::class.java)
            assertThat(mapping).hasSize(15)
        }
    }

    @Nested
    @DisplayName("AC1 — PK·FK 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = ProfessionalPromotionTeamMaster::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = ProfessionalPromotionTeamMaster::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(accountId) 필드에 @SFField 미부착")
        fun accountIdHasNoSfField() {
            val field = ProfessionalPromotionTeamMaster::class.java.getDeclaredField("accountId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 professional_promotion_team_master_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamMaster::class.java)
            assertThat(mapping.values).doesNotContain("professional_promotion_team_master_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (6개, Spec #740: EmployeeNumber + BranchName Formula 제거)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(ProfessionalPromotionTeamMaster::class.java)

        @Test
        @DisplayName("6개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["Account__c"]).isEqualTo("account_sfid")
            assertThat(mapping["ProfessionalPromotionTeam__c"]).isEqualTo("team_type")
            assertThat(mapping["StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["Confirmed__c"]).isEqualTo("is_confirmed")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("branch_code")
        }
    }
}
