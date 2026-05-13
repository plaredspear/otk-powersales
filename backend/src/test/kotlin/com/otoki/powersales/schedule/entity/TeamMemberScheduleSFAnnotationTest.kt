package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #609 — TeamMemberSchedule ↔ Salesforce `DKRetail__TeamMemberSchedule__c` 어노테이션 부착 검증.
 *
 * 단일 권위: Salesforce Object (`DKRetail__TeamMemberSchedule__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (41개 — 33 기존 + 1 누락 + 7 신규)
 *   - AC3: PK 미부착
 */
@DisplayName("TeamMemberSchedule SF 어노테이션 검증 (Spec #609)")
class TeamMemberScheduleSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__TeamMemberSchedule__c'")
        fun sfObjectValue() {
            val annotation = TeamMemberSchedule::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__TeamMemberSchedule__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (41개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(TeamMemberSchedule::class.java)

        @Test
        @DisplayName("매핑 키 수 = 47 (Spec #733: 43 + R-2 Owner/CreatedBy/LastModifiedBy 3 + CostCenterCode 1)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(54)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 1개 신규 부착: isworkreport__c → is_work_report")
        fun section62MissingMapping() {
            assertThat(mapping["isworkreport__c"]).isEqualTo("is_work_report")
        }

        @Test
        @DisplayName("§6.3 — 신규 7개 필드 매핑 (Q1 옵션 1)")
        fun section63NewFields() {
            assertThat(mapping["HRCode__c"]).isEqualTo("hr_code")
            assertThat(mapping["DKRetail__PromotionEmpIdExt__c"]).isEqualTo("promotion_emp_id_ext")
            assertThat(mapping["SecondWorkType__c"]).isEqualTo("second_work_type")
            assertThat(mapping["WorkingCategory5__c"]).isEqualTo("working_category5")
            assertThat(mapping["ref_accountName__c"]).isEqualTo("ref_account_name")
            assertThat(mapping["MonthlyFemaleEmployeeIntegrationSchedule__c"])
                .isEqualTo("monthly_female_employee_integration_schedule_sfid")
            assertThat(mapping["ProfessionalPromotionTeam__c"]).isEqualTo("professional_promotion_team")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 33개 매핑 무변경 샘플")
        fun section61ExistingSample() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["DKRetail__WorkingDate__c"]).isEqualTo("working_date")
            assertThat(mapping["AccountId__c"]).isEqualTo("account_sfid")
            assertThat(mapping["DisplayWorkScheduleMaster__c"]).isEqualTo("display_work_schedule_sfid")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / sfid 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = TeamMemberSchedule::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례 — HCColumn 만)")
        fun sfidHasNoSfField() {
            val field = TeamMemberSchedule::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 team_member_schedule_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(TeamMemberSchedule::class.java)
            assertThat(mapping.values).doesNotContain("team_member_schedule_id")
        }
    }
}
