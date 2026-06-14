package com.otoki.powersales.domain.activity.schedule.entity

import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import jakarta.persistence.Column
import jakarta.persistence.Id
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MonthlyFemaleEmployeeIntegrationSchedule SF 어노테이션 검증 (Spec #621)")
class MonthlyFemaleEmployeeIntegrationScheduleSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'MonthlyFemaleEmployeeIntegrationSchedule__c'")
        fun sfObjectValue() {
            val annotation = MonthlyFemaleEmployeeIntegrationSchedule::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("MonthlyFemaleEmployeeIntegrationSchedule__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 27 (21 Custom + BaseEntity 2 + R-2 Owner/CreatedBy/LastModifiedBy 3 + IsDeleted 1)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(MonthlyFemaleEmployeeIntegrationSchedule::class.java)
            assertThat(mapping).hasSize(27)
        }
    }

    @Nested
    @DisplayName("AC2 — PK 미부착")
    inner class PkConvention {

        @Test
        @DisplayName("PK(id) 필드 @Column(name = \"monthly_female_employee_integration_schedule_id\") 명시")
        fun pkColumnNameConvention() {
            val field = MonthlyFemaleEmployeeIntegrationSchedule::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(Id::class.java)).isTrue()
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("monthly_female_employee_integration_schedule_id")
        }

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = MonthlyFemaleEmployeeIntegrationSchedule::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 monthly_female_employee_integration_schedule_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(MonthlyFemaleEmployeeIntegrationSchedule::class.java)
            assertThat(mapping.values).doesNotContain("monthly_female_employee_integration_schedule_id")
        }
    }

    @Nested
    @DisplayName("AC3 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(MonthlyFemaleEmployeeIntegrationSchedule::class.java)

        @Test
        @DisplayName("21개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["ExternalKey__c"]).isEqualTo("external_key")
            assertThat(mapping["Year__c"]).isEqualTo("year")
            assertThat(mapping["Month__c"]).isEqualTo("month")
            assertThat(mapping["Account__c"]).isEqualTo("account_sfid")
            assertThat(mapping["FullName__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["WorkingCategory1__c"]).isEqualTo("working_category1")
            assertThat(mapping["WorkingCategory3__c"]).isEqualTo("working_category3")
            assertThat(mapping["WorkingCategory4__c"]).isEqualTo("working_category4")
            assertThat(mapping["WorkingCategory5__c"]).isEqualTo("working_category5")
            assertThat(mapping["EmpBranchName__c"]).isEqualTo("emp_branch_name")
            assertThat(mapping["ProfessionalPromotionTeam__c"]).isEqualTo("professional_promotion_team")
            assertThat(mapping["WorkingDaysMonth__c"]).isEqualTo("working_days_month")
            assertThat(mapping["NumberOfInputs__c"]).isEqualTo("number_of_inputs")
            assertThat(mapping["EquivalentNumberOfWorkingDays__c"]).isEqualTo("equivalent_number_of_working_days")
            assertThat(mapping["ConvertedHeadcount__c"]).isEqualTo("converted_headcount")
            assertThat(mapping["EDI_POS__c"]).isEqualTo("edi_pos")
            assertThat(mapping["ThisMonthAmount__c"]).isEqualTo("this_month_amount")
            assertThat(mapping["AccountConvertedHeadcount__c"]).isEqualTo("account_converted_headcount")
            assertThat(mapping["EmployeeInputCriteriaMaster__c"]).isEqualTo("employee_input_criteria_master_sfid")
        }

        @Test
        @DisplayName("매핑 키셋 정확히 일치 (DateForReport__c Formula 컬럼 제거 — sf-meta-diff Q4)")
        fun mappingKeysExact() {
            assertThat(mapping.keys)
                .containsExactlyInAnyOrder(
                    "Name", "ExternalKey__c", "Year__c", "Month__c",
                    "Account__c", "FullName__c", "CostCenterCode__c",
                    "WorkingCategory1__c", "WorkingCategory3__c",
                    "WorkingCategory4__c", "WorkingCategory5__c",
                    "EmpBranchName__c", "ProfessionalPromotionTeam__c",
                    "WorkingDaysMonth__c", "NumberOfInputs__c",
                    "EquivalentNumberOfWorkingDays__c", "ConvertedHeadcount__c",
                    "EDI_POS__c", "ThisMonthAmount__c",
                    "AccountConvertedHeadcount__c", "EmployeeInputCriteriaMaster__c",
                    "OwnerId", "CreatedById", "LastModifiedById",
                    "CreatedDate", "LastModifiedDate",
                    "IsDeleted"
                )
        }
    }
}
